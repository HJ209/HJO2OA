# position-assignment API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 岗位查询 | 查询岗位列表、详情和影响摘要 | `GET /api/org-perm/positions`、`GET /api/org-perm/positions/{positionId}` |
| 岗位写入 | 创建、编辑、停用岗位 | `POST /api/org-perm/positions`、`PUT /api/org-perm/positions/{positionId}`、`POST /api/org-perm/positions/{positionId}/status` |
| 任职查询 | 查询人员任职、岗位任职和主兼岗状态 | `GET /api/org-perm/persons/{personId}/assignments`、`GET /api/org-perm/positions/{positionId}/assignments` |
| 任职写入 | 新增任职、切换主岗、移除任职 | `POST /api/org-perm/persons/{personId}/assignments`、`POST /api/org-perm/persons/{personId}/assignments/primary`、`DELETE /api/org-perm/persons/{personId}/assignments/{assignmentId}` |
| 角色来源与预览 | 查看岗位继承来源和变更影响 | `GET /api/org-perm/persons/{personId}/effective-role-sources`、`POST /api/org-perm/positions/{positionId}/impact-preview` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/positions` | 岗位分页查询 | `organizationId`、`departmentId`、`keyword`、`category`、`status`、`pageNo`、`pageSize` | 返回岗位基础信息、任职人数摘要、角色来源数量摘要 |
| `GET` | `/api/org-perm/positions/{positionId}` | 岗位详情 | 路径参数 `positionId` | 返回岗位归属、类别、等级、状态、任职摘要 |
| `GET` | `/api/org-perm/persons/{personId}/assignments` | 查询某人员的全部任职 | `includeInactive` 可选 | 返回主岗、兼岗、起止时间、状态和当前是否可切换 |
| `GET` | `/api/org-perm/positions/{positionId}/assignments` | 查询某岗位上的任职列表 | `status`、`pageNo`、`pageSize` | 返回在岗人员、主岗/兼岗标识、起止时间 |
| `GET` | `/api/org-perm/persons/{personId}/effective-role-sources` | 查看人员最终角色来源 | `personId` 必填 | 返回主岗继承角色、兼岗继承角色、人员直授角色的分层来源摘要 |
| `POST` | `/api/org-perm/positions/{positionId}/impact-preview` | 预览岗位停用或归属变更影响 | `action`、`targetOrganizationId`、`targetDepartmentId` | 返回受影响任职数、需重算身份数、缓存失效范围 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/positions` | 新增岗位 | `code`、`name`、`organizationId`、`departmentId`、`category`、`level`、`sortOrder` | `departmentId` 可空；若存在，必须与 `organizationId` 一致 |
| `PUT` | `/api/org-perm/positions/{positionId}` | 编辑岗位 | 同创建字段 + `version` | 岗位归属变化前建议先做影响预览；不得把停用组织/部门作为新归属 |
| `POST` | `/api/org-perm/positions/{positionId}/status` | 启停用岗位 | `status`、`reason`、`version` | 停用不清理历史任职，但新任职校验必须失败 |
| `POST` | `/api/org-perm/persons/{personId}/assignments` | 新增任职 | `positionId`、`type`、`startDate`、`endDate` | 主岗只能有一个；同人同岗不可重复；人员和岗位都必须有效 |
| `PUT` | `/api/org-perm/persons/{personId}/assignments/{assignmentId}` | 编辑任职时间或状态 | `startDate`、`endDate`、`status`、`version` | 不允许直接通过编辑把兼岗改成主岗，主岗变更必须走专用接口 |
| `POST` | `/api/org-perm/persons/{personId}/assignments/primary` | 切换主岗 | `newPrimaryAssignmentId`、`reason`、`version` | 必须是该人员已存在且有效的任职关系；旧主岗自动降为兼岗或按请求移除 |
| `DELETE` | `/api/org-perm/persons/{personId}/assignments/{assignmentId}` | 移除任职 | `reason`、`version` | 若移除的是主岗，必须已指定新的主岗或返回错误 |

## 通用约束

### 筛选与分页

- 岗位列表统一支持按组织、部门、类别、状态和关键字组合筛选。
- 任职列表默认按 `type desc, startDate desc` 排序，确保主岗优先展示。
- 分页参数统一为 `pageNo/pageSize`，`pageSize` 一期建议上限 `100`。

### 批量操作

- 一期不提供通用“批量改主岗”接口，避免在一个请求内混合多个人员的主岗约束。
- 如需支持批量调岗，应在人员页编排入口触发逐人校验并返回逐条结果，不得无差别覆盖任职关系。
- 同步导入岗位和任职时，仍须复用本模块单条约束校验逻辑。

### 幂等要求

- 岗位创建、任职创建、主岗切换和任职移除都应支持 `X-Idempotency-Key`。
- 任职写接口必须携带乐观锁 `version` 或聚合版本，防止多人同时操作同一人员任职集。
- 对于“主岗切换”这类高风险动作，幂等重复提交只能返回同一结果，不能生成额外事件。

### 权限与审计约束

- 岗位维护接口需区分 `position.read`、`position.write`、`position.disable` 三类资源权限。
- 任职维护接口需区分 `assignment.read`、`assignment.write`、`assignment.primary-switch` 三类资源权限。
- 所有写操作必须审计 `operatorId`、`personId/positionId`、变更原因、变更前后任职集合摘要和来源渠道。
- 角色来源查询只读展示，不授予岗位角色绑定写权限。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 岗位编码重复 | `409` | `POSITION_CODE_DUPLICATED` | 同租户岗位编码冲突 |
| 岗位组织与部门不一致 | `400` | `POSITION_ORG_DEPT_MISMATCH` | 目标部门不属于所选组织 |
| 人员重复任职同一岗位 | `409` | `ASSIGNMENT_DUPLICATED` | 同人同岗只能有一条有效任职 |
| 缺失主岗或出现多个主岗 | `400` | `PRIMARY_ASSIGNMENT_INVALID` | 不满足唯一主岗约束 |
| 尝试移除唯一主岗但未指定替代 | `409` | `PRIMARY_ASSIGNMENT_REQUIRED` | 先切换主岗再移除 |
| 岗位或人员已停用/离职 | `409` | `ASSIGNMENT_SUBJECT_INACTIVE` | 禁止新增或切换到无效主体 |
| 试图在本模块写入岗位角色绑定 | `403` | `POSITION_ROLE_WRITE_FORBIDDEN` | 配置入口归属 `role-resource-auth` |
