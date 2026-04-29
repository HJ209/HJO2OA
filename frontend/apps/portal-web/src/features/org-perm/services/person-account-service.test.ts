import { beforeEach, describe, expect, it, vi } from 'vitest'
import { deletePersonAccount } from '@/features/org-perm/services/person-account-service'
import { del } from '@/services/request'

const requestMocks = vi.hoisted(() => ({
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const serviceUtilsMocks = vi.hoisted(() => ({
  resolveCurrentTenantId: vi.fn(),
  toPageData: vi.fn(),
}))

vi.mock('@/services/request', () => requestMocks)
vi.mock('@/features/org-perm/services/service-utils', () => serviceUtilsMocks)

const mockedDel = vi.mocked(del)

describe('person-account-service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('deletes a person account with a dedupe key', async () => {
    mockedDel.mockResolvedValueOnce(undefined)

    await deletePersonAccount('person-1')

    expect(mockedDel).toHaveBeenCalledWith(
      '/v1/org/person-accounts/persons/person-1',
      { dedupeKey: 'person:delete:person-1' },
    )
  })
})
