import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  SchedulerTask,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/scheduler/tasks'

export const schedulerService = {
  list(query?: InfraListQuery): Promise<InfraPageData<SchedulerTask>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: SchedulerTask): Promise<SchedulerTask> {
    return post(BASE_URL, payload, {
      dedupeKey: `scheduler:create:${payload.id}`,
    })
  },
  update(id: string, payload: SchedulerTask): Promise<SchedulerTask> {
    return put(`${BASE_URL}/${id}`, payload, {
      dedupeKey: `scheduler:update:${id}`,
    })
  },
  trigger(id: string): Promise<SchedulerTask> {
    return post(`${BASE_URL}/${id}/trigger`, undefined, {
      dedupeKey: `scheduler:trigger:${id}`,
    })
  },
}
