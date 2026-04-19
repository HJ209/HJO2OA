# Shared-共享横切能力 模块设计

## 1. 文档目的

本文档定义 `HJO2OA-Shared` 模块的整体设计策略、内部层次、职责边界、跨模块协作方式和一期交付范围。它是工程骨架、业务模块依赖规则和横切能力沉淀方式的上层约束文档。

对应架构决策：D01（模块化单体）、D11（统一接口契约）、D12（统一事件契约）、D19（工程启动条件）、D20（一期实施范围）、ADR-006（参考设计策略）。

## 2. 设计策略

### 2.1 设计原则

`HJO2OA-Shared` 不追求建设“另一个基础设施中心”，而是负责把所有模块都会重复碰到的公共语义和接入约定收敛成统一代码资产。其设计原则如下：

- **先统一契约，再统一实现**：优先统一异常、上下文、消息信封、持久化约定和测试基类，再由 `Bootstrap` 与 `00-平台基础设施` 负责装配和运行时实现。
- **只沉淀最低公共分母**：只有同时被多个模块复用、且不携带业务主语义的内容才能进入 `Shared`。
- **横切抽象不反向吞并业务**：`Shared` 不能拥有账号、角色、租户档案、审计记录、待办、消息等业务或平台主数据。
- **运行时与测试时分离**：生产运行能力和测试辅助能力分包治理，避免测试工具污染运行时依赖图。

### 2.2 核心边界原则

| 原则 | 说明 |
|------|------|
| `Shared` 负责约定和复用，不负责平台主数据 | 租户档案归 `00-tenant`，审计记录归 `00-audit`，消息总线归 `00-event-bus`，账号与权限主数据归 `01` |
| 业务模块可以依赖 `Shared`，`Shared` 不得依赖业务模块 | 依赖方向必须始终从业务向共享流动 |
| 横切能力优先以接口、注解、上下文和基础模型表达 | 避免把中间件实现细节直接固化进业务代码 |
| `testing` 只服务测试 | 测试基类、夹具、断言工具不进入生产链路 |
| `Shared` 不替代 `Bootstrap` | Bean 装配、Profile 切换、环境配置归启动模块负责 |

## 3. 内部分层

`Shared` 更适合按“基础约定层、运行支撑层、测试支撑层”划分，而不是套用纯业务域的 design-time/run-time/query-model 分法。

### 3.1 基础约定层（Foundation Layer）

负责所有模块都必须遵循的基础类型与持久化约定。

| 子模块 | 核心职责 | 典型内容 |
|--------|----------|----------|
| `shared.kernel` | 统一基础语义 | `Result`、分页对象、基础异常、值对象基类、标识类型、通用枚举/常量 |
| `shared.persistence` | 统一持久化约定 | 基础实体、审计字段、逻辑删除、乐观锁、分页参数、通用 Mapper 约束 |

这一层不感知 HTTP、Broker 或具体业务语义，是其他共享子包的依赖基础。

### 3.2 运行支撑层（Runtime Support Layer）

负责把横切能力以统一扩展点和上下文的方式暴露给运行链路。

| 子模块 | 核心职责 | 说明 |
|--------|----------|------|
| `shared.web` | 统一 Web 接入规范 | 统一响应体、全局异常处理、校验失败输出、请求上下文 |
| `shared.security` | 统一安全接入规范 | 安全上下文、身份头模型、JWT 解析、授权注解与拦截扩展点 |
| `shared.tenant` | 统一租户接入规范 | `TenantContext`、租户解析 SPI、租户透传、多租户拦截器约定 |
| `shared.audit` | 统一审计接入规范 | 审计注解、审计上下文、操作人快照、审计事件模型 |
| `shared.messaging` | 统一消息接入规范 | 事件信封、Outbox 抽象、发布器接口、消费者幂等抽象、链路追踪字段 |

这层只提供**可复用的接入面**，真实落地由 `Bootstrap` 与各基础设施模块装配。

### 3.3 测试支撑层（Testing Support Layer）

| 子模块 | 核心职责 | 说明 |
|--------|----------|------|
| `shared.testing` | 测试底座 | 提供模块集成测试基类、Mock 安全/租户上下文、测试夹具、契约测试辅助工具 |

`shared.testing` 可以依赖前两层，但前两层不得反向依赖测试支撑层。

### 3.4 内部依赖规则

- `shared.kernel` 是最底层，不依赖其他共享子包。
- `shared.persistence` 只允许依赖 `shared.kernel` 和必要的框架 API。
- `shared.web`、`shared.security`、`shared.tenant`、`shared.audit`、`shared.messaging` 可以依赖 `shared.kernel`，必要时通过接口协作，不直接耦合具体业务实现。
- `shared.testing` 可以按需依赖所有运行时共享子包，但不得被生产代码依赖。

## 4. 核心抽象关系

```text
Result / PageResult / BizException
        │
        ├── RequestContext
        │       ├── IdentitySnapshot
        │       └── TenantSnapshot
        │
        ├── AuditContext
        │       └── OperatorSnapshot
        │
        ├── EventEnvelope
        │       ├── OutboxMessageContract
        │       └── IdempotentConsumerContract
        │
        └── BaseEntity / AuditableEntity / TenantScopedEntity
```

说明：

- `Result/PageResult/BizException` 是接口和应用层的统一基础语义。
- `RequestContext`、`IdentitySnapshot`、`TenantSnapshot` 负责把请求级上下文贯穿到 Web、安全、租户和审计链路。
- `AuditContext` 与 `OperatorSnapshot` 只记录统一审计语义，不拥有审计落库能力。
- `EventEnvelope`、`OutboxMessageContract`、`IdempotentConsumerContract` 统一跨模块事件协作契约。
- `BaseEntity`、`AuditableEntity`、`TenantScopedEntity` 只描述持久化约定，不拥有表结构。

## 5. 跨模块集成方式

### 5.1 依赖关系

| 依赖方向 | 集成内容 | 集成方式 |
|----------|----------|----------|
| `Bootstrap -> Shared` | 全局 Bean 装配 | 启动模块负责扫描、注册全局异常处理、安全链路、租户和审计上下文组件 |
| `00 -> Shared` | 平台运行时实现 | `00` 模块负责实现 Outbox 发布、审计落库、租户主数据、多租户策略、缓存与安全策略 |
| `01 -> Shared` | 身份与权限接入 | `01` 模块提供账号、角色、身份上下文主数据，`Shared` 只消费标准化接口和上下文 |
| `02/04/06 -> Shared` | 业务链路统一约定 | 流程、门户、消息模块统一复用异常、事件、持久化和上下文模型 |
| `测试代码 -> shared.testing` | 统一测试底座 | 通过 test scope 复用测试基类、夹具和契约断言 |

### 5.2 包级职责与被依赖方式

| 包 | 被谁依赖 | 依赖方式 | 明确不负责 |
|----|----------|----------|------------|
| `shared.kernel` | 所有模块 | 编译期基础依赖 | 不负责 Web、DB、MQ 细节 |
| `shared.web` | `Bootstrap`、各模块接口层 | 全局异常处理、统一响应体、上下文解析 | 不负责业务 API 实现 |
| `shared.security` | `Bootstrap`、`01/02/04/06` | 安全上下文、注解、过滤/拦截扩展点 | 不负责账号与权限主数据 |
| `shared.tenant` | `Bootstrap`、各持久化模块 | `TenantContext`、租户注入、透传约定 | 不负责租户初始化、套餐和配额 |
| `shared.audit` | 所有写链路模块 | 注解或 SPI 方式记录审计意图 | 不负责审计落库和归档 |
| `shared.messaging` | 事件生产者/消费者模块 | 统一事件信封、Outbox、幂等消费契约 | 不负责队列拓扑和 Broker 治理 |
| `shared.persistence` | 各 `infrastructure.persistence` | 基础实体和 Mapper 约定 | 不负责 Repository 业务查询语义 |
| `shared.testing` | 测试代码 | 测试基类和夹具 | 不负责生产运行 |

## 6. 数据所有权

`HJO2OA-Shared` 自身**不拥有业务主数据，也不拥有平台级运行主数据**。它只定义数据形态和接入约定。

| 数据/能力 | 真正所有者 | `Shared` 角色 |
|-----------|------------|---------------|
| 租户档案、套餐、配额 | `00-tenant` | 提供租户上下文与注入约定 |
| 审计记录、审计归档 | `00-audit` | 提供审计注解、上下文和审计事件模型 |
| 事件消息、Outbox 消息、投递记录 | `00-event-bus` / `06` | 提供信封和发布/消费抽象 |
| 账号、角色、身份上下文主数据 | `01-组织与权限` | 提供安全上下文和身份头模型 |
| 待办、消息、门户聚合数据 | `02` / `04` / `06` | 提供统一异常、上下文和持久化约定 |

## 7. 一期交付范围

### 7.1 一期包含

- `shared.kernel`：统一结果对象、分页对象、基础异常和值对象基类。
- `shared.web`：统一响应体、全局异常处理、参数校验和请求上下文。
- `shared.security`：统一身份头、JWT、基础鉴权注解与安全上下文。
- `shared.tenant`：`tenantId` 透传、上下文和 MyBatis Plus 多租户接入约定。
- `shared.audit`：审计注解、操作人上下文和审计事件模型。
- `shared.messaging`：RabbitMQ + Outbox 所需的统一抽象。
- `shared.persistence`：基础实体、审计字段、逻辑删除、乐观锁和分页约定。
- `shared.testing`：集成测试基类、Mock 上下文和契约测试辅助工具。

### 7.2 一期简化

- `shared.security` 一期只覆盖内部平台统一鉴权入口，不扩展完整 SSO、OAuth2、生态登录协议。
- `shared.tenant` 一期只覆盖共享库 + 租户字段模型，不提前设计独立数据库动态路由。
- `shared.messaging` 一期只覆盖 RabbitMQ + Outbox，不扩展 Kafka、Pulsar 等多 Broker 抽象。
- `shared.testing` 一期只建设后端测试底座，不建设完整的端到端自动化平台。

### 7.3 一期暂缓

- 复杂统一 starter 体系与自动装配元数据细分。
- 多 Broker、跨 Region、复杂消息编排抽象。
- 高级安全能力，如风险画像、自适应风控和复杂多因子认证。
- 完整的测试平台化能力，如统一数据工厂平台、全链路录制回放。

## 8. 风险与约束

| 风险 | 缓解措施 |
|------|----------|
| `Shared` 膨胀成“万能工具箱” | 只有跨多个模块复用且不拥有业务语义的能力才能进入 `Shared` |
| 共享抽象侵入业务模型 | 严格限制为 Result、上下文、注解、SPI、基础实体等最低公共分母 |
| 把运行时实现偷偷放进 `Shared` | RabbitMQ、审计落库、租户主数据、账号权限主数据统一回归 `Bootstrap + 00/01` |
| 测试工具污染生产依赖 | `shared.testing` 独立分包，仅供 test scope 依赖 |
| 包间依赖失控 | 维持 `kernel` 最底层、`testing` 仅向下依赖的单向规则 |

## 9. 子模块目录索引

| 子模块 | 目录名 | 分层 | 说明 |
|--------|--------|------|------|
| `shared.kernel` | `com/hjo2oa/shared/kernel/` | 基础约定层 | 结果对象、异常、分页、值对象、标识类型 |
| `shared.web` | `com/hjo2oa/shared/web/` | 运行支撑层 | 统一响应、异常处理、请求上下文、校验接入 |
| `shared.security` | `com/hjo2oa/shared/security/` | 运行支撑层 | 安全上下文、JWT、鉴权注解与拦截扩展点 |
| `shared.tenant` | `com/hjo2oa/shared/tenant/` | 运行支撑层 | 租户上下文、租户解析 SPI、多租户接入约定 |
| `shared.audit` | `com/hjo2oa/shared/audit/` | 运行支撑层 | 审计注解、操作人上下文、审计事件模型 |
| `shared.messaging` | `com/hjo2oa/shared/messaging/` | 运行支撑层 | 事件信封、Outbox、发布/消费抽象、幂等契约 |
| `shared.persistence` | `com/hjo2oa/shared/persistence/` | 基础约定层 | 基础实体、Mapper 约定、逻辑删除、乐观锁、分页 |
| `shared.testing` | `com/hjo2oa/shared/testing/` | 测试支撑层 | 测试基类、夹具、Mock 上下文、契约测试辅助 |
