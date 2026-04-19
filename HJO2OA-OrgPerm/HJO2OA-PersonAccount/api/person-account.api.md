# person-account API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 人员查询 | 查询人员列表、详情和身份摘要 | `GET /api/org-perm/persons`、`GET /api/org-perm/persons/{personId}`、`GET /api/org-perm/persons/{personId}/identity-summary` |
| 人员写入 | 创建、编辑、启停用、离职 | `POST /api/org-perm/persons`、`PUT /api/org-perm/persons/{personId}`、`POST /api/org-perm/persons/{personId}/status` |
| 账号查询 | 查询账号列表、详情和安全状态 | `GET /api/org-perm/accounts`、`GET /api/org-perm/accounts/{accountId}` |
| 账号写入 | 创建、编辑、主账号切换、启停用 | `POST /api/org-perm/accounts`、`PUT /api/org-perm/accounts/{accountId}`、`POST /api/org-perm/persons/{personId}/accounts/{accountId}/primary`、`POST /api/org-perm/accounts/{accountId}/status` |
| 安全操作 | 锁定、解锁、密码重置、登录结果回写 | `POST /api/org-perm/accounts/{accountId}/lock`、`POST /api/org-perm/accounts/{accountId}/unlock`、`POST /api/org-perm/accounts/{accountId}/password-reset` |
| 导入与批量治理 | 人员导入和批量状态切换 | `POST /api/org-perm/person-import/preview`、`POST /api/org-perm/person-import/{jobId}/confirm`、`POST /api/org-perm/persons/status:batch` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/persons` | 人员分页查询 | `organizationId`、`departmentId`、`keyword`、`status`、`pageNo`、`pageSize` | 返回工号、姓名、组织、部门、状态、账号数、主岗摘要 |
| `GET` | `/api/org-perm/persons/{personId}` | 人员详情 | 路径参数 `personId` | 返回基础档案、组织部门信息、状态、联系方式 |
| `GET` | `/api/org-perm/persons/{personId}/identity-summary` | 查询人员身份总览 | 路径参数 `personId` | 聚合返回账号列表、主账号、主岗/兼岗摘要和当前角色来源摘要 |
| `GET` | `/api/org-perm/accounts` | 账号分页查询 | `personId`、`authType`、`locked`、`status`、`keyword`、`pageNo`、`pageSize` | 返回用户名、认证类型、主账号标记、锁定状态、最近登录摘要 |
| `GET` | `/api/org-perm/accounts/{accountId}` | 账号详情 | 路径参数 `accountId` | 返回账号基础信息、安全状态、外部认证绑定信息 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/persons` | 新增人员 | `employeeNo`、`name`、`organizationId`、`departmentId`、`mobile`、`email` | `organizationId` 必填；`departmentId` 若存在必须归属该组织 |
| `PUT` | `/api/org-perm/persons/{personId}` | 编辑人员 | 同创建字段 + `version` | 不允许通过编辑接口绕过状态流转直接恢复离职人员 |
| `POST` | `/api/org-perm/persons/{personId}/status` | 停用或离职人员 | `status`、`reason`、`version` | 切为 `DISABLED/RESIGNED` 时需同步处理关联账号状态 |
| `POST` | `/api/org-perm/accounts` | 新增账号 | `personId`、`username`、`authType`、`credential`、`isPrimary`、`externalIdentity` | 同一人员同一 `authType` 仅允许一个账号 |
| `PUT` | `/api/org-perm/accounts/{accountId}` | 编辑账号 | `status`、`externalIdentity`、`version` | 凭据更新走专用安全接口，普通编辑不直接回传明文密码 |
| `POST` | `/api/org-perm/persons/{personId}/accounts/{accountId}/primary` | 设置主账号 | `reason`、`version` | 同一人员仅一个主账号，切换时需自动撤销旧主账号标记 |
| `POST` | `/api/org-perm/accounts/{accountId}/lock` | 锁定账号 | `reason`、`lockedUntil`、`version` | 手工锁定与自动锁定都需审计 |
| `POST` | `/api/org-perm/accounts/{accountId}/unlock` | 解锁账号 | `reason`、`version` | 离职或停用人员的账号不得解锁为可用状态 |
| `POST` | `/api/org-perm/accounts/{accountId}/password-reset` | 重置密码 | `temporaryPassword` 或 `resetMode`、`mustChangePassword` | 实际密码策略与加密由 `00-安全工具` 执行 |
| `POST` | `/api/org-perm/person-import/preview` | 人员导入预校验 | 文件、`conflictStrategy=REJECT_ONLY` | 一期只允许预校验后确认 |
| `POST` | `/api/org-perm/person-import/{jobId}/confirm` | 确认导入 | `jobId`、`version` | 仅接受有效预校验结果 |

## 通用约束

### 筛选与分页

- 人员查询必须支持按组织、部门、状态、关键字联动筛选。
- 账号查询必须支持按人员、认证类型、锁定状态、状态筛选。
- 列表统一使用 `pageNo/pageSize`，`pageSize` 一期建议上限 `100`。

### 批量操作

- 一期批量接口只覆盖人员导入和人员批量状态切换。
- 批量调岗不在本模块写入范围内，人员页如提供入口，应编排调用 `position-assignment`。
- 批量结果必须返回成功数、失败数和逐条错误清单。

### 幂等要求

- 人员创建、账号创建、状态切换、主账号切换、密码重置接口都应支持 `X-Idempotency-Key`。
- 所有编辑和状态接口必须使用 `version` 控制并发覆盖。
- 登录成功/失败状态回写属于内部调用，也必须具备幂等保护，避免重复累计错误次数。

### 权限与审计约束

- 人员维护接口至少区分 `person.read`、`person.write`、`person.disable`、`person.import`。
- 账号维护接口至少区分 `account.read`、`account.write`、`account.lock`、`account.unlock`、`account.reset-password`。
- 密码重置、主账号切换、人员离职、账号锁定属于高风险动作，必须记录操作人、原因、目标对象和前后状态。
- 一期不对外暴露登录接口；登录成功/失败、锁定阈值处理通过内部服务调用或事件回写完成。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 工号重复 | `409` | `PERSON_EMPLOYEE_NO_DUPLICATED` | 同租户工号唯一 |
| 人员组织与部门不一致 | `400` | `PERSON_ORG_DEPT_MISMATCH` | 所选部门不属于主属组织 |
| 用户名重复 | `409` | `ACCOUNT_USERNAME_DUPLICATED` | 登录用户名全局唯一 |
| 同一人员同一认证类型重复 | `409` | `ACCOUNT_AUTH_TYPE_DUPLICATED` | 一个认证类型只能绑定一个账号 |
| 主账号冲突 | `409` | `PRIMARY_ACCOUNT_CONFLICT` | 同一人员同时只能有一个主账号 |
| 试图解锁离职人员账号 | `409` | `ACCOUNT_OWNER_RESIGNED` | 需先恢复人员状态，不允许直接解锁 |
| 人员身份摘要依赖数据缺失 | `409` | `PERSON_IDENTITY_SUMMARY_INCOMPLETE` | 任职或角色数据异常时返回可追踪错误 |
