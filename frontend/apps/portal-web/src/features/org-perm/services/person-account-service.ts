import { del, get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  Account,
  AccountPayload,
  AccountStatus,
  ListQuery,
  PersonAccount,
  PersonAccountDetail,
  PersonAccountPayload,
  PersonProfile,
} from '@/features/org-perm/types/org-perm'

const BASE_URL = '/v1/org/person-accounts'
const PERSON_URL = `${BASE_URL}/persons`
const ACCOUNT_URL = `${BASE_URL}/accounts`

interface BackendPersonAccountResponse {
  person: PersonProfile
  accounts: Account[]
}

interface BackendExportResponse {
  persons: PersonProfile[]
  personAccounts: BackendPersonAccountResponse[]
}

function tenantParams(tenantId: string): URLSearchParams {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  return params
}

function mapPersonStatus(
  person: PersonProfile,
  primaryAccount?: Account,
): AccountStatus {
  if (primaryAccount?.locked) {
    return 'LOCKED'
  }

  if (person.status === 'ACTIVE') {
    return 'ACTIVE'
  }

  return 'DISABLED'
}

function mapPersonAccountResponse(
  item: PersonProfile | BackendPersonAccountResponse,
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
    employeeNo: person.employeeNo,
    email: person.email ?? undefined,
    mobile: person.mobile ?? undefined,
    orgId: person.organizationId,
    orgName: person.organizationId,
    departmentId: person.departmentId ?? undefined,
    status: mapPersonStatus(person, primaryAccount),
    personStatus: person.status,
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
    [
      item.accountName,
      item.displayName,
      item.employeeNo,
      item.email,
      item.mobile,
      item.orgId,
      item.departmentId,
    ]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

export async function listPersonAccounts(
  query: ListQuery = {},
): Promise<PageData<PersonAccount>> {
  const tenantId = await resolveCurrentTenantId()
  const items = await get<PersonProfile[]>(PERSON_URL, {
    params: tenantParams(tenantId),
  })
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

export async function getPersonAccountDetail(
  id: string,
): Promise<PersonAccountDetail> {
  return get<BackendPersonAccountResponse>(`${PERSON_URL}/${id}`)
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
      departmentId?: string
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
      departmentId: payload.departmentId,
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
      departmentId?: string
    }
  >(
    `${PERSON_URL}/${id}`,
    {
      name: payload.displayName,
      mobile: payload.mobile,
      email: payload.email,
      organizationId: payload.orgId,
      departmentId: payload.departmentId,
    },
    {
      dedupeKey: `person:update:${id}`,
      idempotencyKey,
    },
  )

  return mapPersonAccountResponse(item)
}

export function activatePerson(id: string): Promise<PersonAccountDetail> {
  return put<BackendPersonAccountResponse, Record<string, never>>(
    `${PERSON_URL}/${id}/activate`,
    {},
    { dedupeKey: `person:activate:${id}` },
  )
}

export function disablePerson(id: string): Promise<PersonAccountDetail> {
  return put<BackendPersonAccountResponse, Record<string, never>>(
    `${PERSON_URL}/${id}/disable`,
    {},
    { dedupeKey: `person:disable:${id}` },
  )
}

export function resignPerson(id: string): Promise<PersonAccountDetail> {
  return put<BackendPersonAccountResponse, Record<string, never>>(
    `${PERSON_URL}/${id}/resign`,
    {},
    { dedupeKey: `person:resign:${id}` },
  )
}

export async function deletePersonAccount(id: string): Promise<void> {
  await del<void>(`${PERSON_URL}/${id}`, {
    dedupeKey: `person:delete:${id}`,
  })
}

export function createAccount(
  personId: string,
  payload: AccountPayload,
  idempotencyKey?: string,
): Promise<PersonAccountDetail> {
  return post<BackendPersonAccountResponse, AccountPayload>(
    `${PERSON_URL}/${personId}/accounts`,
    payload,
    {
      dedupeKey: `account:create:${personId}:${payload.username}`,
      idempotencyKey,
    },
  )
}

export function resetAccountCredential(
  accountId: string,
  credential: string,
  mustChangePassword = true,
): Promise<Account> {
  return put<Account, { credential: string; mustChangePassword: boolean }>(
    `${ACCOUNT_URL}/${accountId}/credential`,
    { credential, mustChangePassword },
    { dedupeKey: `account:reset:${accountId}` },
  )
}

export function lockAccount(
  accountId: string,
  lockedUntil?: string | null,
): Promise<Account> {
  return put<Account, { lockedUntil?: string | null }>(
    `${ACCOUNT_URL}/${accountId}/lock`,
    { lockedUntil },
    { dedupeKey: `account:lock:${accountId}` },
  )
}

export function unlockAccount(accountId: string): Promise<Account> {
  return put<Account, Record<string, never>>(
    `${ACCOUNT_URL}/${accountId}/unlock`,
    {},
    { dedupeKey: `account:unlock:${accountId}` },
  )
}

export function disableAccount(accountId: string): Promise<Account> {
  return put<Account, Record<string, never>>(
    `${ACCOUNT_URL}/${accountId}/disable`,
    {},
    { dedupeKey: `account:disable:${accountId}` },
  )
}

export function activateAccount(accountId: string): Promise<Account> {
  return put<Account, Record<string, never>>(
    `${ACCOUNT_URL}/${accountId}/activate`,
    {},
    { dedupeKey: `account:activate:${accountId}` },
  )
}

export function setPrimaryAccount(
  personId: string,
  accountId: string,
): Promise<PersonAccountDetail> {
  return put<BackendPersonAccountResponse, Record<string, never>>(
    `${PERSON_URL}/${personId}/accounts/${accountId}/primary`,
    {},
    { dedupeKey: `account:primary:${personId}:${accountId}` },
  )
}

export async function exportPersonAccounts(): Promise<{
  persons: PersonProfile[]
  personAccounts: PersonAccountDetail[]
}> {
  return get<BackendExportResponse>(`${BASE_URL}/export`)
}
