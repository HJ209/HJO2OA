import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  ListQuery,
  PositionAssignment,
  PositionAssignmentPayload,
} from '@/features/org-perm/types/org-perm'

const POSITION_URL = '/v1/org/positions'

export function listPositionAssignments(
  query: ListQuery = {},
): Promise<PageData<PositionAssignment>> {
  const params = serializePaginationParams(query)

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }

  return get<PageData<PositionAssignment>>(POSITION_URL, { params })
}

export function getPositionAssignments(
  positionId: string,
): Promise<PositionAssignment[]> {
  return get<PositionAssignment[]>(`${POSITION_URL}/${positionId}/assignments`)
}

export function createPositionAssignment(
  payload: PositionAssignmentPayload,
  idempotencyKey?: string,
): Promise<PositionAssignment> {
  return post<PositionAssignment, PositionAssignmentPayload>(
    POSITION_URL,
    payload,
    {
      dedupeKey: `position:create:${payload.personId}:${payload.positionId}`,
      idempotencyKey,
    },
  )
}

export function updatePositionAssignment(
  id: string,
  payload: PositionAssignmentPayload,
  idempotencyKey?: string,
): Promise<PositionAssignment> {
  return put<PositionAssignment, PositionAssignmentPayload>(
    `${POSITION_URL}/${id}`,
    payload,
    {
      dedupeKey: `position:update:${id}`,
      idempotencyKey,
    },
  )
}
