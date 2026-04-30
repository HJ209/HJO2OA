import { get, post } from '@/services/request'
import type {
  ExceptionInstance,
  InterventionRequest,
  MonitoredInstance,
  NodeTrail,
  ProcessIntervention,
} from '@/features/workflow/types/process-monitor'

const MONITOR_API = '/v1/process/monitor/admin'

function idempotencyKey(scope: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${scope}-${crypto.randomUUID()}`
  }

  return `${scope}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const processMonitorService = {
  getInstances(params: {
    tenantId?: string
    definitionCode?: string
    status?: string
    limit?: number
  }): Promise<MonitoredInstance[]> {
    return get<MonitoredInstance[]>(`${MONITOR_API}/instances`, {
      params: {
        'filter[tenantId]': params.tenantId,
        'filter[definitionCode]': params.definitionCode,
        'filter[status]': params.status,
        'page[limit]': params.limit ?? 50,
      },
    })
  },

  getExceptions(params: {
    tenantId?: string
    definitionCode?: string
    stalledThresholdMinutes?: number
    limit?: number
  }): Promise<ExceptionInstance[]> {
    return get<ExceptionInstance[]>(`${MONITOR_API}/exceptions`, {
      params: {
        'filter[tenantId]': params.tenantId,
        'filter[definitionCode]': params.definitionCode,
        'filter[stalledThresholdMinutes]':
          params.stalledThresholdMinutes ?? 1440,
        'page[limit]': params.limit ?? 50,
      },
    })
  },

  getNodeTrail(tenantId: string, instanceId: string): Promise<NodeTrail[]> {
    return get<NodeTrail[]>(`${MONITOR_API}/instances/${instanceId}/trail`, {
      params: { 'filter[tenantId]': tenantId },
    })
  },

  intervene(
    instanceId: string,
    payload: InterventionRequest,
  ): Promise<ProcessIntervention> {
    return post<ProcessIntervention, InterventionRequest>(
      `${MONITOR_API}/instances/${instanceId}/interventions`,
      payload,
      {
        dedupeKey: `process-monitor:intervene:${instanceId}:${payload.actionType}`,
        idempotencyKey: idempotencyKey('process-monitor-intervene'),
      },
    )
  },
}
