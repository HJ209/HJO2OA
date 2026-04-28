import type { PaginationQuery } from '@/types/api'

export type OrgStatus = 'ACTIVE' | 'DISABLED'
export type AccountStatus = 'ACTIVE' | 'LOCKED' | 'DISABLED'
export type AuthEffect = 'ALLOW' | 'DENY'
export type SyncStatus = 'SUCCESS' | 'FAILED' | 'RUNNING'

export interface OrgStructure {
  id: string
  parentId?: string | null
  name: string
  code: string
  type: 'COMPANY' | 'DEPARTMENT' | 'TEAM'
  status: OrgStatus
  sortOrder: number
  children?: OrgStructure[]
}

export interface OrgStructurePayload {
  parentId?: string | null
  name: string
  code: string
  type: OrgStructure['type']
  status: OrgStatus
  sortOrder: number
}

export interface PersonAccount {
  id: string
  accountName: string
  displayName: string
  email?: string
  mobile?: string
  orgId?: string
  orgName?: string
  status: AccountStatus
  updatedAtUtc?: string
}

export interface PersonAccountPayload {
  accountName: string
  displayName: string
  email?: string
  mobile?: string
  orgId?: string
  status: AccountStatus
}

export interface PositionAssignment {
  id: string
  personId: string
  personName: string
  orgId: string
  orgName: string
  positionId: string
  positionName: string
  primary: boolean
  effectiveFromUtc?: string
  effectiveToUtc?: string
}

export interface PositionAssignmentPayload {
  personId: string
  orgId: string
  positionId: string
  primary: boolean
  effectiveFromUtc?: string
  effectiveToUtc?: string
}

export interface Role {
  id: string
  code: string
  name: string
  description?: string
  enabled: boolean
}

export interface RolePayload {
  code: string
  name: string
  description?: string
  enabled: boolean
}

export interface ResourceNode {
  id: string
  parentId?: string | null
  name: string
  code: string
  type: 'MENU' | 'BUTTON' | 'API'
  effect: AuthEffect
  checked?: boolean
  children?: ResourceNode[]
}

export interface DataPermissionPolicy {
  id: string
  name: string
  code: string
  scopeType: 'SELF' | 'ORG' | 'ORG_AND_CHILDREN' | 'CUSTOM'
  targetRoleId?: string
  enabled: boolean
  updatedAtUtc?: string
}

export interface DataPermissionPolicyPayload {
  name: string
  code: string
  scopeType: DataPermissionPolicy['scopeType']
  targetRoleId?: string
  enabled: boolean
}

export interface SyncAuditRecord {
  id: string
  sourceSystem: string
  batchNo: string
  status: SyncStatus
  totalCount: number
  successCount: number
  failedCount: number
  startedAtUtc?: string
  finishedAtUtc?: string
  message?: string
}

export interface IdentityContext {
  accountId: string
  displayName: string
  orgId?: string
  roleIds: string[]
}

export interface ListQuery extends PaginationQuery {
  keyword?: string
}
