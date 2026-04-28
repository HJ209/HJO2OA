import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  ListQuery,
  PersonAccount,
  PersonAccountPayload,
} from '@/features/org-perm/types/org-perm'

const PERSON_URL = '/v1/org/persons'

function buildPersonParams(query: ListQuery): URLSearchParams {
  const params = serializePaginationParams(query)

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }

  return params
}

export function listPersonAccounts(
  query: ListQuery = {},
): Promise<PageData<PersonAccount>> {
  return get<PageData<PersonAccount>>(PERSON_URL, {
    params: buildPersonParams(query),
  })
}

export function getPersonAccount(id: string): Promise<PersonAccount> {
  return get<PersonAccount>(`${PERSON_URL}/${id}`)
}

export function createPersonAccount(
  payload: PersonAccountPayload,
  idempotencyKey?: string,
): Promise<PersonAccount> {
  return post<PersonAccount, PersonAccountPayload>(PERSON_URL, payload, {
    dedupeKey: `person:create:${payload.accountName}`,
    idempotencyKey,
  })
}

export function updatePersonAccount(
  id: string,
  payload: PersonAccountPayload,
  idempotencyKey?: string,
): Promise<PersonAccount> {
  return put<PersonAccount, PersonAccountPayload>(
    `${PERSON_URL}/${id}`,
    payload,
    {
      dedupeKey: `person:update:${id}`,
      idempotencyKey,
    },
  )
}
