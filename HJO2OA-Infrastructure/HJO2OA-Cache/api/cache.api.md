# 缓存管理 API

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 策略管理 | `GET /api/v1/infra/cache-policies`、`POST /api/v1/infra/cache-policies`、`GET /api/v1/infra/cache-policies/{policyId}`、`PUT /api/v1/infra/cache-policies/{policyId}` | 管理命名空间、TTL 和淘汰策略。 |
| 失效治理 | `POST /api/v1/infra/cache-invalidation`、`POST /api/v1/infra/cache-invalidation/batch`、`GET /api/v1/infra/cache-invalidation/records` | 手工执行失效、批量刷新并查看轨迹。 |
| 监控查询 | `GET /api/v1/infra/cache-metrics`、`GET /api/v1/infra/cache-metrics/hot-keys` | 查询命中率、容量、热点 Key 等指标。 |
| 命名空间排障 | `GET /api/v1/infra/cache-namespaces/{namespace}` | 查看命名空间策略、最近失效记录和运行摘要。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/infra/cache-policies` | 新建缓存策略 | 需携带 `X-Idempotency-Key`；`namespace` 唯一。 |
| `PUT` | `/api/v1/infra/cache-policies/{policyId}` | 修改 TTL、容量、淘汰策略、失效模式 | 不允许直接修改已有命名空间的语义归属。 |
| `POST` | `/api/v1/infra/cache-invalidation` | 对单个命名空间或 Key 执行手工失效 | 必须记录 `reasonType=MANUAL` 并写入审计。 |
| `POST` | `/api/v1/infra/cache-invalidation/batch` | 批量按命名空间或模式失效 | 高风险操作，需二次确认。 |
| `GET` | `/api/v1/infra/cache-invalidation/records` | 查询失效轨迹 | 默认按 `invalidatedAt,desc` 返回。 |
| `GET` | `/api/v1/infra/cache-metrics` | 查询命中率、容量和失效统计 | 只读监控接口，不返回业务主数据。 |

## 分页与筛选

- 列表接口统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 策略列表按 `updatedAt,desc`
  - 失效记录按 `invalidatedAt,desc`
- 支持筛选字段：
  - `filter[namespace]like`
  - `filter[backendType]`
  - `filter[invalidationMode]`
  - `filter[active]`
  - `filter[reasonType]`
  - `filter[reasonRef]`
  - `filter[invalidatedAt]gte` / `filter[invalidatedAt]lte`
- 指标接口不分页，支持 `namespace`、`timeWindow`、`topN`。

## 幂等

- 策略写入、手工失效和批量失效接口必须携带 `X-Idempotency-Key`。
- 手工失效以 `namespace + invalidateKey + reasonType + 幂等键` 判定重复，避免重复删缓存。
- 事件驱动失效在内部以 `eventId + namespace + invalidateKey` 去重。
- 监控查询接口天然幂等。

## 权限与审计约束

- 仅缓存管理员、运维管理员可新增或修改缓存策略。
- 手工失效和批量失效属于治理动作，必须记录操作者、原因、影响范围和 `traceId`。
- 业务模块只能通过统一缓存抽象间接使用缓存，不开放直接治理接口。
- 监控视图可开放给只读运维角色，但不得下发写操作。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 缓存策略不存在 | `404` | `INFRA_CACHE_POLICY_NOT_FOUND` | `policyId` 或命名空间不存在。 |
| 命名空间冲突 | `409` | `INFRA_CACHE_NAMESPACE_CONFLICT` | `namespace` 已存在。 |
| 后端类型不支持 | `422` | `INFRA_CACHE_BACKEND_UNSUPPORTED` | 一期仅支持已注册后端类型。 |
| 失效范围过大 | `422` | `INFRA_CACHE_INVALIDATION_SCOPE_TOO_LARGE` | 批量模式超出治理阈值。 |
| 高风险治理未确认 | `409` | `INFRA_CACHE_GOVERNANCE_CONFIRMATION_REQUIRED` | 缺少二次确认令牌。 |
| 监控时间窗非法 | `422` | `INFRA_CACHE_METRIC_WINDOW_INVALID` | 时间范围或粒度不合法。 |
