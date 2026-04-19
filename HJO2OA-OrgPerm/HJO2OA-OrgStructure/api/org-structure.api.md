# org-structure API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 组织查询 | 输出组织树和组织详情 | `GET /api/org-perm/organizations/tree`、`GET /api/org-perm/organizations/{organizationId}` |
| 组织写入 | 创建、编辑、移动、启停用组织 | `POST /api/org-perm/organizations`、`PUT /api/org-perm/organizations/{organizationId}`、`POST /api/org-perm/organizations/{organizationId}/move`、`POST /api/org-perm/organizations/{organizationId}/status` |
| 部门查询 | 输出部门树、部门分页列表和详情 | `GET /api/org-perm/departments/tree`、`GET /api/org-perm/departments`、`GET /api/org-perm/departments/{departmentId}` |
| 部门写入 | 创建、编辑、移动、启停用部门 | `POST /api/org-perm/departments`、`PUT /api/org-perm/departments/{departmentId}`、`POST /api/org-perm/departments/{departmentId}/move`、`POST /api/org-perm/departments/{departmentId}/status` |
| 导入与预校验 | 提供模板、预校验、确认导入 | `GET /api/org-perm/organization-import/template`、`POST /api/org-perm/organization-import/preview`、`POST /api/org-perm/organization-import/{jobId}/confirm` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/organizations/tree` | 查询组织树 | `keyword`、`status`、`rootOrganizationId`、`includeDisabled` | 树节点需返回 `id/code/name/type/parentId/path/level/sortOrder/status/hasChildren` |
| `GET` | `/api/org-perm/organizations/{organizationId}` | 查询组织详情 | 路径参数 `organizationId` | 返回组织基础信息、上下级摘要、下属部门数、下属岗位/人员摘要 |
| `GET` | `/api/org-perm/departments/tree` | 按组织查询部门树 | `organizationId` 必填；`keyword`、`status`、`parentDepartmentId` 可选 | 返回部门树节点及负责人摘要 |
| `GET` | `/api/org-perm/departments` | 部门分页查询 | `organizationId`、`keyword`、`status`、`managerId`、`pageNo`、`pageSize` | 返回平铺分页结果，便于表格筛选和导出 |
| `GET` | `/api/org-perm/departments/{departmentId}` | 查询部门详情 | 路径参数 `departmentId` | 返回部门基础信息、上级路径、负责人摘要、绑定岗位/人员数量摘要 |
| `POST` | `/api/org-perm/organizations/{organizationId}/move-preview` | 组织移动影响预览 | `targetParentId`、`targetSortOrder` | 返回新旧路径、后代数量、影响摘要，不落库 |
| `POST` | `/api/org-perm/departments/{departmentId}/move-preview` | 部门移动影响预览 | `targetOrganizationId`、`targetParentId`、`targetSortOrder` | 返回跨组织移动合法性与影响摘要 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/organizations` | 新增组织 | `code`、`name`、`type`、`parentId`、`sortOrder`、`shortName` | 上级组织必须存在且未停用；同租户编码唯一 |
| `PUT` | `/api/org-perm/organizations/{organizationId}` | 编辑组织 | 同创建字段 + `version` | 不允许借由编辑直接变更层级，层级变化必须走移动接口 |
| `POST` | `/api/org-perm/organizations/{organizationId}/move` | 调整组织层级 | `targetParentId`、`targetSortOrder`、`version` | 必须先通过预览；禁止形成环；需级联更新后代路径 |
| `POST` | `/api/org-perm/organizations/{organizationId}/status` | 启停用组织 | `status`、`reason`、`version` | 停用需返回影响摘要；原因建议审计留痕 |
| `POST` | `/api/org-perm/departments` | 新增部门 | `code`、`name`、`organizationId`、`parentId`、`managerId`、`sortOrder` | 上级部门与所属组织必须一致；负责人必须属于同组织 |
| `PUT` | `/api/org-perm/departments/{departmentId}` | 编辑部门 | 同创建字段 + `version` | 仅允许更新基础字段和负责人，不允许绕过移动接口改层级 |
| `POST` | `/api/org-perm/departments/{departmentId}/move` | 调整部门层级 | `targetOrganizationId`、`targetParentId`、`targetSortOrder`、`version` | 跨组织移动时需校验目标组织有效且同步重算后代路径 |
| `POST` | `/api/org-perm/departments/{departmentId}/status` | 启停用部门 | `status`、`reason`、`version` | 已停用父部门下不得新增子部门 |
| `POST` | `/api/org-perm/organization-import/preview` | 导入预校验 | 文件、`importType`、`conflictStrategy=REJECT_ONLY` | 一期只允许校验后确认，不允许直接覆盖 |
| `POST` | `/api/org-perm/organization-import/{jobId}/confirm` | 确认导入 | `jobId`、`version` | 只接受最近一次成功预校验结果，超时需重新校验 |

## 通用约束

### 筛选与分页

- 树查询默认返回当前权限范围内全量节点，不做分页，但必须支持 `keyword`、`status` 和根节点裁剪。
- 列表查询统一使用 `pageNo`、`pageSize`，其中 `pageSize` 一期建议上限 `100`。
- 组织和部门详情返回的绑定数量摘要只做展示和预警，不替代下游模块的真相查询。

### 批量操作

- 一期不提供通用批量删除接口，批量治理只开放导入和状态变更。
- 导入预校验结果必须给出逐行错误码、错误原因和建议修复项。
- 批量状态切换若后续补充，单次请求建议不超过 `200` 个节点，并返回逐条结果。

### 幂等要求

- 所有创建、移动、状态切换、导入确认接口都应支持 `X-Idempotency-Key`。
- 幂等键建议以“租户 + 操作人 + 业务动作 + 客户端请求号”为唯一维度，服务端至少保留 `24h`。
- 组织/部门编辑与移动接口必须使用乐观锁字段 `version`，避免并发覆盖树结构。

### 权限与审计约束

- 查询接口至少受资源权限 `organization.read`、`department.read` 控制，写接口受 `write/move/disable/import` 动作控制。
- 所有写接口必须记录 `operatorId`、`operatorName`、`tenantId`、`reason`、变更前后快照和来源渠道。
- 层级移动、停用和导入确认属于高风险操作，必须进入统一审计日志。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 组织或部门编码重复 | `409` | `ORG_CODE_DUPLICATED` / `DEPT_CODE_DUPLICATED` | 同租户内编码冲突，必须提示冲突节点 |
| 调整层级形成环 | `400` | `ORG_HIERARCHY_CYCLE` / `DEPT_HIERARCHY_CYCLE` | 目标父节点是自身或自身后代 |
| 目标父节点已停用 | `409` | `ORG_PARENT_DISABLED` / `DEPT_PARENT_DISABLED` | 禁止挂接到停用节点下 |
| 部门跨组织挂接非法 | `400` | `DEPT_PARENT_ORG_MISMATCH` | 上级部门与目标组织不一致 |
| 部门负责人不属于同组织 | `400` | `DEPT_MANAGER_ORG_MISMATCH` | 负责人选择越界 |
| 导入确认基于过期预校验结果 | `409` | `IMPORT_PREVIEW_EXPIRED` | 需重新预校验后再确认 |
| 并发修改导致版本冲突 | `409` | `ORG_VERSION_CONFLICT` / `DEPT_VERSION_CONFLICT` | 前端需刷新最新数据后重试 |
