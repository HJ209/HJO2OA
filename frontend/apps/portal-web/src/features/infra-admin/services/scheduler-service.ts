import { get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  SchedulerExecution,
  SchedulerExecutionStatus,
  SchedulerTask,
} from '@/features/infra-admin/types/infra'

const JOBS_URL = '/v1/infra/scheduler/jobs'
const EXECUTIONS_URL = '/v1/infra/scheduler/executions'

interface BackendScheduledJob {
  id: string
  jobCode: string
  handlerName: string
  name: string
  triggerType: 'CRON' | 'MANUAL' | 'DEPENDENCY'
  cronExpr?: string | null
  timezoneId?: string | null
  concurrencyPolicy: 'ALLOW' | 'FORBID' | 'REPLACE'
  retryPolicy?: string | null
  status: 'ACTIVE' | 'PAUSED' | 'DISABLED'
  tenantId?: string | null
  createdAt?: string
  updatedAt?: string
}

interface BackendJobExecution {
  id: string
  scheduledJobId: string
  parentExecutionId?: string | null
  triggerSource: 'CRON' | 'MANUAL' | 'RETRY' | 'DEPENDENCY'
  executionStatus: SchedulerExecutionStatus
  startedAt: string
  finishedAt?: string | null
  durationMs?: number | null
  attemptNo: number
  maxAttempts: number
  errorCode?: string | null
  errorMessage?: string | null
  errorStack?: string | null
  executionLog?: string | null
  triggerContext?: string | null
  idempotencyKey?: string | null
  nextRetryAt?: string | null
}

interface RegisterJobRequest {
  jobCode: string
  handlerName: string
  name: string
  triggerType: 'CRON' | 'MANUAL' | 'DEPENDENCY'
  cronExpr?: string
  timezoneId?: string
  concurrencyPolicy: 'ALLOW' | 'FORBID' | 'REPLACE'
  timeoutSeconds?: number
  retryPolicy?: string
  tenantId?: string | null
}

export interface SchedulerExecutionQuery extends InfraListQuery {
  jobId?: string
  executionStatus?: SchedulerExecutionStatus | ''
  from?: string
  to?: string
}

function mapStatus(
  status: BackendScheduledJob['status'],
): SchedulerTask['status'] {
  if (status === 'PAUSED') {
    return 'paused'
  }

  return status === 'ACTIVE' ? 'enabled' : 'disabled'
}

function mapSchedulerTask(item: BackendScheduledJob): SchedulerTask {
  return {
    id: item.id,
    jobCode: item.jobCode,
    handlerName: item.handlerName,
    name: item.name,
    cron: item.cronExpr ?? item.triggerType,
    triggerType: item.triggerType,
    timezoneId: item.timezoneId,
    concurrencyPolicy: item.concurrencyPolicy,
    retryPolicy: item.retryPolicy,
    status: mapStatus(item.status),
    tenantId: item.tenantId,
    nextRunAt: item.updatedAt,
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
  }
}

function mapExecution(item: BackendJobExecution): SchedulerExecution {
  return {
    id: item.id,
    scheduledJobId: item.scheduledJobId,
    parentExecutionId: item.parentExecutionId,
    triggerSource: item.triggerSource,
    executionStatus: item.executionStatus,
    startedAt: item.startedAt,
    finishedAt: item.finishedAt,
    durationMs: item.durationMs,
    attemptNo: item.attemptNo,
    maxAttempts: item.maxAttempts,
    errorCode: item.errorCode,
    errorMessage: item.errorMessage,
    errorStack: item.errorStack,
    executionLog: item.executionLog,
    triggerContext: item.triggerContext,
    idempotencyKey: item.idempotencyKey,
    nextRetryAt: item.nextRetryAt,
  }
}

function buildExecutionParams(
  query: SchedulerExecutionQuery = {},
): URLSearchParams {
  const params = buildListParams(query)

  if (query.jobId) {
    params.set('jobId', query.jobId)
  }

  if (query.executionStatus) {
    params.set('executionStatus', query.executionStatus)
  }

  if (query.from) {
    params.set('from', query.from)
  }

  if (query.to) {
    params.set('to', query.to)
  }

  return params
}

function toRegisterRequest(payload: SchedulerTask): RegisterJobRequest {
  return {
    jobCode: payload.jobCode || payload.id,
    handlerName: payload.handlerName || payload.jobCode || payload.id,
    name: payload.name,
    triggerType: payload.triggerType,
    cronExpr: payload.triggerType === 'CRON' ? payload.cron : undefined,
    timezoneId: payload.timezoneId ?? 'Asia/Shanghai',
    concurrencyPolicy: payload.concurrencyPolicy,
    retryPolicy: payload.retryPolicy ?? undefined,
    tenantId: payload.tenantId,
  }
}

export const schedulerService = {
  async list(
    query: InfraListQuery = {},
  ): Promise<InfraPageData<SchedulerTask>> {
    const items = await get<BackendScheduledJob[]>(JOBS_URL, {
      params: buildListParams(query),
    })

    return toPageData(items.map(mapSchedulerTask), query)
  },
  async create(payload: SchedulerTask): Promise<SchedulerTask> {
    const item = await post<BackendScheduledJob, RegisterJobRequest>(
      JOBS_URL,
      toRegisterRequest(payload),
      {
        dedupeKey: `scheduler:create:${payload.jobCode || payload.id}`,
      },
    )

    return mapSchedulerTask(item)
  },
  async enable(id: string): Promise<SchedulerTask> {
    return mapSchedulerTask(
      await put<BackendScheduledJob, undefined>(
        `${JOBS_URL}/${id}/enable`,
        undefined,
        {
          dedupeKey: `scheduler:enable:${id}`,
        },
      ),
    )
  },
  async pause(id: string): Promise<SchedulerTask> {
    return mapSchedulerTask(
      await put<BackendScheduledJob, undefined>(
        `${JOBS_URL}/${id}/pause`,
        undefined,
        {
          dedupeKey: `scheduler:pause:${id}`,
        },
      ),
    )
  },
  async resume(id: string): Promise<SchedulerTask> {
    return mapSchedulerTask(
      await put<BackendScheduledJob, undefined>(
        `${JOBS_URL}/${id}/resume`,
        undefined,
        {
          dedupeKey: `scheduler:resume:${id}`,
        },
      ),
    )
  },
  async disable(id: string): Promise<SchedulerTask> {
    return mapSchedulerTask(
      await put<BackendScheduledJob, undefined>(
        `${JOBS_URL}/${id}/disable`,
        undefined,
        {
          dedupeKey: `scheduler:disable:${id}`,
        },
      ),
    )
  },
  async trigger(id: string): Promise<SchedulerExecution> {
    return mapExecution(
      await post<BackendJobExecution, undefined>(
        `${JOBS_URL}/${id}/trigger`,
        undefined,
        {
          dedupeKey: `scheduler:trigger:${id}`,
        },
      ),
    )
  },
  async listExecutions(
    query: SchedulerExecutionQuery = {},
  ): Promise<InfraPageData<SchedulerExecution>> {
    const items = await get<BackendJobExecution[]>(EXECUTIONS_URL, {
      params: buildExecutionParams(query),
    })

    return toPageData(items.map(mapExecution), query)
  },
  async getExecution(id: string): Promise<SchedulerExecution> {
    return mapExecution(
      await get<BackendJobExecution>(`${EXECUTIONS_URL}/${id}`),
    )
  },
  async retryExecution(id: string): Promise<SchedulerExecution> {
    return mapExecution(
      await post<BackendJobExecution, undefined>(
        `${EXECUTIONS_URL}/${id}/retry`,
        undefined,
        {
          dedupeKey: `scheduler:retry:${id}`,
        },
      ),
    )
  },
}
