import { del, get, post, put } from '@/services/request'
import { resolveCurrentTenantId } from '@/features/org-perm/services/service-utils'
import type {
  Department,
  DepartmentPayload,
  MoveNodePayload,
  OrgStructure,
  OrgStructurePayload,
} from '@/features/org-perm/types/org-perm'

const STRUCTURE_URL = '/v1/org/structure'
const ORG_URL = `${STRUCTURE_URL}/organizations`
const DEPT_URL = `${STRUCTURE_URL}/departments`

interface BackendOrgStructure {
  id: string
  code: string
  name: string
  shortName?: string | null
  type: string
  parentId?: string | null
  level?: number
  path?: string
  sortOrder: number
  status: 'ACTIVE' | 'DISABLED'
}

interface BackendDepartment {
  id: string
  code: string
  name: string
  organizationId: string
  parentId?: string | null
  level: number
  path: string
  managerId?: string | null
  sortOrder: number
  status: 'ACTIVE' | 'DISABLED'
}

interface BackendExportResponse {
  organizations: BackendOrgStructure[]
  departments: BackendDepartment[]
}

function asOrgType(type: string): OrgStructure['type'] {
  if (type === 'COMPANY' || type === 'DEPARTMENT' || type === 'TEAM') {
    return type
  }

  return 'DEPARTMENT'
}

function mapOrgStructure(item: BackendOrgStructure): OrgStructure {
  return {
    id: item.id,
    parentId: item.parentId,
    name: item.name,
    code: item.code,
    shortName: item.shortName,
    type: asOrgType(item.type),
    level: item.level,
    path: item.path,
    status: item.status,
    sortOrder: item.sortOrder,
  }
}

function mapDepartment(item: BackendDepartment): Department {
  return {
    id: item.id,
    code: item.code,
    name: item.name,
    organizationId: item.organizationId,
    parentId: item.parentId,
    level: item.level,
    path: item.path,
    managerId: item.managerId,
    sortOrder: item.sortOrder,
    status: item.status,
  }
}

function tenantParams(tenantId: string): URLSearchParams {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  return params
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
      parentNode.children.push(node)
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
  const items = await get<BackendOrgStructure[]>(ORG_URL, {
    params: tenantParams(tenantId),
  })

  return toOrgTree(items.map(mapOrgStructure))
}

export async function listFlatOrganizations(): Promise<OrgStructure[]> {
  const tenantId = await resolveCurrentTenantId()
  const items = await get<BackendOrgStructure[]>(ORG_URL, {
    params: tenantParams(tenantId),
  })

  return items.map(mapOrgStructure)
}

export async function getOrgStructure(id: string): Promise<OrgStructure> {
  const item = await get<BackendOrgStructure>(`${ORG_URL}/${id}`)

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
    ORG_URL,
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
    `${ORG_URL}/${id}`,
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

export async function moveOrgStructure(
  id: string,
  payload: MoveNodePayload,
  idempotencyKey?: string,
): Promise<OrgStructure> {
  const item = await put<BackendOrgStructure, MoveNodePayload>(
    `${ORG_URL}/${id}/move`,
    payload,
    {
      dedupeKey: `org-structure:move:${id}`,
      idempotencyKey,
    },
  )

  return mapOrgStructure(item)
}

export async function setOrgStructureEnabled(
  id: string,
  enabled: boolean,
): Promise<OrgStructure> {
  const item = await put<BackendOrgStructure, Record<string, never>>(
    `${ORG_URL}/${id}/${enabled ? 'activate' : 'disable'}`,
    {},
    { dedupeKey: `org-structure:status:${id}:${enabled}` },
  )

  return mapOrgStructure(item)
}

export async function deleteOrgStructure(id: string): Promise<void> {
  await del<void>(`${ORG_URL}/${id}`, {
    dedupeKey: `org-structure:delete:${id}`,
  })
}

export async function listDepartments(
  organizationId: string,
): Promise<Department[]> {
  const tenantId = await resolveCurrentTenantId()
  const params = tenantParams(tenantId)
  params.set('organizationId', organizationId)
  const items = await get<BackendDepartment[]>(DEPT_URL, { params })

  return items.map(mapDepartment)
}

export async function createDepartment(
  payload: DepartmentPayload,
  idempotencyKey?: string,
): Promise<Department> {
  const tenantId = await resolveCurrentTenantId()
  const item = await post<
    BackendDepartment,
    DepartmentPayload & { tenantId: string }
  >(
    DEPT_URL,
    { ...payload, tenantId },
    {
      dedupeKey: `department:create:${payload.organizationId}:${payload.code}`,
      idempotencyKey,
    },
  )

  return mapDepartment(item)
}

export async function updateDepartment(
  id: string,
  payload: DepartmentPayload,
  idempotencyKey?: string,
): Promise<Department> {
  const item = await put<
    BackendDepartment,
    Omit<DepartmentPayload, 'organizationId'>
  >(
    `${DEPT_URL}/${id}`,
    {
      code: payload.code,
      name: payload.name,
      parentId: payload.parentId,
      managerId: payload.managerId,
      sortOrder: payload.sortOrder,
    },
    {
      dedupeKey: `department:update:${id}`,
      idempotencyKey,
    },
  )

  return mapDepartment(item)
}

export async function moveDepartment(
  id: string,
  payload: MoveNodePayload,
  idempotencyKey?: string,
): Promise<Department> {
  const item = await put<BackendDepartment, MoveNodePayload>(
    `${DEPT_URL}/${id}/move`,
    payload,
    {
      dedupeKey: `department:move:${id}`,
      idempotencyKey,
    },
  )

  return mapDepartment(item)
}

export async function setDepartmentEnabled(
  id: string,
  enabled: boolean,
): Promise<Department> {
  const item = await put<BackendDepartment, Record<string, never>>(
    `${DEPT_URL}/${id}/${enabled ? 'activate' : 'disable'}`,
    {},
    { dedupeKey: `department:status:${id}:${enabled}` },
  )

  return mapDepartment(item)
}

export async function deleteDepartment(id: string): Promise<void> {
  await del<void>(`${DEPT_URL}/${id}`, {
    dedupeKey: `department:delete:${id}`,
  })
}

export async function exportOrgMasterData(): Promise<{
  organizations: OrgStructure[]
  departments: Department[]
}> {
  const data = await get<BackendExportResponse>(`${STRUCTURE_URL}/export`)

  return {
    organizations: data.organizations.map(mapOrgStructure),
    departments: data.departments.map(mapDepartment),
  }
}
