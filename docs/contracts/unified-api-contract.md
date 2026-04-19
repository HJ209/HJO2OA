# 统一接口契约

## 1. 文档目的

本文档定义 HJO2OA 平台所有后端 API 的统一契约规范，包括请求头、响应体、分页、过滤、排序、错误码、幂等和版本策略。所有业务模块的 API 设计必须遵循本契约，前后端协作以本契约为准。

对应架构决策编号：D11（建立统一 API 契约）。

## 2. 总体原则

- 所有 API 采用 RESTful 风格，资源路径以名词复数形式命名。
- 所有 API 必须经过网关，由网关统一处理认证、限流和审计。
- 所有 API 必须支持多租户隔离，租户 ID 通过请求头或 Token 传递，不作为路径参数。
- 所有 API 必须支持国际化，响应中的用户可见文案必须走 i18n 资源包。
- 所有写操作必须支持幂等。
- 所有 API 必须有版本策略。

## 3. 请求规范

### 3.1 通用请求头

| 请求头 | 必填 | 说明 |
|--------|------|------|
| `Authorization` | 是 | Bearer Token，格式 `Bearer {jwt}` |
| `X-Tenant-Id` | 是 | 租户 ID（网关也可从 Token 中提取） |
| `X-Request-Id` | 是 | 请求唯一标识，UUID 格式，用于链路追踪和幂等校验 |
| `X-Idempotency-Key` | 写操作必填 | 幂等键，客户端生成的唯一标识，同一键的重复请求返回相同结果 |
| `Accept-Language` | 否 | 期望的语言，如 `zh-CN`、`en-US`，默认 `zh-CN` |
| `X-Timezone` | 否 | 客户端时区，如 `Asia/Shanghai`，默认 `UTC` |
| `X-Identity-Assignment-Id` | 否 | 当前身份任职关系 ID，用于唯一标识当前身份上下文 |
| `X-Identity-Position-Id` | 否 | 当前身份岗位 ID（身份切换后传递），默认主岗 |
| `If-None-Match` | 否 | ETag 匹配，用于条件查询 |

### 3.2 请求路径规范

```
/api/{version}/{module-prefix}/{resource}
```

| 部分 | 说明 | 示例 |
|------|------|------|
| version | API 版本 | `v1` |
| module-prefix | 模块前缀 | `org`, `process`, `form`, `content`, `portal`, `msg`, `data` |
| resource | 资源名称，名词复数 | `organizations`, `departments`, `positions` |

**路径示例**：

- `GET /api/v1/org/organizations` — 查询组织列表
- `POST /api/v1/org/organizations` — 创建组织
- `GET /api/v1/org/organizations/{id}` — 查询组织详情
- `PUT /api/v1/org/organizations/{id}` — 更新组织
- `DELETE /api/v1/org/organizations/{id}` — 删除组织
- `POST /api/v1/org/organizations/{id}/disable` — 停用组织（非 CRUD 动作）

### 3.3 模块前缀约定

| 模块 | 前缀 | 说明 |
|------|------|------|
| 00-平台基础设施 | `infra` | 事件总线、i18n、附件、字典、配置、审计等 |
| 01-组织与权限 | `org` | 组织、部门、岗位、人员、账号、角色、权限 |
| 02-流程与表单 | `process` | 流程定义、流程实例、任务、动作 |
| 02-流程与表单 | `form` | 表单元数据 |
| 02-流程与表单 | `todo` | 待办中心 |
| 03-内容与知识 | `content` | 栏目、内容、搜索 |
| 04-门户与工作台 | `portal` | 门户、页面、工作台、聚合接口 |
| 05-协同办公应用 | `biz` | 公文、会议、日程、考勤、合同、资产、公告 |
| 06-消息移动与生态 | `msg` | 消息、渠道、待办提醒、移动端、生态 |
| 07-数据服务与集成 | `data` | 数据服务、开放接口、连接器、同步、报表、治理 |

### 3.4 请求体规范

- `Content-Type: application/json`
- 日期时间字段使用 ISO 8601 格式：`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`（UTC）
- 日期字段使用 `yyyy-MM-dd` 格式
- 布尔字段使用 `true` / `false`
- 空值使用 `null`，不省略必填字段
- 枚举值使用大写蛇形命名：`ACTIVE`, `DISABLED`, `PRIMARY`

## 4. 响应规范

### 4.1 统一响应体

#### 成功响应

```json
{
  "code": "OK",
  "message": "操作成功",
  "data": { ... },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-18T01:30:00.000Z",
    "serverTimezone": "UTC"
  }
}
```

#### 列表响应（含分页）

```json
{
  "code": "OK",
  "message": "查询成功",
  "data": {
    "items": [ ... ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 156,
      "totalPages": 8
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-18T01:30:00.000Z"
  }
}
```

#### 错误响应

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "errors": [
    {
      "field": "name",
      "message": "名称不能为空",
      "rejectedValue": null
    }
  ],
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-18T01:30:00.000Z"
  }
}
```

### 4.2 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 业务状态码，见 §5 错误码 |
| message | String | 用户可见的本地化消息 |
| data | Object / Array / null | 业务数据 |
| data.items | Array | 列表数据 |
| data.pagination | Object | 分页信息 |
| errors | Array | 校验错误详情（仅 VALIDATION_ERROR 时） |
| meta.requestId | String | 请求 ID，与请求头 X-Request-Id 对应 |
| meta.timestamp | String | 服务器响应时间（UTC） |
| meta.serverTimezone | String | 服务器时区 |

### 4.3 HTTP 状态码使用

| HTTP 状态码 | 含义 | 使用场景 |
|-------------|------|----------|
| 200 | 成功 | GET / PUT / PATCH 成功 |
| 201 | 已创建 | POST 创建资源成功 |
| 204 | 无内容 | DELETE 成功 |
| 304 | 未修改 | ETag 匹配，缓存有效 |
| 400 | 请求错误 | 参数校验失败、请求格式错误 |
| 401 | 未认证 | Token 缺失或过期 |
| 403 | 无权限 | 权限不足 |
| 404 | 未找到 | 资源不存在 |
| 409 | 冲突 | 幂等重复请求、版本冲突、唯一约束冲突 |
| 422 | 不可处理 | 业务规则校验失败 |
| 429 | 请求过多 | 限流 |
| 500 | 服务器错误 | 未捕获异常 |

## 5. 错误码规范

### 5.1 错误码格式

错误码采用 `{类别}_{具体错误}` 的大写蛇形格式。

### 5.2 通用错误码

| 错误码 | HTTP 状态码 | 说明 |
|--------|------------|------|
| OK | 200 | 成功 |
| VALIDATION_ERROR | 400 | 请求参数校验失败 |
| BAD_REQUEST | 400 | 请求格式错误 |
| UNAUTHORIZED | 401 | 未认证 |
| TOKEN_EXPIRED | 401 | Token 过期 |
| FORBIDDEN | 403 | 无权限 |
| RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| CONFLICT | 409 | 资源冲突 |
| IDEMPOTENT_DUPLICATE | 409 | 幂等重复请求（返回首次结果） |
| VERSION_CONFLICT | 409 | 乐观锁版本冲突 |
| BUSINESS_RULE_VIOLATION | 422 | 业务规则校验失败 |
| RATE_LIMITED | 429 | 限流 |
| INTERNAL_ERROR | 500 | 服务器内部错误 |
| SERVICE_UNAVAILABLE | 503 | 服务不可用 |

### 5.3 模块错误码前缀

| 模块 | 前缀 | 示例 |
|------|------|------|
| 00-平台基础设施 | `INFRA_` | `INFRA_ATTACHMENT_NOT_FOUND`, `INFRA_DICT_ITEM_DUPLICATE` |
| 01-组织与权限 | `ORG_` | `ORG_PRIMARY_POSITION_REQUIRED`, `ORG_ROLE_ALREADY_GRANTED` |
| 02-流程与表单 | `PROCESS_` / `FORM_` / `TODO_` | `PROCESS_DEFINITION_NOT_PUBLISHED`, `FORM_FIELD_TYPE_INVALID` |
| 03-内容与知识 | `CONTENT_` | `CONTENT_CATEGORY_NOT_FOUND`, `CONTENT_PERMISSION_DENIED` |
| 04-门户与工作台 | `PORTAL_` | `PORTAL_TEMPLATE_NOT_FOUND` |
| 05-协同办公应用 | `BIZ_` | `BIZ_MEETING_CONFLICT`, `BIZ_CONTRACT_EXPIRED` |
| 06-消息移动与生态 | `MSG_` | `MSG_CHANNEL_UNAVAILABLE`, `MSG_SEND_FAILED` |
| 07-数据服务与集成 | `DATA_` | `DATA_SYNC_FAILED`, `DATA_CONNECTOR_ERROR` |

### 5.4 错误码设计原则

- 错误码面向开发者，不面向终端用户；用户可见消息走 i18n 资源包。
- 错误码必须唯一，不可复用。
- 错误码不可删除，废弃后标记为 deprecated。
- 每个错误码必须有对应的 i18n 消息模板。

## 6. 分页规范

### 6.1 请求参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | Integer | 1 | 页码，从 1 开始 |
| size | Integer | 20 | 每页大小，最大 100 |
| sort | String | | 排序字段，格式 `{field},{direction}`，多字段用 `;` 分隔 |
| fields | String | | 返回字段过滤，逗号分隔，如 `id,name,code` |

**请求示例**：

```
GET /api/v1/org/organizations?page=2&size=50&sort=name,asc;code,desc&fields=id,name,code
```

### 6.2 响应分页结构

```json
{
  "pagination": {
    "page": 2,
    "size": 50,
    "total": 156,
    "totalPages": 4
  }
}
```

### 6.3 分页约束

- `page` 最小值为 1。
- `size` 范围 1–100，超出自动截断为 100。
- 大数据量导出场景不使用分页，走异步导出接口。

## 7. 过滤规范

### 7.1 过滤参数格式

过滤参数以 `filter[{field}]{operator}={value}` 格式传递。

| 运算符 | 格式 | 说明 | 示例 |
|--------|------|------|------|
| 等于 | `filter[{field}]` | 精确匹配 | `filter[status]=ACTIVE` |
| 不等于 | `filter[{field}]ne` | 不匹配 | `filter[status]ne=DISABLED` |
| 包含 | `filter[{field}]like` | 模糊匹配 | `filter[name]like=技术` |
| 起始于 | `filter[{field}]start` | 前缀匹配 | `filter[code]start=ORG` |
| 大于 | `filter[{field}]gt` | 数值/日期大于 | `filter[createdAt]gt=2026-01-01` |
| 大于等于 | `filter[{field}]gte` | 数值/日期大于等于 | `filter[level]gte=3` |
| 小于 | `filter[{field}]lt` | 数值/日期小于 | `filter[dueTime]lt=2026-12-31` |
| 小于等于 | `filter[{field}]lte` | 数值/日期小于等于 | |
| 在范围内 | `filter[{field}]in` | 多值匹配，逗号分隔 | `filter[type]in=PRIMARY,SECONDARY` |
| 空值 | `filter[{field}]null` | 为空 | `filter[parentId]null=true` |

### 7.2 过滤示例

```
GET /api/v1/org/departments?filter[organizationId]=org-001&filter[status]=ACTIVE&filter[name]like=技术&page=1&size=20
```

### 7.3 过滤约束

- 过滤字段必须是实体上的可查询字段。
- 不支持跨实体关联过滤（如通过部门名称过滤人员），复杂查询走专用查询接口。
- 日期过滤值使用 ISO 8601 格式。
- `like` 运算符服务端实现为 `%value%` 模糊匹配。

## 8. 排序规范

### 8.1 排序参数

排序通过 `sort` 查询参数传递，格式 `{field},{direction}`，多字段用 `;` 分隔。

| 方向 | 值 | 说明 |
|------|-----|------|
| 升序 | asc | 默认方向 |
| 降序 | desc | |

**示例**：

```
GET /api/v1/org/organizations?sort=name,asc;createdAt,desc
```

### 8.2 排序约束

- 排序字段必须是实体上的可排序字段。
- 不支持跨实体关联排序。
- 无排序参数时，默认按 `sortOrder,asc;createdAt,desc` 排序。

## 9. 幂等规范

### 9.1 幂等键

除 9.3 节列出的天然幂等操作外，所有写操作（POST / PUT / PATCH / DELETE）**必须**携带 `X-Idempotency-Key` 请求头。

- 幂等键由客户端生成，建议使用 UUID。
- 同一幂等键的重复请求，服务端返回首次请求的结果（HTTP 409 + 首次响应体）。
- 幂等键有效期 24 小时，过期后可重用。
- 天然幂等操作不强制要求幂等键，但客户端仍可携带，服务端照常处理。

### 9.2 幂等实现

- 服务端维护幂等键与请求结果的映射表。
- 写操作先检查幂等键是否存在，存在则直接返回缓存结果。
- 幂等键映射表定期清理过期记录。

### 9.3 天然幂等操作

以下操作天然幂等，**不强制**要求 `X-Idempotency-Key`：

- 基于唯一业务键的创建（如 `code` 唯一的资源创建，重复提交返回 CONFLICT）。
- 基于乐观锁版本的更新（版本冲突返回 VERSION_CONFLICT）。
- 删除操作（重复删除返回 RESOURCE_NOT_FOUND）。

> **注意**：天然幂等操作豁免幂等键的前提是服务端已通过业务键或乐观锁实现了幂等保障。若接口文档未明确标注天然幂等，客户端仍应携带幂等键。

## 10. 版本策略

### 10.1 API 版本

- API 版本通过 URL 路径传递：`/api/v1/...`、`/api/v2/...`。
- 主版本号变更表示不兼容变更（如删除字段、变更语义）。
- 次要变更（新增可选字段、新增枚举值）不升级版本号，保持向后兼容。

### 10.2 版本生命周期

| 阶段 | 说明 |
|------|------|
| Current | 当前活跃版本，全量支持 |
| Deprecated | 已废弃版本，仍可用但响应头包含 `Deprecation: true` 和 `Sunset: {date}` |
| Retired | 已下线版本，请求返回 410 Gone |

- Deprecated 版本至少保留 6 个月。
- 废弃通知通过响应头和 API 文档同步发布。

### 10.3 兼容性规则

**允许的兼容变更**：

- 新增可选请求字段
- 新增响应字段
- 新增枚举值（客户端必须容忍未知枚举值）
- 新增 API 端点
- 新增可选查询参数

**不允许的不兼容变更**（需升级版本号）：

- 删除或重命名请求/响应字段
- 变更字段类型
- 变更字段语义
- 变更 API 路径
- 删除 API 端点
- 变更错误码含义

## 11. 认证与授权

### 11.1 认证方式

- 所有 API 使用 JWT Bearer Token 认证。
- Token 由认证服务签发，包含 `sub`(accountId)、`tid`(tenantId)、`pid`(personId)、`posId`(currentPositionId)、`orgId`(currentOrganizationId) 等声明。
- Token 有效期 2 小时，Refresh Token 有效期 7 天。

### 11.2 授权判定

- 网关负责 Token 校验和基础路由。
- 业务服务负责资源权限和数据权限判定。
- 授权判定基于 `01-组织与权限` 输出的身份上下文。

### 11.3 跨模块调用

- 跨模块**业务联动**（写操作、状态变更）必须通过事件总线异步完成，禁止同步调用。
- 跨模块**只读查询**允许通过内部服务接口同步调用，不走网关，但必须满足：仅限查询、不触发写操作、调用方不依赖被调用方的内部模型。
- 模块间调用必须传递 `X-Request-Id` 和 `X-Tenant-Id`。
- 模块间调用使用服务级 Token，非用户 Token。

## 12. 文件上传与下载

### 12.1 上传

- 小文件（< 10MB）：直接 `POST /api/v1/infra/attachments`，`Content-Type: multipart/form-data`。
- 大文件（>= 10MB）：分片上传，先 `POST /api/v1/infra/attachments/init-upload` 获取上传 ID，再逐片上传，最后 `POST /api/v1/infra/attachments/complete-upload` 合并。

### 12.2 下载

- `GET /api/v1/infra/attachments/{id}/download` 返回文件流。
- 支持 `Range` 请求头实现断点续传。

### 12.3 预览

- `GET /api/v1/infra/attachments/{id}/preview` 返回预览信息（URL 或转换后格式）。

## 13. 异步操作

### 13.1 长时间操作

对于耗时较长的操作（批量导入、批量导出、数据同步），采用异步模式：

1. 客户端发起请求，服务端返回 `202 Accepted` + `operationId`。
2. 客户端轮询 `GET /api/v1/infra/operations/{operationId}` 获取进度。
3. 操作完成后返回最终结果。

**响应示例**：

```json
{
  "code": "ACCEPTED",
  "message": "操作已接受，正在处理",
  "data": {
    "operationId": "op-550e8400",
    "status": "PROCESSING",
    "progress": 45,
    "estimatedCompletionTime": "2026-04-18T02:00:00.000Z"
  }
}
```

### 13.2 批量导出

- 导出请求走异步模式，完成后生成附件。
- 客户端通过操作进度接口获取附件下载链接。

## 14. 缓存与条件请求

### 14.1 ETag

- GET 请求响应包含 `ETag` 响应头，值为资源哈希。
- 后续请求携带 `If-None-Match: {etag}`，资源未变更时返回 `304 Not Modified`。

### 14.2 Last-Modified

- 响应包含 `Last-Modified` 响应头。
- 请求携带 `If-Modified-Since`，资源未修改时返回 `304`。

## 15. 限流

- 限流由网关统一执行，基于租户 + API 维度。
- 默认限流：读操作 100 次/秒，写操作 20 次/秒。
- 超出限流返回 `429 Too Many Requests` + `Retry-After` 响应头。
- 特殊接口可配置独立限流策略。

## 16. 审计日志

- 所有写操作必须记录审计日志。
- 审计日志包含：requestId、操作人、操作时间、操作类型、资源类型、资源 ID、变更前后值。
- 审计日志由 `00-审计日志` 统一采集和存储。
- 审计日志不可修改和删除。

## 17. 接口文档

- 所有 API 必须提供 OpenAPI 3.0 规范文档。
- 接口文档与代码同步维护，变更接口必须同步更新文档。
- 接口文档通过 Swagger UI 或 Redoc 提供在线浏览。
- 接口文档必须标注版本、废弃状态和兼容性说明。
