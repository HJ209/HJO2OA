import { get, post, put } from '@/services/request'
import { resolveCurrentTenantId } from '@/features/org-perm/services/service-utils'
import type {
  OrgStructure,
  OrgStructurePayload,
} from '@/features/org-perm/types/org-perm'

const ORG_STRUCTURE_URL = '/v1/org/structure/organizations'

interface BackendOrgStructure {
  id: string
  code: string
  name: string
  type: string
  parentId?: string | null
  sortOrder: number
  status: 'ACTIVE' | 'DISABLED'
}

function mapOrgStructure(item: BackendOrgStructure): OrgStructure {
  return {
    id: item.id,
    parentId: item.parentId,
    name: item.name,
    code: item.code,
    type:
      item.type === 'COMPANY' ||
      item.type === 'DEPARTMENT' ||
      item.type === 'TEAM'
        ? item.type
        : 'DEPARTMENT',
    status: item.status,
    sortOrder: item.sortOrder,
  }
}

function toOrgTree(items: OrgStructure[]): OrgStructure[] {
  const nodes = items.map((item) => ({
    ...item,
    children: [] as OrgStructure[],
  }))
  const nodeMap = new Map(nodes.map((node) => [node.id, node]))
  const roots: OrgStructure[] = []

  nodes.forEach((node) => {
    const parentNode = node.parentId ? nodeMap.get(node.parentId) : undefined

    if (parentNode) {
      ;(parentNode.children ?? []).push(node)
      return
    }

    roots.push(node)
  })

  const sortNodes = (list: OrgStructure[]): OrgStructure[] =>
    list
      .sort((left, right) => left.sortOrder - right.sortOrder)
      .map((node) => ({
        ...node,
        children: node.children?.length ? sortNodes(node.children) : undefined,
      }))

  return sortNodes(roots)
}

export async function listOrgStructures(): Promise<OrgStructure[]> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendOrgStructure[]>(ORG_STRUCTURE_URL, { params })

  return toOrgTree(items.map(mapOrgStructure))
}

export async function getOrgStructure(id: string): Promise<OrgStructure> {
  const item = await get<BackendOrgStructure>(`${ORG_STRUCTURE_URL}/${id}`)

  return mapOrgStructure(item)
}

export async function createOrgStructure(
  payload: OrgStructurePayload,
  idempotencyKey?: string,
): Promise<OrgStructure> {
  const tenantId = await resolveCurrentTenantId()
  const item = await post<
    BackendOrgStructure,
    {
      code: string
      name: string
      shortName?: string
      type: OrgStructurePayload['type']
      parentId?: string | null
      sortOrder: number
      tenantId: string
    }
  >(
    ORG_STRUCTURE_URL,
    {
      code: payload.code,
      name: payload.name,
      shortName: payload.name,
      type: payload.type,
      parentId: payload.parentId,
      sortOrder: payload.sortOrder,
      tenantId,
    },
    {
      dedupeKey: `org-structure:create:${payload.code}`,
      idempotencyKey,
    },
  )

  return mapOrgStructure(item)
}

export async function updateOrgStructure(
  id: string,
  payload: OrgStructurePayload,
  idempotencyKey?: string,
): Promise<OrgStructure> {
  const item = await put<
    BackendOrgStructure,
    {
      code: string
      name: string
      shortName?: string
      type: OrgStructurePayload['type']
      sortOrder: number
    }
  >(
    `${ORG_STRUCTURE_URL}/${id}`,
    {
      code: payload.code,
      name: payload.name,
      shortName: payload.name,
      type: payload.type,
      sortOrder: payload.sortOrder,
    },
    {
      dedupeKey: `org-structure:update:${id}`,
      idempotencyKey,
    },
  )

  return mapOrgStructure(item)
}
