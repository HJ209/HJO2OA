# 身份上下文协议文档

对应架构决策：D08（身份上下文）、D09（`01` 领域模型）。

## 1. 文档目的

定义 HJO2OA 身份上下文的完整协议，包括上下文构成、切换机制、传递方式、刷新策略和权限重算规则。所有模块涉及身份判定时必须遵循本协议。

## 2. 核心概念

### 2.1 身份上下文定义

身份上下文（Identity Context）是当前操作用户的完整身份快照，用于权限判定、数据范围裁剪和业务联动。

```json
{
  "accountId": "UUID",
  "personId": "UUID",
  "tenantId": "UUID",
  "currentAssignmentId": "UUID",
  "currentPositionId": "UUID",
  "currentOrganizationId": "UUID",
  "currentDepartmentId": "UUID",
  "assignmentType": "PRIMARY | SECONDARY",
  "roleIds": ["UUID"],
  "permissionSnapshotVersion": "Long"
}
```

### 2.2 关键规则

- **默认上下文**：用户登录后自动使用主岗（`type=PRIMARY`）作为默认身份上下文。
- **主岗唯一**：每个人员有且仅有一个主岗任职关系。
- **兼岗可多**：同一人员可拥有多个兼岗，但同一时刻的身份上下文仅对应一个任职关系。
- **身份切换**：用户可从主岗切换到任一兼岗，切换后整个上下文刷新。
- **被动失效**：当当前任职、岗位、组织、人员或账号状态变化导致当前上下文不能继续使用时，必须重算上下文，并发布 `org.identity-context.invalidated`。
- **isDefault 不持久化**：`isDefault` 不作为 Assignment 的静态字段，而是运行时会话状态。

## 3. 上下文生命周期

### 3.1 登录构建

1. 用户通过 Account 登录（账号认证）。
2. 系统查询该 Account 关联的 Person。
3. 查询 Person 的主岗 Assignment（`type=PRIMARY`）。
4. 基于主岗构建默认身份上下文。
5. 计算角色集合（岗位关联角色 + 人员直接授权角色，去重）。
6. 将上下文写入 JWT Claims 和 Redis 缓存。

### 3.2 身份切换

1. 用户选择一个兼岗任职关系。
2. 系统验证该任职关系状态为 ACTIVE。
3. 基于新任职关系重建身份上下文。
4. 更新 JWT Claims（`asgId`、`posId`）以及请求头透传字段（`X-Identity-Assignment-Id`、`X-Identity-Position-Id`），并刷新 Redis 缓存。
5. 发布 `org.identity.switched` 事件。
6. 前端收到事件后刷新菜单、待办、消息、门户卡片、通讯录。

### 3.3 上下文失效与重算

| 触发事件 | 重算内容 |
|----------|----------|
| `org.assignment.primary-changed` | 主岗变更，所有使用主岗上下文的会话需重算 |
| `org.assignment.removed` | 任职移除，若为当前上下文则强制切回主岗或登出，并发布 `org.identity-context.invalidated` |
| `org.assignment.expired` | 任职到期，若为当前上下文则强制切回主岗或登出，并发布 `org.identity-context.invalidated` |
| `org.role.position-bound` / `org.role.position-unbound` | 角色集合变更，权限缓存失效 |
| `org.role.person-granted` / `org.role.person-revoked` | 直接授权变更，权限缓存失效 |
| `org.position.disabled` / `org.organization.disabled` | 当前岗位或所属组织失效时，重算上下文并发布 `org.identity-context.invalidated` |
| `org.person.disabled` / `org.account.locked` | 当前账号不可继续访问，强制登出并发布 `org.identity-context.invalidated` |

### 3.4 被动身份上下文失效事件

当后台主数据变化不是由用户主动切换触发，但会影响当前会话可继续使用的身份时，`identity-context` 必须对外发布统一事件 `org.identity-context.invalidated`。

```json
{
  "eventType": "org.identity-context.invalidated",
  "payload": {
    "personId": "UUID",
    "accountId": "UUID",
    "invalidatedAssignmentId": "UUID",
    "fallbackAssignmentId": "UUID | null",
    "reasonCode": "ASSIGNMENT_REMOVED | ASSIGNMENT_EXPIRED | PRIMARY_CHANGED | POSITION_DISABLED | ORGANIZATION_DISABLED | PERSON_DISABLED | ACCOUNT_LOCKED",
    "forceLogout": false,
    "permissionSnapshotVersion": 42
  }
}
```

处理规则：

1. `identity-context` 接收任职、岗位、组织、人员、账号等上游事件后，先判断是否影响当前激活会话。
2. 若受影响上下文仍可回退到一个合法主岗或兼岗，则先重建上下文、提升 `permissionSnapshotVersion`、失效旧缓存，再发布 `org.identity-context.invalidated`。
3. 若无合法回退目标，或账号/人员已不可继续访问，则设置 `forceLogout=true`，并要求下游终止当前会话。
4. 角色、资源权限、数据权限的纯授权变化仍优先通过既有 `org.role.*`、`org.resource-permission.changed`、`org.data-permission.*` 传播；只有“当前身份本身失效或必须回退/登出”时才发布本事件。

## 4. 上下文传递方式

### 4.1 HTTP 请求头

| 请求头 | 说明 |
|--------|------|
| `Authorization` | Bearer Token，JWT 中包含 accountId、personId、tenantId |
| `X-Identity-Assignment-Id` | 当前身份任职关系 ID，用于唯一标识当前上下文 |
| `X-Identity-Position-Id` | 当前身份岗位 ID，便于网关和下游服务快速识别岗位维度 |
| `X-Tenant-Id` | 租户 ID（网关也可从 Token 提取） |

### 4.2 JWT Claims

```json
{
  "sub": "accountId",
  "pid": "personId",
  "tid": "tenantId",
  "asgId": "currentAssignmentId",
  "posId": "currentPositionId",
  "orgId": "currentOrganizationId",
  "deptId": "currentDepartmentId",
  "asgType": "PRIMARY | SECONDARY",
  "pVer": "permissionSnapshotVersion"
}
```

### 4.3 事件信封

事件信封中的 `operatorAccountId` 和 `operatorPersonId` 标识事件触发者身份，消费端可据此做权限判定。

### 4.4 内部服务调用

跨模块只读查询调用时，通过服务级 Token + `X-Identity-Assignment-Id` + `X-Identity-Position-Id` 传递当前身份上下文。

## 5. 权限重算策略

### 5.1 重算时机

- 身份切换时：立即重算角色集合和权限缓存。
- 角色变更时：标记权限缓存失效，下次请求时懒加载重算。
- 任职变更时：若影响当前上下文则立即重算，否则标记失效。

### 5.2 重算范围

1. 角色集合 = 岗位关联角色 ∪ 人员直接授权角色（去重）
2. 资源权限 = 角色集合对应的资源权限并集
3. 行级数据权限 = 基于当前身份的组织/部门/岗位/人员/角色的数据范围
4. 字段级数据权限 = 基于当前身份和业务场景的字段可见/编辑/导出/脱敏/隐藏范围

### 5.3 缓存策略

- 权限缓存存储在 Redis，Key 格式：`perm:{tenantId}:{personId}:{positionId}`
- 缓存版本号（`permissionSnapshotVersion`）用于乐观锁，变更时递增。
- 缓存 TTL 30 分钟，失效后下次请求触发重算。
- 身份切换时主动删除旧缓存、写入新缓存。

## 6. 前端刷新协议

### 6.1 切换后刷新范围

| 刷新项 | 刷新方式 |
|--------|----------|
| 菜单 | 重新获取菜单树（基于新角色集合） |
| 待办列表 | 重新获取待办（基于新身份的数据范围） |
| 消息 | 重新获取未读消息计数 |
| 门户卡片 | 重新获取聚合数据（基于新身份） |
| 通讯录 | 重新获取组织人员树（基于新组织/部门） |
| 工作台布局 | 若有角色化模板差异则切换 |

### 6.2 刷新触发

- WebSocket 推送 `identity.switched` 事件。
- WebSocket 推送 `identity.context-invalidated` 事件，用于被动回退或强制登出。
- 前端收到后批量刷新，显示加载状态。
- 刷新期间禁止操作，避免上下文不一致。

## 7. 安全约束

- 身份切换必须验证目标任职关系属于当前人员且状态为 ACTIVE。
- 切换操作必须审计留痕（记录切换前后的 positionId）。
- JWT 中不直接存储角色列表（避免 Token 过大和角色变更不同步），角色通过缓存获取。
- 服务端每次鉴权时校验 `permissionSnapshotVersion`，若与缓存版本不一致则触发重算。
- `person.disabled`、`account.locked` 等高风险场景必须通过 `org.identity-context.invalidated(forceLogout=true)` 统一触发下游退出与缓存清理。
