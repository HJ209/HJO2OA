import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  createOrgStructure,
  getOrgStructure,
  listOrgStructures,
  updateOrgStructure,
} from '@/features/org-perm/services/org-structure-service'
import type { OrgStructurePayload } from '@/features/org-perm/types/org-perm'

const requestMocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const serviceUtilsMocks = vi.hoisted(() => ({
  resolveCurrentTenantId: vi.fn(),
}))

vi.mock('@/services/request', () => requestMocks)
vi.mock('@/features/org-perm/services/service-utils', () => serviceUtilsMocks)

describe('org-structure-service', () => {
  const payload: OrgStructurePayload = {
    parentId: null,
    name: '研发中心',
    code: 'RD',
    type: 'DEPARTMENT',
    status: 'ACTIVE',
    sortOrder: 1,
  }

  beforeEach(() => {
    requestMocks.get.mockReset()
    requestMocks.post.mockReset()
    requestMocks.put.mockReset()
    serviceUtilsMocks.resolveCurrentTenantId.mockReset()
    serviceUtilsMocks.resolveCurrentTenantId.mockResolvedValue(
      '00000000-0000-0000-0000-000000000001',
    )
  })

  it('calls list endpoint', async () => {
    requestMocks.get.mockResolvedValueOnce([])

    await listOrgStructures()

    expect(requestMocks.get).toHaveBeenCalledWith(
      '/v1/org/structure/organizations',
      {
        params: expect.any(URLSearchParams),
      },
    )
  })

  it('calls detail endpoint', async () => {
    requestMocks.get.mockResolvedValueOnce({ id: 'org-1' })

    await getOrgStructure('org-1')

    expect(requestMocks.get).toHaveBeenCalledWith(
      '/v1/org/structure/organizations/org-1',
    )
  })

  it('creates organization with dedupe and idempotency options', async () => {
    requestMocks.post.mockResolvedValueOnce({ id: 'org-1' })

    await createOrgStructure(payload, 'idem-1')

    expect(requestMocks.post).toHaveBeenCalledWith(
      '/v1/org/structure/organizations',
      {
        code: 'RD',
        name: '研发中心',
        shortName: '研发中心',
        type: 'DEPARTMENT',
        parentId: null,
        sortOrder: 1,
        tenantId: '00000000-0000-0000-0000-000000000001',
      },
      {
        dedupeKey: 'org-structure:create:RD',
        idempotencyKey: 'idem-1',
      },
    )
  })

  it('updates organization with dedupe and idempotency options', async () => {
    requestMocks.put.mockResolvedValueOnce({ id: 'org-1' })

    await updateOrgStructure('org-1', payload, 'idem-2')

    expect(requestMocks.put).toHaveBeenCalledWith(
      '/v1/org/structure/organizations/org-1',
      {
        code: 'RD',
        name: '研发中心',
        shortName: '研发中心',
        type: 'DEPARTMENT',
        sortOrder: 1,
      },
      {
        dedupeKey: 'org-structure:update:org-1',
        idempotencyKey: 'idem-2',
      },
    )
  })
})
