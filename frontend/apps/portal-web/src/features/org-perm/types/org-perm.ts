import type { PaginationQuery } from '@/types/api'

export type OrgStatus = 'ACTIVE' | 'DISABLED'
export type DeptStatus = 'ACTIVE' | 'DISABLED'
export type PersonStatus = 'ACTIVE' | 'DISABLED' | 'RESIGNED'
export type AccountStatus = 'ACTIVE' | 'LOCKED' | 'DISABLED'
export type BackendAccountStatus = 'ACTIVE' | 'DISABLED'
export type AccountType = 'PASSWORD' | 'LDAP' | 'SSO' | 'WECHAT' | 'DINGTALK'
export type PositionCategory =
  | 'MANAGEMENT'
  | 'PROFESSIONAL'
  | 'TECHNICAL'
  | 'OPERATIONAL'
  | 'OTHER'
export type PositionStatus = 'ACTIVE' | 'DISABLED'
export type AssignmentType = 'PRIMARY' | 'SECONDARY' | 'PART_TIME'
export type AssignmentStatus = 'ACTIVE' | 'INACTIVE'
export type AuthEffect = 'ALLOW' | 'DENY'
export type SyncStatus = 'SUCCESS' | 'FAILED' | 'RUNNING'
export type ResourceType = 'MENU' | 'BUTTON' | 'API' | 'DATA_RESOURCE'
export type ResourceAction =
  | 'READ'
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'EXPORT'
  | 'IMPORT'
  | 'APPROVE'
export type PermissionSubjectType =
  | 'ORGANIZATION'
  | 'DEPARTMENT'
  | 'ROLE'
  | 'PERSON'
  | 'POSITION'

export interface OrgStructure {
  id: string
  parentId?: string | null
  name: string
  code: string
  shortName?: string | null
  type: 'COMPANY' | 'DEPARTMENT' | 'TEAM'
  level?: number
  path?: string
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

export interface Department {
  id: string
  code: string
  name: string
  organizationId: string
  parentId?: string | null
  level: number
  path: string
  managerId?: string | null
  sortOrder: number
  status: DeptStatus
}

export interface DepartmentPayload {
  code: string
  name: string
  organizationId: string
  parentId?: string | null
  managerId?: string | null
  sortOrder: number
}

export interface MoveNodePayload {
  parentId?: string | null
  sortOrder?: number
}

export interface PersonAccount {
  id: string
  accountName: string
  displayName: string
  employeeNo?: string
  email?: string
  mobile?: string
  orgId?: string
  orgName?: string
  departmentId?: string
  status: AccountStatus
  personStatus?: PersonStatus
  updatedAtUtc?: string
}

export interface PersonAccountPayload {
  accountName: string
  displayName: string
  email?: string
  mobile?: string
  orgId?: string
  departmentId?: string
  status: AccountStatus
}

export interface PersonProfile {
  id: string
  employeeNo: string
  name: string
  pinyin?: string | null
  gender?: string | null
  mobile?: string | null
  email?: string | null
  organizationId: string
  departmentId?: string | null
  status: PersonStatus
  tenantId: string
  createdAt?: string
  updatedAt?: string
}

export interface Account {
  id: string
  personId: string
  username: string
  accountType: AccountType
  primaryAccount: boolean
  locked: boolean
  lockedUntil?: string | null
  lastLoginAt?: string | null
  lastLoginIp?: string | null
  passwordChangedAt?: string | null
  mustChangePassword: boolean
  status: BackendAccountStatus
  tenantId: string
  createdAt?: string
  updatedAt?: string
}

export interface PersonAccountDetail {
  person: PersonProfile
  accounts: Account[]
}

export interface AccountPayload {
  username: string
  credential: string
  accountType: AccountType
  primaryAccount: boolean
  mustChangePassword: boolean
}

export interface OrgPosition {
  id: string
  code: string
  name: string
  organizationId: string
  departmentId?: string | null
  category: PositionCategory
  level?: number | null
  sortOrder: number
  status: PositionStatus
  tenantId: string
  createdAt?: string
  updatedAt?: string
}

export interface PositionPayload {
  code: string
  name: string
  organizationId: string
  departmentId?: string | null
  category: PositionCategory
  level?: number | null
  sortOrder: number
}

export interface Assignment {
  id: string
  personId: string
  positionId: string
  type: AssignmentType
  startDate?: string | null
  endDate?: string | null
  status: AssignmentStatus
  tenantId: string
  createdAt?: string
  updatedAt?: string
}

export interface AssignmentPayload {
  personId: string
  positionId: string
  type: AssignmentType
  startDate?: string | null
  endDate?: string | null
}

export interface PositionRoleBinding {
  id: string
  positionId: string
  roleId: string
  tenantId: string
  createdAt?: string
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
  status?: AssignmentStatus
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
  type: ResourceType
  action?: ResourceAction
  effect: AuthEffect
  checked?: boolean
  children?: ResourceNode[]
}

export interface ResourcePermissionPayload {
  resourceType: ResourceType
  resourceCode: string
  action: ResourceAction
  effect: AuthEffect
}

export interface ResourceDefinitionPayload {
  resourceType: ResourceType | 'PAGE' | 'RESOURCE_ACTION'
  resourceCode: string
  name: string
  parentCode?: string | null
  sortOrder?: number
  status?: 'ACTIVE' | 'DISABLED'
}

export interface ApiPermissionDecision {
  allowed: boolean
  effect: AuthEffect
  matchedPermissions: ResourceNode[]
  snapshot: {
    roleIds: string[]
    version: number
  }
}

export interface DataPermissionPolicy {
  id: string
  name: string
  code: string
  subjectType: PermissionSubjectType
  subjectId: string
  businessObject?: string
  scopeType:
    | 'ALL'
    | 'SELF'
    | 'ORG'
    | 'ORG_AND_CHILDREN'
    | 'DEPT_AND_CHILDREN'
    | 'CUSTOM'
    | 'CONDITION'
  conditionExpr?: string | null
  effect?: AuthEffect
  priority?: number
  targetRoleId?: string
  enabled: boolean
  updatedAtUtc?: string
}

export interface DataPermissionPolicyPayload {
  subjectType: PermissionSubjectType
  subjectId: string
  businessObject: string
  scopeType: DataPermissionPolicy['scopeType']
  conditionExpr?: string
  effect: AuthEffect
  priority: number
  enabled: boolean
}

export interface FieldPermissionPolicy {
  id: string
  subjectType: PermissionSubjectType
  subjectId: string
  businessObject: string
  usageScenario: string
  fieldCode: string
  action: 'VISIBLE' | 'EDITABLE' | 'EXPORTABLE' | 'DESENSITIZED' | 'HIDDEN'
  effect: AuthEffect
  updatedAtUtc?: string
}

export interface FieldPermissionPayload {
  subjectType: PermissionSubjectType
  subjectId: string
  businessObject: string
  usageScenario: string
  fieldCode: string
  action: FieldPermissionPolicy['action']
  effect: AuthEffect
}

export interface PermissionDecision {
  allowed: boolean
  effect: AuthEffect
  scopeType?: DataPermissionPolicy['scopeType']
  sqlCondition?: string
  matchedPolicies: DataPermissionPolicy[]
}

export interface PermissionIdentityPayload {
  tenantId: string
  personId: string
  organizationId?: string
  departmentId?: string
  positionId: string
  roleIds: string[]
}

export interface FieldMaskResult {
  row: Record<string, unknown>
  decision: {
    hiddenFields: string[]
    desensitizedFields: string[]
    matchedPolicies: FieldPermissionPolicy[]
  }
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
