export interface MonitoredInstance {
  instanceId: string
  definitionId: string
  definitionCode: string
  title: string
  category?: string
  initiatorId: string
  status: string
  startTime: string
  endTime?: string
  updatedAt: string
}

export interface ExceptionInstance {
  instanceId: string
  definitionId: string
  definitionCode: string
  title: string
  category?: string
  status: string
  exceptionType: string
  exceptionMinutes: number
  detectedAt: string
}

export interface NodeTrail {
  taskId: string
  instanceId: string
  nodeId: string
  nodeName: string
  nodeType: string
  assigneeId?: string
  taskStatus: string
  createdAt: string
  claimTime?: string
  completedTime?: string
  dueTime?: string
  lastActionCode?: string
  lastActionName?: string
  lastOperatorId?: string
  lastActionAt?: string
}

export interface ProcessIntervention {
  interventionId: string
  instanceId: string
  taskId?: string
  actionType: string
  operatorId: string
  targetAssigneeId?: string
  reason?: string
  createdAt: string
}

export interface InterventionRequest {
  tenantId: string
  taskId?: string
  actionType: 'SUSPEND' | 'RESUME' | 'TERMINATE' | 'REASSIGN_TASK'
  operatorId: string
  targetAssigneeId?: string
  reason?: string
}
