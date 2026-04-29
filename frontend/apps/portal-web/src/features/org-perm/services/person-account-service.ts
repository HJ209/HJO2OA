import { del, get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  ListQuery,
  PersonAccount,
  PersonAccountPayload,
} from '@/features/org-perm/types/org-perm'

const PERSON_URL = '/v1/org/person-accounts/persons'

interface BackendPersonResponse {
  id: string
  employeeNo: string
  name: string
  mobile?: string | null
  email?: string | null
  organizationId?: string | null
  status: 'ACTIVE' | 'DISABLED' | 'RESIGNED'
  updatedAt?: string
}

interface BackendAccountResponse {
  id: string
  username: string
  primaryAccount: boolean
  locked: boolean
  status: 'ACTIVE' | 'LOCKED' | 'DISABLED'
}

interface BackendPersonAccountResponse {
  person: BackendPersonResponse
  accounts: BackendAccountResponse[]
}

function mapPersonStatus(
  person: BackendPersonResponse,
  primaryAccount?: BackendAccountResponse,
): PersonAccount['status'] {
  if (primaryAccount?.locked || primaryAccount?.status === 'LOCKED') {
    return 'LOCKED'
  }

  if (person.status === 'ACTIVE') {
    return 'ACTIVE'
  }

  return 'DISABLED'
}

function mapPersonAccountResponse(
  item: BackendPersonResponse | BackendPersonAccountResponse,
): PersonAccount {
  const person = 'person' in item ? item.person : item
  const primaryAccount =
    'accounts' in item
      ? (item.accounts.find((account) => account.primaryAccount) ??
        item.accounts[0])
      : undefined

  return {
    id: person.id,
    accountName: primaryAccount?.username ?? person.employeeNo,
    displayName: person.name,
    email: person.email ?? undefined,
    mobile: person.mobile ?? undefined,
    orgId: person.organizationId ?? undefined,
    orgName: person.organizationId ?? undefined,
    status: mapPersonStatus(person, primaryAccount),
    updatedAtUtc: person.updatedAt,
  }
}

function filterPersonAccounts(
  items: PersonAccount[],
  query: ListQuery,
): PersonAccount[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [item.accountName, item.displayName, item.email, item.mobile, item.orgId]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

export async function listPersonAccounts(
  query: ListQuery = {},
): Promise<PageData<PersonAccount>> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendPersonResponse[]>(PERSON_URL, { params })
  const mappedItems = filterPersonAccounts(
    items.map(mapPersonAccountResponse),
    query,
  )

  return toPageData(mappedItems, query)
}

export async function getPersonAccount(id: string): Promise<PersonAccount> {
  const item = await get<BackendPersonAccountResponse>(`${PERSON_URL}/${id}`)

  return mapPersonAccountResponse(item)
}

export async function createPersonAccount(
  payload: PersonAccountPayload,
  idempotencyKey?: string,
): Promise<PersonAccount> {
  const tenantId = await resolveCurrentTenantId()
  const item = await post<
    BackendPersonAccountResponse,
    {
      employeeNo: string
      name: string
      mobile?: string
      email?: string
      organizationId?: string
      tenantId: string
    }
  >(
    PERSON_URL,
    {
      employeeNo: payload.accountName,
      name: payload.displayName,
      mobile: payload.mobile,
      email: payload.email,
      organizationId: payload.orgId,
      tenantId,
    },
    {
      dedupeKey: `person:create:${payload.accountName}`,
      idempotencyKey,
    },
  )

  return mapPersonAccountResponse(item)
}

export async function updatePersonAccount(
  id: string,
  payload: PersonAccountPayload,
  idempotencyKey?: string,
): Promise<PersonAccount> {
  const item = await put<
    BackendPersonAccountResponse,
    {
      name: string
      mobile?: string
      email?: string
      organizationId?: string
    }
  >(
    `${PERSON_URL}/${id}`,
    {
      name: payload.displayName,
      mobile: payload.mobile,
      email: payload.email,
      organizationId: payload.orgId,
    },
    {
      dedupeKey: `person:update:${id}`,
      idempotencyKey,
    },
  )

  return mapPersonAccountResponse(item)
}

export async function deletePersonAccount(id: string): Promise<void> {
  await del<void>(`${PERSON_URL}/${id}`, {
    dedupeKey: `person:delete:${id}`,
  })
}
