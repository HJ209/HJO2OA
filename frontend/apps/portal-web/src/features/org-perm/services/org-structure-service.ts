import { get, post, put } from '@/services/request'
import type {
  OrgStructure,
  OrgStructurePayload,
} from '@/features/org-perm/types/org-perm'

const ORG_STRUCTURE_URL = '/v1/org/structures'

export function listOrgStructures(): Promise<OrgStructure[]> {
  return get<OrgStructure[]>(ORG_STRUCTURE_URL)
}

export function getOrgStructure(id: string): Promise<OrgStructure> {
  return get<OrgStructure>(`${ORG_STRUCTURE_URL}/${id}`)
}

export function createOrgStructure(
  payload: OrgStructurePayload,
  idempotencyKey?: string,
): Promise<OrgStructure> {
  return post<OrgStructure, OrgStructurePayload>(ORG_STRUCTURE_URL, payload, {
    dedupeKey: `org-structure:create:${payload.code}`,
    idempotencyKey,
  })
}

export function updateOrgStructure(
  id: string,
  payload: OrgStructurePayload,
  idempotencyKey?: string,
): Promise<OrgStructure> {
  return put<OrgStructure, OrgStructurePayload>(
    `${ORG_STRUCTURE_URL}/${id}`,
    payload,
    {
      dedupeKey: `org-structure:update:${id}`,
      idempotencyKey,
    },
  )
}
