import { get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  SchedulerTask,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/scheduler/jobs'

interface BackendScheduledJob {
  id: string
  jobCode: string
  name: string
  triggerType: 'CRON' | 'MANUAL' | 'DEPENDENCY'
  cronExpr?: string | null
  status: 'ACTIVE' | 'PAUSED' | 'DISABLED'
  updatedAt?: string
}

interface RegisterJobRequest {
  jobCode: string
  name: string
  triggerType: 'CRON' | 'MANUAL'
  cronExpr?: string
  timezoneId?: string
  concurrencyPolicy: 'ALLOW' | 'FORBID' | 'REPLACE'
}

function mapStatus(status: BackendScheduledJob['status']): SchedulerTask['status'] {
  return status === 'ACTIVE' ? 'enabled' : 'disabled'
}

function mapSchedulerTask(item: BackendScheduledJob): SchedulerTask {
  return {
    id: item.jobCode,
    name: item.name,
    cron: item.cronExpr ?? item.triggerType,
    status: mapStatus(item.status),
    nextRunAt: item.updatedAt,
  }
}

export const schedulerService = {
  async list(query: InfraListQuery = {}): Promise<InfraPageData<SchedulerTask>> {
    const items = await get<BackendScheduledJob[]>(BASE_URL, {
      params: buildListParams(query),
    })

    return toPageData(items.map(mapSchedulerTask), query)
  },
  async create(payload: SchedulerTask): Promise<SchedulerTask> {
    const item = await post<BackendScheduledJob, RegisterJobRequest>(
      BASE_URL,
      {
        jobCode: payload.id,
        name: payload.name,
        triggerType: payload.cron ? 'CRON' : 'MANUAL',
        cronExpr: payload.cron || undefined,
        timezoneId: 'Asia/Shanghai',
        concurrencyPolicy: 'FORBID',
      },
      {
        dedupeKey: `scheduler:create:${payload.id}`,
      },
    )

    return mapSchedulerTask(item)
  },
  update(id: string, payload: SchedulerTask): Promise<SchedulerTask> {
    return put(`${BASE_URL}/${id}`, payload, {
      dedupeKey: `scheduler:update:${id}`,
    })
  },
  trigger(id: string): Promise<SchedulerTask> {
    return post(`${BASE_URL}/trigger/${id}`, undefined, {
      dedupeKey: `scheduler:trigger:${id}`,
    })
  },
}
