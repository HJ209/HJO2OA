# security API

## 接口分组

| 分组 | 代表接口 | 说明 |
|------|----------|------|
| 安全策略管理 | `GET /api/v1/infra/security/policies`、`POST /api/v1/infra/security/policies`、`PUT /api/v1/infra/security/policies/{policyId}` | 管理策略编码、类型、作用域和配置快照。 |
| 策略发布与回滚 | `POST /api/v1/infra/security/policies/{policyId}/publish`、`POST /api/v1/infra/security/policies/{policyId}/rollback` | 控制策略生效、恢复历史版本。 |
| 密钥引用治理 | `POST /api/v1/infra/security/keys/{keyRef}/rotate` | 对密钥引用执行轮换治理。 |
| 安全服务接口 | `POST /api/v1/infra/security/masking/preview`、`POST /api/v1/infra/security/signatures/sign`、`POST /api/v1/infra/security/signatures/verify` | 为前后端、开放接口提供脱敏预览和签名能力。 |
| 异常与审计 | `GET /api/v1/infra/security/anomalies`、`POST /api/v1/infra/security/anomalies/{anomalyId}/resolve`、`GET /api/v1/infra/security/sensitive-operations` | 查询异常、处置风险、检索敏感操作。 |

## 关键查询/写入接口

| 方法 | 路径 | 用途 | 幂等/并发约束 | 权限与审计 |
|------|------|------|---------------|------------|
| `GET` | `/api/v1/infra/security/policies` | 查询安全策略列表 | 只读接口，无需幂等键 | 需要 `infra.security.read`，默认按作用域过滤。 |
| `POST` | `/api/v1/infra/security/policies` | 创建安全策略 | 必须传 `X-Idempotency-Key`；`policyCode` 全局唯一 | 需要 `infra.security.manage`，记录策略初始快照。 |
| `PUT` | `/api/v1/infra/security/policies/{policyId}` | 更新策略配置 | 必须传 `expectedVersion`，避免并发覆盖 | 需要 `infra.security.manage`，审计差异字段。 |
| `POST` | `/api/v1/infra/security/policies/{policyId}/publish` | 发布或重新生效策略版本 | 自然幂等；重复发布返回当前版本信息 | 需要 `infra.security.operate`，要求 `confirmToken` 与 `operationReason`。 |
| `POST` | `/api/v1/infra/security/policies/{policyId}/rollback` | 回滚到上一个策略快照 | 必须传 `X-Idempotency-Key`；仅允许回滚到可兼容历史版本 | 需要 `infra.security.operate`，必须二次确认并写审计。 |
| `POST` | `/api/v1/infra/security/keys/{keyRef}/rotate` | 轮换密钥引用版本 | 必须传 `X-Idempotency-Key`；同一 `keyRef` 同时只允许一个轮换任务 | 需要 `infra.security.secret.rotate`，审计轮换前后版本。 |
| `POST` | `/api/v1/infra/security/masking/preview` | 预览脱敏规则效果 | 只读语义，无需幂等键 | 需要 `infra.security.read`，请求体中的样例数据必须即时脱敏。 |
| `POST` | `/api/v1/infra/security/signatures/sign` | 生成签名 | 业务无状态，可不要求幂等键 | 需要 `infra.security.use`，调用本身视为敏感操作并记录审计。 |
| `POST` | `/api/v1/infra/security/signatures/verify` | 验证签名 | 业务无状态，可不要求幂等键 | 需要 `infra.security.use`，失败结果写安全审计。 |
| `GET` | `/api/v1/infra/security/anomalies` | 查询异常检测结果 | 只读接口，无需幂等键 | 需要 `infra.security.read`，支持按风险等级、主体类型过滤。 |
| `POST` | `/api/v1/infra/security/anomalies/{anomalyId}/resolve` | 标记异常已处置 | 必须传 `X-Idempotency-Key`；已处置异常重复提交返回当前状态 | 需要 `infra.security.operate`，要求二次确认、处理意见和审计。 |
| `GET` | `/api/v1/infra/security/sensitive-operations` | 查询敏感操作留痕 | 只读接口，无需幂等键 | 需要 `infra.security.read`，输出字段默认脱敏。 |

## 分页与筛选

| 接口 | 分页规则 | 推荐筛选项 |
|------|----------|------------|
| `GET /policies` | 使用 `page`、`size`、`sort=-updatedAt` | `filter[policyCode]`、`filter[policyType]`、`filter[status]`、`filter[tenantId]`、`filter[updatedAt]gte/lte` |
| `GET /anomalies` | 使用 `page`、`size`、`sort=-detectedAt` | `filter[anomalyType]`、`filter[riskLevel]`、`filter[subjectType]`、`filter[tenantId]`、`filter[detectedAt]gte/lte`、`filter[status]` |
| `GET /sensitive-operations` | 使用 `page`、`size`、`sort=-occurredAt` | `filter[operationCode]`、`filter[result]`、`filter[targetType]`、`filter[tenantId]`、`filter[occurredAt]gte/lte` |

## 幂等

| 场景 | 规则 |
|------|------|
| 创建策略 | `policyCode` 唯一 + `X-Idempotency-Key` 去重。 |
| 发布/回滚策略 | 以 `policyId + idempotencyKey` 去重，防止重复发布或重复回滚。 |
| 密钥轮换 | 以 `keyRef + rotationRequestId` 去重，同一时刻只允许一个轮换流程。 |
| 异常处置 | 以 `anomalyId + idempotencyKey` 去重，避免重复点击造成多次处置。 |
| 签名/验签 | 无状态服务接口，可不要求幂等键，但每次调用都必须记录调用审计。 |

## 权限与审计

| 维度 | 要求 |
|------|------|
| 权限分层 | `infra.security.read` 查询；`infra.security.manage` 配置策略；`infra.security.operate` 发布/回滚/处置异常；`infra.security.secret.rotate` 轮换密钥；`infra.security.use` 调用签名服务。 |
| 审计字段 | 记录 `operatorAccountId`、`operatorPersonId`、`tenantId`、`traceId`、操作原因、前后版本、风险等级。 |
| 数据保护 | 返回结果中不暴露明文密钥、原始密码、完整敏感字段；仅返回引用、摘要或脱敏值。 |
| 多租户边界 | 租户级策略默认只对本租户管理员可见，全局策略仅平台安全管理员可维护。 |

## 治理动作约束

- 策略发布、回滚、密钥轮换、异常处置都属于治理动作，必须先展示差异摘要和影响面，再提交 `confirmToken`。
- 回滚只恢复安全策略快照，不自动变更账号主状态，也不覆盖 `01-组织与权限` 的认证主流程。
- 安全服务接口是通用基础设施调用，不等价于认证或授权接口，禁止外部系统绕过业务权限直接使用其结果判权。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 策略编码重复 | `409` | `SECURITY_POLICY_CODE_DUPLICATE` | `policyCode` 已存在。 |
| 规则表达式非法 | `422` | `SECURITY_POLICY_INVALID_RULE` | 脱敏、限频或密码规则不合法。 |
| 策略版本冲突 | `409` | `SECURITY_POLICY_VERSION_CONFLICT` | 并发更新导致版本不一致。 |
| 不支持的算法 | `422` | `SECURITY_ALGORITHM_UNSUPPORTED` | 请求的签名/密钥算法未被支持。 |
| 密钥轮换冲突 | `409` | `SECURITY_KEY_ROTATION_IN_PROGRESS` | 同一引用已有轮换任务。 |
| 签名校验失败 | `422` | `SECURITY_SIGNATURE_INVALID` | 返回失败结论并写安全审计。 |
| 命中限频策略 | `429` | `SECURITY_RATE_LIMIT_EXCEEDED` | 由统一限频策略返回。 |
| 异常已处置 | `409` | `SECURITY_ANOMALY_ALREADY_RESOLVED` | 不重复执行处置动作。 |
