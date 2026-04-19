# identity-context API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 当前上下文查询 | 查询当前会话身份上下文 | `GET /api/org-perm/identity-context/current` |
| 候选身份查询 | 查询主岗和兼岗候选身份 | `GET /api/org-perm/identity-context/available` |
| 身份切换 | 切换到兼岗或切回主岗 | `POST /api/org-perm/identity-context/switch`、`POST /api/org-perm/identity-context/reset-primary` |
| 内部刷新 | 后台重算并刷新上下文缓存 | `POST /api/org-perm/identity-context/refresh` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/identity-context/current` | 查询当前会话上下文 | 无 | 返回 `personId`、`accountId`、`tenantId`、当前组织/部门/岗位、岗位类型、继承角色集合、`effectiveAt` |
| `GET` | `/api/org-perm/identity-context/available` | 查询可切换身份列表 | `includePrimary=true` 默认 | 返回当前主岗和全部有效兼岗，附带组织、部门、岗位名称和可切换标记 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/identity-context/switch` | 切换到指定兼岗 | `targetPositionId`、`reason` | 只能切换到当前人员有效兼岗；切换结果只写会话缓存 |
| `POST` | `/api/org-perm/identity-context/reset-primary` | 切回主岗 | `reason` 可选 | 主岗必须仍然有效；无主岗时返回错误 |
| `POST` | `/api/org-perm/identity-context/refresh` | 内部重算当前上下文 | `personId`、`triggerEvent`、`force` | 仅内部服务或事件消费者可调用；不可由普通前端直接访问 |

## 通用约束

### 查询与返回约束

- 当前上下文查询必须返回完整 `IdentityContext`，不得只返回岗位编号。
- 候选身份列表应明确区分主岗与兼岗，并返回“不可切换原因”，方便前端禁用无效选项。
- 本模块不提供分页接口，候选身份数量受任职模型限制，默认返回全量有效任职。

### 幂等要求

- 身份切换和切回主岗接口都应支持 `X-Idempotency-Key`。
- 重复提交相同目标身份切换请求时，应返回当前上下文，不得重复发布额外事件。
- 后台刷新接口应以 `personId + triggerEvent + eventId` 做幂等控制。

### 权限与审计约束

- 切换接口只允许操作当前登录人的上下文，不允许代他人切换。
- 身份切换、切回主岗和后台强制刷新都必须留下审计记录，记录旧岗位、新岗位、触发来源和原因。
- 内部刷新接口需通过服务级鉴权，普通管理端和门户端不可直接调用。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 当前人员缺少有效主岗 | `409` | `PRIMARY_ASSIGNMENT_MISSING` | 默认上下文无法建立 |
| 目标岗位不属于当前人员有效兼岗 | `403` | `IDENTITY_SWITCH_FORBIDDEN` | 不能切换到他人岗位或无效岗位 |
| 目标任职已到期或岗位已停用 | `409` | `IDENTITY_SWITCH_TARGET_INACTIVE` | 目标身份不可用 |
| 当前账号已锁定或人员已离职 | `409` | `IDENTITY_CONTEXT_UNAVAILABLE` | 不允许建立或切换上下文 |
| 上下文刷新出现部分失败 | `500` | `IDENTITY_REFRESH_PARTIAL_FAILURE` | 需进入补偿重试并保持旧上下文 |
