# role-resource-auth API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 角色查询 | 查询角色列表、详情和继承摘要 | `GET /api/org-perm/roles`、`GET /api/org-perm/roles/{roleId}` |
| 角色写入 | 创建、编辑、停用角色 | `POST /api/org-perm/roles`、`PUT /api/org-perm/roles/{roleId}`、`POST /api/org-perm/roles/{roleId}/status` |
| 岗位绑定 | 绑定或解绑岗位角色 | `GET /api/org-perm/positions/{positionId}/roles`、`POST /api/org-perm/positions/{positionId}/roles`、`DELETE /api/org-perm/positions/{positionId}/roles/{roleId}` |
| 人员直授 | 维护人员直授角色 | `GET /api/org-perm/persons/{personId}/roles/direct`、`POST /api/org-perm/persons/{personId}/roles/direct`、`POST /api/org-perm/persons/{personId}/roles/{roleId}/revoke` |
| 资源权限配置 | 查询和替换角色资源权限 | `GET /api/org-perm/roles/{roleId}/resource-permissions`、`PUT /api/org-perm/roles/{roleId}/resource-permissions` |
| 模板管理 | 维护角色模板和模板应用 | `GET /api/org-perm/role-templates`、`POST /api/org-perm/role-templates`、`POST /api/org-perm/role-templates/{templateId}/apply` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/roles` | 角色分页查询 | `keyword`、`category`、`scope`、`status`、`pageNo`、`pageSize` | 返回角色基础信息、绑定岗位数、直授人数摘要 |
| `GET` | `/api/org-perm/roles/{roleId}` | 角色详情 | 路径参数 `roleId` | 返回角色基础字段、资源权限摘要、绑定范围摘要 |
| `GET` | `/api/org-perm/positions/{positionId}/roles` | 查询岗位已绑定角色 | `includeDisabled` 可选 | 返回岗位继承角色列表及来源说明 |
| `GET` | `/api/org-perm/persons/{personId}/roles/direct` | 查询人员直授角色 | `includeExpired` 可选 | 返回直授角色、原因、有效期和状态 |
| `GET` | `/api/org-perm/roles/{roleId}/resource-permissions` | 查询角色资源权限明细 | `resourceType` 可选 | 返回资源类型、资源编码、动作、效果 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/roles` | 新增角色 | `code`、`name`、`category`、`scope`、`description` | 角色编码租户内唯一，作用域必须符合配置范围 |
| `PUT` | `/api/org-perm/roles/{roleId}` | 编辑角色 | 同创建字段 + `version` | 不允许通过编辑接口静默改变历史授权轨迹 |
| `POST` | `/api/org-perm/roles/{roleId}/status` | 启停用角色 | `status`、`reason`、`version` | 停用后需触发权限缓存重算 |
| `POST` | `/api/org-perm/positions/{positionId}/roles` | 绑定岗位角色 | `roleIds[]`、`reason` | 绑定岗位必须有效；重复绑定需幂等返回 |
| `DELETE` | `/api/org-perm/positions/{positionId}/roles/{roleId}` | 解绑岗位角色 | 路径参数 + `reason`、`version` | 解绑后不删除历史审计，仅失效继承关系 |
| `POST` | `/api/org-perm/persons/{personId}/roles/direct` | 新增人员直授角色 | `roleId`、`reason`、`expiresAt` | `reason` 必填；直授只作为例外机制 |
| `POST` | `/api/org-perm/persons/{personId}/roles/{roleId}/revoke` | 撤销人员直授角色 | `reason`、`version` | 撤销动作必须保留原因和操作者 |
| `PUT` | `/api/org-perm/roles/{roleId}/resource-permissions` | 替换角色资源权限 | `permissions[]`、`reason`、`version` | 一次提交应视为该角色资源权限的完整快照写入 |
| `POST` | `/api/org-perm/role-templates/{templateId}/apply` | 应用角色模板 | `targetRoleId` 或 `createRole=true`、`reason` | 模板应用后复制成新的角色与权限快照，不建立动态关联 |

## 通用约束

### 筛选与分页

- 角色列表支持按角色类别、作用域、状态和关键字组合筛选。
- 资源权限查询支持按 `resourceType` 分组，前端应按树或矩阵模式展示。
- 分页统一使用 `pageNo/pageSize`，`pageSize` 一期建议上限 `100`。

### 批量操作

- 岗位绑定角色接口可一次提交多个 `roleId`，但必须返回逐条成功/失败结果。
- 资源权限配置建议采用“整角色全量替换”模式，避免局部打补丁造成遗漏。
- 模板应用属于批量复制操作，需返回角色、资源权限和继承影响摘要。

### 幂等要求

- 角色创建、岗位绑定、人员直授、模板应用和资源权限替换都应支持 `X-Idempotency-Key`。
- 所有编辑和配置替换操作必须使用 `version` 做乐观锁控制。
- 对重复岗位绑定、重复人员直授请求，幂等重复提交应返回同一授权结果，不新增重复关系。

### 权限与审计约束

- 页面和接口权限至少区分 `role.read`、`role.write`、`role.disable`、`role.bind-position`、`role.grant-person`、`resource-permission.write`。
- 人员直授和资源权限替换必须记录操作原因，原因是审计必填项。
- 停用角色、批量绑定岗位、模板应用和整量替换资源权限都必须进入统一审计。
- 资源权限接口只输出资源动作结果，不负责返回数据范围或字段可见性。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 角色编码重复 | `409` | `ROLE_CODE_DUPLICATED` | 同租户角色编码唯一 |
| 角色作用域与绑定目标不匹配 | `400` | `ROLE_SCOPE_MISMATCH` | 如部门级角色绑定到不合法范围 |
| 岗位角色重复绑定 | `409` | `POSITION_ROLE_DUPLICATED` | 同岗位同角色不能重复 |
| 人员直授缺失原因 | `400` | `PERSON_ROLE_REASON_REQUIRED` | 例外授权必须留痕 |
| 直授角色已过期或无效 | `409` | `PERSON_ROLE_EXPIRED` | 不允许重复激活已失效关系 |
| 资源权限存在重复资源动作 | `409` | `RESOURCE_PERMISSION_DUPLICATED` | 同角色同资源动作不能出现多条重复记录 |
| 试图在本模块配置数据权限 | `403` | `DATA_PERMISSION_OUT_OF_SCOPE` | 行级/字段级权限归属 `data-permission` |
