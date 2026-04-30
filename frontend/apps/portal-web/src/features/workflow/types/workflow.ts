export type DefinitionStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'
export type ProcessInstanceStatus =
  | 'RUNNING'
  | 'COMPLETED'
  | 'TERMINATED'
  | 'SUSPENDED'
export type TaskInstanceStatus =
  | 'CREATED'
  | 'CLAIMED'
  | 'COMPLETED'
  | 'TERMINATED'
  | 'TRANSFERRED'
export type TaskNodeType =
  | 'START'
  | 'USER_TASK'
  | 'SERVICE_TASK'
  | 'EXCLUSIVE_GATEWAY'
  | 'PARALLEL_GATEWAY'
  | 'INCLUSIVE_GATEWAY'
  | 'END'
export type CandidateType =
  | 'PERSON'
  | 'ROLE'
  | 'POSITION'
  | 'DEPARTMENT'
  | 'ORGANIZATION'
  | 'CUSTOM'
export type ActionCategory =
  | 'APPROVE'
  | 'REJECT'
  | 'RETURN'
  | 'WITHDRAW'
  | 'DELEGATE'
  | 'TRANSFER'
  | 'ADD_SIGN'
  | 'REDUCE_SIGN'
  | 'TERMINATE'
  | 'SUSPEND'
  | 'CUSTOM'
export type RouteTarget = 'NEXT_NODE' | 'PREVIOUS_NODE' | 'CURRENT_NODE' | 'END'

export interface WorkflowDefinition {
  id: string
  code: string
  name: string
  category?: string | null
  version: number
  status: DefinitionStatus
  formMetadataId?: string | null
  startNodeId?: string | null
  endNodeId?: string | null
  nodes: string
  routes: string
  tenantId: string
  publishedAt?: string | null
  publishedBy?: string | null
  createdAt: string
  updatedAt: string
}

export interface SaveWorkflowDefinitionRequest {
  code: string
  name: string
  category?: string
  formMetadataId?: string | null
  startNodeId: string
  endNodeId: string
  nodes: unknown
  routes: unknown
}

export interface TaskParticipantRequest {
  assigneeId?: string
  assigneeOrgId?: string
  assigneeDeptId?: string
  assigneePositionId?: string
  candidateType?: CandidateType
  candidateIds?: string[]
}

export interface StartProcessRequest {
  definitionId: string
  businessKey?: string
  title: string
  initiatorId: string
  initiatorOrgId: string
  initiatorDeptId?: string
  initiatorPositionId: string
  formDataId: string
  variables?: Record<string, unknown>
  participants?: TaskParticipantRequest[]
}

export interface ProcessInstance {
  id: string
  definitionId: string
  definitionVersion: number
  definitionCode: string
  businessKey?: string | null
  title: string
  category?: string | null
  initiatorId: string
  initiatorOrgId: string
  initiatorDeptId?: string | null
  initiatorPositionId: string
  formMetadataId: string
  formDataId: string
  currentNodes: string[]
  status: ProcessInstanceStatus
  startTime: string
  endTime?: string | null
  tenantId: string
  idempotencyKey?: string | null
  createdAt: string
  updatedAt: string
}

export interface TaskInstance {
  id: string
  instanceId: string
  nodeId: string
  nodeName: string
  nodeType: TaskNodeType
  assigneeId?: string | null
  assigneeOrgId?: string | null
  assigneeDeptId?: string | null
  assigneePositionId?: string | null
  candidateType?: CandidateType | null
  candidateIds: string[]
  multiInstanceType: 'NONE' | 'PARALLEL' | 'SEQUENTIAL'
  completionCondition?: string | null
  status: TaskInstanceStatus
  claimTime?: string | null
  completedTime?: string | null
  dueTime?: string | null
  tenantId: string
  createdAt: string
  updatedAt: string
}

export interface TaskAction {
  id: string
  taskId: string
  instanceId: string
  actionCode: string
  actionName: string
  operatorId: string
  operatorOrgId: string
  operatorPositionId: string
  opinion?: string | null
  targetNodeId?: string | null
  formDataPatch?: Record<string, unknown>
  createdAt: string
}

export interface NodeHistory {
  id: string
  instanceId: string
  taskId?: string | null
  nodeId: string
  nodeName: string
  nodeType: TaskNodeType
  status: string
  actionCode?: string | null
  operatorId?: string | null
  occurredAt: string
  tenantId: string
}

export interface VariableHistory {
  id: string
  instanceId: string
  taskId?: string | null
  variableName: string
  oldValue?: string | null
  newValue?: string | null
  operatorId?: string | null
  occurredAt: string
  tenantId: string
}

export interface ProcessInstanceDetail {
  instance: ProcessInstance
  tasks: TaskInstance[]
  actions: TaskAction[]
  nodeHistory: NodeHistory[]
  variableHistory: VariableHistory[]
}

export interface ActionDefinition {
  id: string
  code: string
  name: string
  category: ActionCategory
  routeTarget: RouteTarget
  requireOpinion: boolean
  requireTarget: boolean
  uiConfig?: Record<string, unknown>
  tenantId: string
}

export interface ExecuteActionRequest {
  actionCode: string
  opinion?: string
  targetNodeId?: string
  targetAssigneeIds?: string[]
  formDataPatch?: Record<string, unknown>
  operatorAccountId: string
  operatorPersonId: string
  operatorPositionId: string
  operatorOrgId: string
}
