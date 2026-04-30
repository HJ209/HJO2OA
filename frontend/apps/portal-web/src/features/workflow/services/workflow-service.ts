import { get, post, put } from '@/services/request'
import type {
  ActionDefinition,
  ExecuteActionRequest,
  ProcessInstanceDetail,
  SaveWorkflowDefinitionRequest,
  StartProcessRequest,
  WorkflowDefinition,
} from '@/features/workflow/types/workflow'

const DEFINITION_API = '/v1/process/definitions'
const INSTANCE_API = '/v1/process/instances'
const ACTION_API = '/v1/process/tasks'

function idempotencyKey(scope: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${scope}-${crypto.randomUUID()}`
  }

  return `${scope}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const workflowService = {
  listDefinitions(): Promise<WorkflowDefinition[]> {
    return get<WorkflowDefinition[]>(DEFINITION_API)
  },

  createDefinition(
    payload: SaveWorkflowDefinitionRequest,
  ): Promise<WorkflowDefinition> {
    return post<WorkflowDefinition, SaveWorkflowDefinitionRequest>(
      DEFINITION_API,
      payload,
      {
        dedupeKey: `workflow:definition:create:${payload.code}`,
        idempotencyKey: idempotencyKey('workflow-definition-create'),
      },
    )
  },

  updateDefinition(
    definitionId: string,
    payload: SaveWorkflowDefinitionRequest,
  ): Promise<WorkflowDefinition> {
    return put<WorkflowDefinition, SaveWorkflowDefinitionRequest>(
      `${DEFINITION_API}/${definitionId}`,
      payload,
      {
        dedupeKey: `workflow:definition:update:${definitionId}`,
        idempotencyKey: idempotencyKey('workflow-definition-update'),
      },
    )
  },

  publishDefinition(definitionId: string, publishedBy?: string) {
    return put<WorkflowDefinition, { publishedBy?: string }>(
      `${DEFINITION_API}/${definitionId}/publish`,
      publishedBy ? { publishedBy } : {},
      {
        dedupeKey: `workflow:definition:publish:${definitionId}`,
        idempotencyKey: idempotencyKey('workflow-definition-publish'),
      },
    )
  },

  startProcess(payload: StartProcessRequest): Promise<ProcessInstanceDetail> {
    return post<ProcessInstanceDetail, StartProcessRequest>(
      INSTANCE_API,
      payload,
      {
        dedupeKey: `workflow:instance:start:${payload.definitionId}:${payload.businessKey ?? payload.title}`,
        idempotencyKey: idempotencyKey('workflow-instance-start'),
      },
    )
  },

  getInstance(instanceId: string): Promise<ProcessInstanceDetail> {
    return get<ProcessInstanceDetail>(`${INSTANCE_API}/${instanceId}`)
  },

  getTimeline(instanceId: string): Promise<ProcessInstanceDetail> {
    return get<ProcessInstanceDetail>(`${INSTANCE_API}/${instanceId}/timeline`)
  },

  listActions(taskId: string): Promise<ActionDefinition[]> {
    return get<ActionDefinition[]>(`${ACTION_API}/${taskId}/actions`)
  },

  executeAction(
    taskId: string,
    payload: ExecuteActionRequest,
  ): Promise<unknown> {
    return post<unknown, ExecuteActionRequest>(
      `${ACTION_API}/${taskId}/actions/execute`,
      payload,
      {
        dedupeKey: `workflow:task-action:${taskId}:${payload.actionCode}`,
        idempotencyKey: idempotencyKey('workflow-task-action'),
      },
    )
  },
}
