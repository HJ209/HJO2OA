# data-permission API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 行级策略查询 | 查询行级权限策略 | `GET /api/org-perm/data-permissions/row-policies`、`GET /api/org-perm/data-permissions/row-policies/{policyId}` |
| 行级策略写入 | 新增、编辑、删除行级策略 | `POST /api/org-perm/data-permissions/row-policies`、`PUT /api/org-perm/data-permissions/row-policies/{policyId}`、`DELETE /api/org-perm/data-permissions/row-policies/{policyId}` |
| 字段策略查询 | 查询字段级权限策略 | `GET /api/org-perm/data-permissions/field-policies`、`GET /api/org-perm/data-permissions/field-policies/{policyId}` |
| 字段策略写入 | 新增、编辑、删除字段策略 | `POST /api/org-perm/data-permissions/field-policies`、`PUT /api/org-perm/data-permissions/field-policies/{policyId}`、`DELETE /api/org-perm/data-permissions/field-policies/{policyId}` |
| 权限预览 | 预览指定人员或身份的最终权限 | `POST /api/org-perm/data-permissions/preview` |
| 统一决策 | 供业务模块调用统一判定接口 | `POST /api/org-perm/data-permissions/decisions/row`、`POST /api/org-perm/data-permissions/decisions/field` |
| 模板管理 | 维护和应用权限模板 | `GET /api/org-perm/data-permissions/templates`、`POST /api/org-perm/data-permissions/templates`、`POST /api/org-perm/data-permissions/templates/{templateId}/apply` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/data-permissions/row-policies` | 行级策略分页查询 | `subjectType`、`subjectId`、`businessObject`、`scopeType`、`effect`、`pageNo`、`pageSize` | 返回授权主体、范围、优先级、效果、状态 |
| `GET` | `/api/org-perm/data-permissions/field-policies` | 字段策略分页查询 | `subjectType`、`subjectId`、`businessObject`、`usageScenario`、`fieldCode`、`action`、`pageNo`、`pageSize` | 返回字段动作、效果、主体摘要 |
| `POST` | `/api/org-perm/data-permissions/preview` | 预览最终权限 | `personId` 或 `identityContext`、`businessObject`、`usageScenario` | 返回行范围、字段矩阵、命中策略、冲突解释链 |
| `POST` | `/api/org-perm/data-permissions/decisions/row` | 业务侧请求行级决策 | `identityContext`、`businessObject`、`queryIntent` | 返回标准化过滤条件、命中策略列表 |
| `POST` | `/api/org-perm/data-permissions/decisions/field` | 业务侧请求字段决策 | `identityContext`、`businessObject`、`usageScenario`、`fieldCodes[]` | 返回每个字段的动作矩阵和解释信息 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/data-permissions/row-policies` | 新增行级策略 | `subjectType`、`subjectId`、`businessObject`、`scopeType`、`conditionExpr`、`effect`、`priority` | `scopeType=CONDITION` 时 `conditionExpr` 必填；`CUSTOM` 一期不对外开放写入 |
| `PUT` | `/api/org-perm/data-permissions/row-policies/{policyId}` | 编辑行级策略 | 同创建字段 + `version` | 修改优先级或效果需进入审计 |
| `DELETE` | `/api/org-perm/data-permissions/row-policies/{policyId}` | 删除行级策略 | `reason`、`version` | 删除前建议展示影响摘要 |
| `POST` | `/api/org-perm/data-permissions/field-policies` | 新增字段策略 | `subjectType`、`subjectId`、`businessObject`、`usageScenario`、`fieldCode`、`action`、`effect` | 同一主体同一场景同一字段动作不得重复 |
| `PUT` | `/api/org-perm/data-permissions/field-policies/{policyId}` | 编辑字段策略 | 同创建字段 + `version` | `DESENSITIZED` 与 `HIDDEN` 冲突时以后端判定为准 |
| `DELETE` | `/api/org-perm/data-permissions/field-policies/{policyId}` | 删除字段策略 | `reason`、`version` | 删除后需刷新字段动作缓存 |
| `POST` | `/api/org-perm/data-permissions/templates/{templateId}/apply` | 应用权限模板 | `targetSubjectType`、`targetSubjectId`、`businessObjects[]`、`reason` | 模板应用后复制成独立策略，不建立动态关联 |

## 通用约束

### 筛选与分页

- 策略查询必须支持按授权主体、业务对象、范围类型、动作和效果组合筛选。
- 分页统一使用 `pageNo/pageSize`，`pageSize` 一期建议上限 `100`。
- 业务侧统一决策接口不分页，必须在单次请求内返回完整裁剪结果。

### 批量操作

- 模板应用和导入能力属于批量写入，必须返回新建、更新、跳过、失败四类统计。
- 一期不提供“无预览的整主体全量删除”能力，批量移除必须先预览影响范围。
- 业务侧若需要批量字段决策，应在 `fieldCodes[]` 内一次提交，避免对每个字段逐条调用。

### 幂等要求

- 行级/字段策略创建、模板应用和批量变更都应支持 `X-Idempotency-Key`。
- 策略编辑和删除必须使用 `version` 做乐观锁。
- 统一决策接口是纯查询语义，不应产生副作用缓存写入以外的状态变化。

### 权限与审计约束

- 页面和接口权限至少区分 `data-permission.read`、`data-permission.write`、`data-permission.preview`、`data-permission.template-write`。
- 影响数据范围或字段可见性的写操作都必须记录操作原因、命中业务对象和变更前后策略快照。
- 业务模块调用决策接口时，必须传入 `identityContext` 或可追踪的 `personId + accountId`，禁止匿名决策。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 条件策略缺少表达式 | `400` | `ROW_POLICY_CONDITION_REQUIRED` | `scopeType=CONDITION` 时必须提供表达式 |
| 使用了一期未开放的 `CUSTOM` 类型 | `400` | `ROW_POLICY_SCOPE_UNSUPPORTED` | 该枚举暂不开放直接配置 |
| 字段策略重复 | `409` | `FIELD_POLICY_DUPLICATED` | 同主体同业务对象同场景同字段动作不能重复 |
| 主体不存在或已失效 | `409` | `DATA_PERMISSION_SUBJECT_INVALID` | 授权主体无效时不得保存策略 |
| 决策请求缺少身份上下文 | `400` | `IDENTITY_CONTEXT_REQUIRED` | 业务侧必须传上下文或可解析身份 |
| 试图在本模块配置资源权限 | `403` | `RESOURCE_PERMISSION_OUT_OF_SCOPE` | 菜单/按钮/API 权限归属 `role-resource-auth` |
