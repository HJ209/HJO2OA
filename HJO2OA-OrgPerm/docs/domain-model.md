# 01-组织与权限 领域模型

## 1. 文档目的

本文档细化 `01-组织与权限` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计和接口契约的统一依据。

对应架构决策编号：D06（租户与组织分离）、D07（权限模型）、D08（身份上下文）、D09（01 领域模型优先细化）。

## 2. 领域总览

### 2.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| Organization | 组织/机构，树形层级 | `org-structure/` |
| Department | 部门，树形层级，归属组织 | `org-structure/` |
| Position | 岗位，归属组织 + 部门 | `position-assignment/` |
| Person | 人员档案 | `person-account/` |
| Account | 登录账号 | `person-account/` |
| Role | 角色 | `role-resource-auth/` |
| ResourcePermission | 资源权限策略 | `role-resource-auth/` |
| DataPermission | 行级数据权限策略 | `data-permission/` |
| FieldPermission | 字段级数据权限策略 | `data-permission/` |

### 2.2 核心实体关系

```
Organization ──1:N──> Organization     (上级机构)
Organization ──1:N──> Department       (机构下属部门)
Department   ──1:N──> Department       (上级部门)
Organization ──1:N──> Position        (机构下属岗位)
Department   ──1:N──> Position        (部门下属岗位)
Position     ──M:N──> Role            (岗位关联角色)
Person       ──1:N──> Assignment      (人员任职关系)
Assignment   ──M:1──> Position        (任职岗位)
Person       ──1:N──> Account         (人员登录账号，含主账号)
```

## 3. 核心聚合定义

### 3.1 Organization（组织/机构）

组织是五维模型中的顶层管理结构，支持树形层级。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 组织唯一标识 |
| code | String(64) | UK, NOT NULL | 组织编码，租户内唯一 |
| name | String(128) | NOT NULL | 组织名称 |
| shortName | String(64) | | 组织简称 |
| type | Enum | NOT NULL | 组织类型：GROUP / COMPANY / BRANCH / SUBSIDIARY / DEPARTMENT_ORG |
| parentId | UUID | FK -> Organization.id, NULLABLE | 上级组织，NULL 表示顶层 |
| level | Integer | NOT NULL | 层级深度，顶层为 0 |
| path | String(512) | NOT NULL | 物化路径，如 `/root/org1/org2/` |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 同级排序号 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅用于数据隔离，不参与业务层级） |
| createdAt | Timestamp | NOT NULL | 创建时间 |
| updatedAt | Timestamp | NOT NULL | 更新时间 |

**业务规则**：

- 组织可归属上级组织，形成机构树，无环约束。
- 组织类型仅做分类标记，不决定层级深度上限。
- `tenantId` 仅用于数据隔离查询，组织层级、岗位层级和授权主体计算中不得使用 `tenantId` 作为业务条件。
- 组织停用时，其下属部门、岗位和人员不影响已有数据，但新增关联需校验。
- `path` 字段用于快速查询所有后代，变更层级时需级联更新。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.organization.created` | 组织创建 | organizationId, code, parentId, tenantId |
| `org.organization.updated` | 组织更新 | organizationId, changedFields |
| `org.organization.hierarchy-changed` | 层级变更 | organizationId, oldParentId, newParentId, affectedDescendantIds |
| `org.organization.disabled` | 组织停用 | organizationId |

### 3.2 Department（部门）

部门是组织内部的职能单元，支持树形层级，必须归属一个组织。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 部门唯一标识 |
| code | String(64) | UK, NOT NULL | 部门编码，租户内唯一 |
| name | String(128) | NOT NULL | 部门名称 |
| organizationId | UUID | FK -> Organization.id, NOT NULL | 所属组织 |
| parentId | UUID | FK -> Department.id, NULLABLE | 上级部门，NULL 表示组织下顶级部门 |
| level | Integer | NOT NULL | 层级深度 |
| path | String(512) | NOT NULL | 物化路径 |
| managerId | UUID | FK -> Person.id, NULLABLE | 部门负责人 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 同级排序号 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅数据隔离） |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 部门必须归属一个组织，不可游离。
- 部门支持树形层级，无环约束。
- 部门负责人必须是同组织下的人员。
- 部门停用时，其下属岗位和人员关联不自动清除，但影响新增校验。
- 组织停用时，其下属部门建议同步停用。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.department.created` | 部门创建 | departmentId, organizationId, parentId |
| `org.department.updated` | 部门更新 | departmentId, changedFields |
| `org.department.hierarchy-changed` | 层级变更 | departmentId, oldParentId, newParentId |
| `org.department.disabled` | 部门停用 | departmentId |

### 3.3 Position（岗位）

岗位同时归属组织和部门，是角色继承和身份上下文的关键锚点。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 岗位唯一标识 |
| code | String(64) | UK, NOT NULL | 岗位编码，租户内唯一 |
| name | String(128) | NOT NULL | 岗位名称 |
| organizationId | UUID | FK -> Organization.id, NOT NULL | 所属组织 |
| departmentId | UUID | FK -> Department.id, NULLABLE | 所属部门（可为空，表示组织级岗位） |
| category | Enum | NOT NULL | 岗位类别：MANAGEMENT / PROFESSIONAL / TECHNICAL / OPERATIONAL / OTHER |
| level | Integer | NULLABLE | 岗位等级（用于排序和比较） |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 同级排序号 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅数据隔离） |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 岗位必须归属一个组织，可选归属一个部门。
- 岗位与角色的关联通过 `PositionRole` 实体实现（M:N）。
- 岗位停用时，该岗位上的任职关系不自动清除，但影响新增校验。
- 岗位变更（组织/部门归属变化）时，需触发身份上下文重算。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.position.created` | 岗位创建 | positionId, organizationId, departmentId |
| `org.position.updated` | 岗位更新 | positionId, changedFields |
| `org.position.disabled` | 岗位停用 | positionId |

### 3.4 Person（人员）

人员是五维模型中的核心实体，承载人员档案和任职关系。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 人员唯一标识 |
| employeeNo | String(64) | UK, NOT NULL | 工号，租户内唯一 |
| name | String(64) | NOT NULL | 姓名 |
| pinyin | String(128) | | 姓名拼音（用于搜索排序） |
| gender | Enum | NULLABLE | 性别：MALE / FEMALE / OTHER |
| mobile | String(32) | NULLABLE | 手机号 |
| email | String(128) | NULLABLE | 邮箱 |
| organizationId | UUID | FK -> Organization.id, NOT NULL | 主属组织 |
| departmentId | UUID | FK -> Department.id, NULLABLE | 主属部门 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED / RESIGNED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅数据隔离） |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 人员必须归属一个组织（主属组织），可选归属一个部门（主属部门）。
- 人员通过 Assignment 关联岗位，主岗唯一且必选，兼岗可多个。
- 人员的角色通过主岗和兼岗继承，不直接存储在 Person 上。
- 人员停用或离职时，其账号同步锁定，任职关系保留但标记为历史。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.person.created` | 人员创建 | personId, employeeNo, organizationId |
| `org.person.updated` | 人员更新 | personId, changedFields |
| `org.person.disabled` | 人员停用 | personId |
| `org.person.resigned` | 人员离职 | personId |

### 3.5 Assignment（任职关系）

任职关系是人员与岗位之间的关联实体，是身份上下文和角色继承的核心。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 任职关系唯一标识 |
| personId | UUID | FK -> Person.id, NOT NULL | 人员 |
| positionId | UUID | FK -> Position.id, NOT NULL | 岗位 |
| type | Enum | NOT NULL | 任职类型：PRIMARY / SECONDARY |
| startDate | Date | NULLABLE | 任职开始日期 |
| endDate | Date | NULLABLE | 任职结束日期 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / INACTIVE |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅数据隔离） |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 每个人员有且仅有一个 `type=PRIMARY` 的任职关系，主岗必选且唯一。
- 兼岗数量不限，但同一人员不可重复任职同一岗位。
- **身份上下文不在任职关系中持久化**：`isDefault` 不作为 Assignment 的静态字段，而是身份上下文的运行时会话状态（详见《身份上下文协议》）。
- 默认身份上下文为主岗（`type=PRIMARY`），用户可切换到兼岗，切换结果仅存于会话/缓存中。
- 切换身份上下文时发布 `org.identity.switched` 事件，触发菜单、待办、权限等刷新。
- 任职失效、岗位停用、组织停用、人员停用、账号锁定等导致当前会话上下文不可继续使用时，由 `identity-context` 发布 `org.identity-context.invalidated`，统一触发下游回退或登出。
- 任职关系变更（新增/删除/主岗变更）必须触发身份上下文重算和权限重算。
- 任职结束日期到达后自动标记为 INACTIVE。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.assignment.created` | 任职关系创建 | assignmentId, personId, positionId, type |
| `org.assignment.primary-changed` | 主岗变更 | personId, oldPositionId, newPositionId |
| `org.assignment.removed` | 任职关系移除 | assignmentId, personId, positionId |
| `org.assignment.expired` | 任职到期 | assignmentId, personId, positionId |

### 3.6 Account（账号）

账号是人员的登录身份，与人员档案分离。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 账号唯一标识 |
| personId | UUID | FK -> Person.id, NOT NULL | 关联人员（1:N，一人可多号） |
| username | String(64) | UK, NOT NULL | 登录用户名，全局唯一 |
| credential | String(256) | NOT NULL | 凭据（加密存储） |
| authType | Enum | NOT NULL | 认证类型：PASSWORD / LDAP / SSO / WECHAT / DINGTALK |
| isPrimary | Boolean | NOT NULL, DEFAULT FALSE | 是否为该人员的主账号（每人仅一个主账号） |
| locked | Boolean | NOT NULL, DEFAULT FALSE | 是否锁定 |
| lockedUntil | Timestamp | NULLABLE | 锁定截止时间 |
| lastLoginAt | Timestamp | NULLABLE | 最后登录时间 |
| lastLoginIp | String(64) | NULLABLE | 最后登录 IP |
| passwordChangedAt | Timestamp | NULLABLE | 密码最后修改时间 |
| mustChangePassword | Boolean | NOT NULL, DEFAULT FALSE | 是否强制修改密码 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅数据隔离） |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 账号与人员 1:N 关联，一个人员可拥有多个账号（如密码账号、企业微信账号、钉钉账号等），但每人仅一个主账号（`isPrimary=TRUE`）。
- 人员停用时所有关联账号同步锁定。
- 同一认证类型下，同一人员最多一个账号。
- 密码策略、锁定策略由 `00-安全工具` 统一管理。
- 账号锁定后不可登录，但人员档案数据不受影响。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.account.created` | 账号创建 | accountId, personId, username, authType |
| `org.account.locked` | 账号锁定 | accountId, reason, lockedUntil |
| `org.account.unlocked` | 账号解锁 | accountId |
| `org.account.login-succeeded` | 登录成功 | accountId, loginIp |
| `org.account.login-failed` | 登录失败 | accountId, reason |

### 3.7 Role（角色）

角色是权限分配的命名集合，优先与岗位关联。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 角色唯一标识 |
| code | String(64) | UK, NOT NULL | 角色编码，租户内唯一 |
| name | String(128) | NOT NULL | 角色名称 |
| category | Enum | NOT NULL | 角色类别：SYSTEM / BUSINESS / PLATFORM_ADMIN |
| scope | Enum | NOT NULL | 角色作用域：GLOBAL / ORGANIZATION / DEPARTMENT |
| description | String(512) | NULLABLE | 角色说明 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户（仅数据隔离） |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**关联实体**：

#### PositionRole（岗位-角色关联）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| positionId | UUID | FK -> Position.id, NOT NULL | 岗位 |
| roleId | UUID | FK -> Role.id, NOT NULL | 角色 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |

- UK 约束：(positionId, roleId)

#### PersonRole（人员-角色直接授权，例外机制）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| personId | UUID | FK -> Person.id, NOT NULL | 人员 |
| roleId | UUID | FK -> Role.id, NOT NULL | 角色 |
| reason | String(256) | NULLABLE | 直接授权原因（审计要求） |
| expiresAt | Timestamp | NULLABLE | 授权过期时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |

- UK 约束：(personId, roleId)

**业务规则**：

- 角色优先与岗位关联，人员通过任职关系继承角色。
- 人员直接授权角色仅作为例外机制，必须填写原因（审计要求）。
- 角色停用时，所有通过该角色获得的权限同步失效。
- 角色作用域决定其可分配的权限范围：GLOBAL 角色可分配全局资源，ORGANIZATION 角色仅限组织内资源。
- 角色继承计算顺序：先计算岗位关联角色，再叠加人员直接授权角色，去重后得到最终权限集合。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `org.role.created` | 角色创建 | roleId, code, category |
| `org.role.updated` | 角色更新 | roleId, changedFields |
| `org.role.disabled` | 角色停用 | roleId |
| `org.role.position-bound` | 岗位绑定角色 | positionId, roleId |
| `org.role.position-unbound` | 岗位解绑角色 | positionId, roleId |
| `org.role.person-granted` | 人员直接授权 | personId, roleId, reason |
| `org.role.person-revoked` | 人员撤销授权 | personId, roleId |

## 4. 权限模型（D07）

### 4.1 三层权限架构

| 层级 | 模型 | 管辖范围 | 授权主体 |
|------|------|----------|----------|
| 资源权限 | RBAC | 菜单、按钮、接口、页面、资源动作 | 角色 |
| 行级数据权限 | ABAC / 策略化 | 数据记录的可见范围 | 组织、部门、岗位、人员、角色 |
| 字段级数据权限 | 独立字段授权 | 字段的查看/编辑/导出权限 | 组织、部门、岗位、人员、角色 |

### 4.2 ResourcePermission（资源权限策略）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| roleId | UUID | FK -> Role.id, NOT NULL | 授权角色 |
| resourceType | Enum | NOT NULL | 资源类型：MENU / BUTTON / API / PAGE / RESOURCE_ACTION |
| resourceCode | String(128) | NOT NULL | 资源编码 |
| action | Enum | NOT NULL | 动作：READ / CREATE / UPDATE / DELETE / APPROVE / EXPORT / IMPORT |
| effect | Enum | NOT NULL, DEFAULT ALLOW | 效果：ALLOW / DENY |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |

- UK 约束：(roleId, resourceType, resourceCode, action)

### 4.3 DataPermission（行级数据权限策略）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| subjectType | Enum | NOT NULL | 授权主体类型：ORGANIZATION / DEPARTMENT / POSITION / PERSON / ROLE |
| subjectId | UUID | NOT NULL | 授权主体 ID |
| businessObject | String(128) | NOT NULL | 业务对象编码（如 `process_instance`, `content_article`） |
| scopeType | Enum | NOT NULL | 范围类型：ALL / ORG_AND_CHILDREN / DEPT_AND_CHILDREN / SELF / CUSTOM / CONDITION |
| conditionExpr | String(1024) | NULLABLE | 条件表达式（scopeType=CONDITION 时使用） |
| effect | Enum | NOT NULL, DEFAULT ALLOW | 效果：ALLOW / DENY |
| priority | Integer | NOT NULL, DEFAULT 0 | 优先级，数值越大越优先 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |

### 4.4 FieldPermission（字段级数据权限策略）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| subjectType | Enum | NOT NULL | 授权主体类型：ORGANIZATION / DEPARTMENT / POSITION / PERSON / ROLE |
| subjectId | UUID | NOT NULL | 授权主体 ID |
| businessObject | String(128) | NOT NULL | 业务对象编码 |
| usageScenario | String(64) | NOT NULL | 使用场景（如 `view`, `edit`, `export`, `print`） |
| fieldCode | String(128) | NOT NULL | 字段编码 |
| action | Enum | NOT NULL | 动作：VISIBLE / EDITABLE / EXPORTABLE / DESENSITIZED / HIDDEN |
| effect | Enum | NOT NULL, DEFAULT ALLOW | 效果：ALLOW / DENY |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |

- UK 约束：(subjectType, subjectId, businessObject, usageScenario, fieldCode, action)

### 4.5 权限判定顺序

1. **身份上下文判定**：默认使用主岗，显式切换后使用兼岗上下文。
2. **资源权限判定**：根据当前身份继承的角色集合，判定是否有权访问菜单/按钮/接口/动作。
3. **行级数据权限判定**：根据当前身份的组织、部门、岗位、人员、角色，裁剪可访问记录范围。
4. **字段级数据权限判定**：根据当前身份和业务场景，裁剪可查看、可编辑、可导出、可脱敏、可隐藏的字段范围。

### 4.6 策略冲突原则

- 显式拒绝（DENY）优先于显式允许（ALLOW）。
- 更具体的授权主体优先于更宽泛的授权主体（人员 > 岗位 > 部门 > 组织）。
- 字段级限制优先于页面展示层的默认能力。
- 所有权限决策都必须保留可审计依据。

## 5. 身份上下文协议（D08）

### 5.1 IdentityContext（身份上下文）

身份上下文是权限判定的入口，所有业务模块必须基于此上下文进行访问控制。

| 字段 | 类型 | 说明 |
|------|------|------|
| personId | UUID | 当前人员 |
| accountId | UUID | 当前账号 |
| tenantId | UUID | 当前租户 |
| currentOrganizationId | UUID | 当前身份所属组织 |
| currentDepartmentId | UUID | NULLABLE，当前身份所属部门 |
| currentPositionId | UUID | 当前身份所在岗位（主岗或切换后的兼岗） |
| currentPositionType | Enum | PRIMARY / SECONDARY |
| inheritedRoleIds | List\<UUID\> | 当前身份继承的角色 ID 集合 |
| effectiveAt | Timestamp | 上下文生效时间 |

### 5.2 身份切换规则

- **默认身份**：主岗身份，系统自动计算。
- **切换触发**：用户主动切换到兼岗身份。
- **切换后必须刷新的上下文**：
  - 菜单权限（可见菜单列表）
  - 待办列表与待办数量
  - 消息与通知范围
  - 数据访问范围（行级权限裁剪）
  - 门户卡片数据
  - 组织通讯录上下文
- **切换后必须失效的缓存**：角色缓存、资源权限缓存、数据权限缓存、字段权限缓存。
- **切换后必须重算的权限**：菜单树、按钮权限集、数据范围条件、字段可见性矩阵。

### 5.3 身份上下文生命周期

```
登录 ──> 计算主岗上下文 ──> 缓存权限
  │
  ├── 切换兼岗 ──> 重算上下文 ──> 刷新缓存 ──> 通知前端刷新
  │
  ├── 切回主岗 ──> 重算上下文 ──> 刷新缓存 ──> 通知前端刷新
  │
  └── 任职/岗位/账号后台事件 ──> 异步重算或回退上下文 ──> 刷新缓存 ──> 发布 `org.identity-context.invalidated`
```

## 6. 租户与组织分离约束（D06）

### 6.1 分离原则

| 概念 | 职责 | 管理模块 | 参与计算 |
|------|------|----------|----------|
| 租户 | 系统隔离边界、资源隔离、配置隔离、配额隔离 | `00-平台基础设施` | 仅用于数据隔离查询 |
| 组织 | 业务管理结构、层级管理、授权主体 | `01-组织与权限` | 参与组织层级、岗位层级、授权主体计算 |

### 6.2 约束规则

- 租户 ID 作为数据隔离字段存在于所有业务表中，但不得作为组织层级、岗位层级和授权主体的判断条件。
- 组织树、部门树、岗位树的层级计算完全在租户内部完成，不跨租户。
- 权限判定时，先按租户隔离数据，再按组织/部门/岗位/人员/角色进行授权计算。
- 前端不得将租户选择与组织层级选择混用。

## 7. 领域事件汇总

所有领域事件遵循命名规范 `{模块前缀}.{子模块名}.{动作}`，模块前缀为 `org`。

| 事件类型 | 载荷关键字段 | 消费模块 |
|----------|-------------|----------|
| `org.organization.created` | organizationId, code, parentId | 02/04/06 |
| `org.organization.updated` | organizationId, changedFields | 02/04/06 |
| `org.organization.hierarchy-changed` | organizationId, affectedDescendantIds | 01/02/04/06 |
| `org.organization.disabled` | organizationId | 01/02/04/06 |
| `org.department.created` | departmentId, organizationId | 02/04/06 |
| `org.department.updated` | departmentId, changedFields | 02/04/06 |
| `org.department.hierarchy-changed` | departmentId, affectedDescendantIds | 01/02/04/06 |
| `org.department.disabled` | departmentId | 01/02/04/06 |
| `org.position.created` | positionId, organizationId, departmentId | 01/06 |
| `org.position.updated` | positionId, changedFields | 01/06 |
| `org.position.disabled` | positionId | 01/06 |
| `org.assignment.created` | assignmentId, personId, positionId, type | 01/02/04/06 |
| `org.assignment.primary-changed` | personId, oldPositionId, newPositionId | 01/02/04/06 |
| `org.assignment.removed` | assignmentId, personId, positionId | 01/02/04/06 |
| `org.assignment.expired` | assignmentId, personId, positionId | 01/02/04/06 |
| `org.person.created` | personId, employeeNo, organizationId | 02/04/06 |
| `org.person.updated` | personId, changedFields | 02/04/06 |
| `org.person.disabled` | personId | 02/04/06 |
| `org.person.resigned` | personId | 02/04/06 |
| `org.account.created` | accountId, personId, username, authType | 06 |
| `org.account.locked` | accountId, reason | 06 |
| `org.account.unlocked` | accountId | 06 |
| `org.account.login-succeeded` | accountId, loginIp | 00/06 |
| `org.account.login-failed` | accountId, reason | 00 |
| `org.role.created` | roleId, code, category | 01/02 |
| `org.role.updated` | roleId, changedFields | 01/02 |
| `org.role.disabled` | roleId | 01/02 |
| `org.role.position-bound` | positionId, roleId | 01/02 |
| `org.role.position-unbound` | positionId, roleId | 01/02 |
| `org.role.person-granted` | personId, roleId, reason | 01/02 |
| `org.role.person-revoked` | personId, roleId | 01/02 |
| `org.resource-permission.changed` | roleId, resourceTypes, permissionCount, version | 02/03/04/05/06/07 |
| `org.data-permission.row-changed` | policyId, subjectType, subjectId, businessObject, scopeType | 02/03/04/05/06/07 |
| `org.data-permission.field-changed` | policyId, subjectType, subjectId, businessObject, fieldCode, action | 02/03/04/05/06/07 |
| `org.identity.switched` | personId, fromPositionId, toPositionId | 02/04/06 |
| `org.identity-context.invalidated` | personId, invalidatedAssignmentId, fallbackAssignmentId, reasonCode, forceLogout | 02/03/04/06 |
| `org.sync.completed` | syncTaskId, sourceId, syncMode, createdCount, updatedCount, failedCount | 00/07 |
| `org.sync.failed` | syncTaskId, sourceId, syncMode, errorCode, errorSummary | 00/07 |
| `org.audit.org-changed` | auditLogId, entityType, entityId, action, operatorId | 00 |
| `org.audit.auth-changed` | auditLogId, entityType, entityId, action, operatorId | 00 |
| `org.audit.account-changed` | auditLogId, entityType, entityId, action, operatorId | 00 |

## 8. 跨模块依赖

| 依赖方向 | 依赖内容 | 说明 |
|----------|----------|------|
| 01 -> 00 | 事件总线、多租户隔离、审计日志、错误码 | 基础设施依赖 |
| 01 -> 00 | 安全工具（密码策略、加解密、脱敏） | 账号安全依赖 |
| 02 -> 01 | 身份上下文、角色集合、数据权限裁剪 | 流程审批依赖权限判定 |
| 03 -> 01 | 发布范围校验、内容权限判定 | 内容可见范围依赖组织权限 |
| 04 -> 01 | 门户角色模板、聚合数据权限裁剪 | 门户差异化依赖身份上下文 |
| 05 -> 01 | 业务单据权限、流程发起人身份 | 业务应用依赖组织权限 |
| 06 -> 01 | 消息触达范围、移动端身份切换 | 消息路由依赖组织结构 |
| 07 -> 01 | 开放接口授权、数据同步映射 | 数据开放依赖权限模型 |

## 9. 数据库设计建议

### 9.1 核心表清单

| 表名 | 对应聚合根 | 说明 |
|------|-----------|------|
| org_organization | Organization | 组织主表 |
| org_department | Department | 部门主表 |
| org_position | Position | 岗位主表 |
| org_person | Person | 人员主表 |
| org_assignment | Assignment | 任职关系表 |
| org_account | Account | 账号主表 |
| org_role | Role | 角色主表 |
| org_position_role | PositionRole | 岗位-角色关联表 |
| org_person_role | PersonRole | 人员-角色直接授权表 |
| org_resource_permission | ResourcePermission | 资源权限策略表 |
| org_data_permission | DataPermission | 行级数据权限策略表 |
| org_field_permission | FieldPermission | 字段级数据权限策略表 |

### 9.2 索引建议

- `org_organization`：(tenantId, parentId, sortOrder)、(tenantId, path)
- `org_department`：(tenantId, organizationId, parentId, sortOrder)、(tenantId, path)
- `org_position`：(tenantId, organizationId, departmentId)
- `org_person`：(tenantId, organizationId, departmentId)、(tenantId, employeeNo)
- `org_assignment`：(tenantId, personId, type)、(tenantId, positionId)
- `org_account`：(username)、(tenantId, personId)
- `org_role`：(tenantId, category, scope)
- `org_position_role`：(positionId, roleId)
- `org_person_role`：(personId, roleId)
- `org_resource_permission`：(roleId, resourceType, resourceCode)
- `org_data_permission`：(businessObject, subjectType, subjectId)
- `org_field_permission`：(businessObject, usageScenario, fieldCode, subjectType, subjectId)

### 9.3 多租户隔离策略

所有表均包含 `tenantId` 字段，查询时通过 MyBatis Plus 租户拦截器自动注入租户隔离条件。隔离方案已由 ADR-005 确定：**默认采用共享数据库 + 租户字段**，对数据隔离要求极高的大客户可按需切换为独立数据库。
