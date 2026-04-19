# ecosystem API 契约

## 1. 范围与边界

本文件定义 `ecosystem` 的接入配置、连接测试、回调入口、健康监控和审计查询接口。

- 本模块负责：企业微信、钉钉、短信、邮件、SSO、LDAP 等生态接入的治理接口。
- 本模块不负责：通用 Webhook 平台、开放 API、消息模板/路由/投递任务、移动会话。
- 消息发送由 `channel-sender` 发起；本模块只提供配置、回调和治理能力。

## 2. 通用约束

### 2.1 鉴权

- 配置管理、连接测试、健康监控和审计查询要求租户管理员或安全管理员权限。
- 回调入口为开放地址，但必须通过签名、时间戳和租户线索校验。

### 2.2 分页与筛选

- 配置列表、回调审计列表统一使用 `pageNo`、`pageSize`，默认 `20`，最大 `100`。
- 审计日志默认按 `occurredAt desc` 排序。

### 2.3 幂等与审计

- 配置创建、更新、连接测试需要 `Idempotency-Key`。
- 回调幂等键优先使用 `providerEventId + integrationId`；无该字段时退化为 `providerMessageId + callbackType`。
- 所有配置变更、测试发送、回调验签失败都必须写审计。

## 3. 接口分组

| 分组 | 说明 | 主要调用方 |
|------|------|-----------|
| 接入配置管理 | 集成列表、详情、创建、启停 | 管理端、安全端 |
| 连接测试与健康 | 手工测试、查看健康状态 | 管理端、运维端 |
| 回调入口 | 第三方平台回调 | 外部生态平台 |
| 回调审计 | 查看校验结果、失败原因和原始摘要 | 管理端、审计端 |

## 4. 接口定义

### 4.1 GET `/admin/ecosystem/integrations`

接入配置列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `pageNo` / `pageSize` | 否 | 分页参数 |
| `integrationType` | 否 | `WECHAT_WORK / DINGTALK / EMAIL / SMS / SSO / LDAP` |
| `status` | 否 | `DRAFT / ENABLED / DISABLED / ERROR` |
| `healthStatus` | 否 | `UNKNOWN / HEALTHY / DEGRADED / UNREACHABLE` |

返回关键字段：

- `integrationId`
- `integrationType`
- `displayName`
- `authMode`
- `callbackUrl`
- `configRef`
- `status`
- `healthStatus`
- `lastCheckAt`
- `lastErrorSummary`

### 4.2 POST `/admin/ecosystem/integrations`

创建或更新接入配置。

请求体关键字段：

- `integrationType`
- `displayName`
- `authMode`
- `callbackUrl`
- `signAlgorithm`
- `configRef`
- `status`

规则：

- 只允许保存配置引用，不回显或落库存储明文密钥。
- `SSO / LDAP` 与消息渠道接入共用统一配置模型，但业务用途不同。

### 4.3 POST `/admin/ecosystem/integrations/{integrationId}/test-connection`

连接测试。

请求体关键字段：

- `testTarget`
- `testPayload`

规则：

- 仅验证连接、认证或最小可用调用。
- 测试结果必须回写 `healthStatus`、`lastCheckAt`、`lastErrorSummary`。

### 4.4 GET `/admin/ecosystem/health`

健康状态总览。

返回关键字段：

- `integrationId`
- `integrationType`
- `status`
- `healthStatus`
- `lastCheckAt`
- `lastErrorSummary`

### 4.5 POST `/open/ecosystem/callbacks/{integrationType}`

第三方回调入口。

路径参数：

| 参数 | 说明 |
|------|------|
| `integrationType` | 接入类型，用于路由到具体校验逻辑 |

请求头关键字段：

- 签名
- 时间戳
- 请求 ID
- 租户线索

规则：

- 先验签、再租户隔离、再做幂等校验。
- 验签失败直接拒绝，不进入业务处理。
- 回调处理结果必须写 `CallbackAuditRecord`。

### 4.6 GET `/admin/ecosystem/callback-audits`

回调审计列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `integrationId` | 否 | 接入配置 ID |
| `callbackType` | 否 | 回调类型 |
| `verifyResult` | 否 | `PASSED / FAILED` |
| `occurredFrom` / `occurredTo` | 否 | 时间范围 |

返回关键字段：

- `auditId`
- `integrationId`
- `callbackType`
- `verifyResult`
- `payloadSummary`
- `errorMessage`
- `occurredAt`

## 5. 权限与审计约束

| 场景 | 权限要求 | 审计要求 |
|------|----------|----------|
| 接入配置创建、更新、启停 | 管理员或安全管理员 | 必须记录前后差异和操作者 |
| 连接测试 | 管理员、运维 | 记录测试目标、结果、时间 |
| 回调入口 | 对外开放但需验签 | 记录验签结果、租户、摘要 |
| 回调审计查询 | 管理员、审计 | 记录访问日志 |

## 6. 错误场景

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 集成类型不支持 | `422` | 超出父模块定义范围 |
| 缺少 `configRef` 或引用无效 | `422 / 409` | 配置不完整或不可用 |
| 验签失败、时间戳过期 | `401` | 回调请求被拒绝 |
| 回调重复投递 | `200` | 幂等接受，不重复处理 |
| 接入已禁用仍发起测试或回调处理 | `409` | 返回当前状态 |
| 尝试保存明文敏感信息 | `422` | 明确拒绝 |
