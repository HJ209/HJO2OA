# Shared-共享横切能力-模块功能

## 模块定位

`HJO2OA-Shared` 是全仓统一共享 jar，承载跨模块复用的横切能力、基础约定和最小公共实现，为 `HJO2OA-Bootstrap` 与 `00-07` 各业务模块提供一致的 Web、鉴权、租户、审计、消息、持久化和测试底座。

它只负责沉淀**通用抽象、统一约定和可复用组件**，不承载业务主数据，不替代 `00-平台基础设施` 对运行时资源和平台级主数据的所有权。

## 模块目标

- 统一 Result、分页、异常、基础值对象、标识类型等共享语义，避免各模块重复定义。
- 统一 HTTP 响应、全局异常处理、参数校验和请求上下文入口，避免接口风格漂移。
- 统一安全上下文、租户上下文、审计上下文、事件信封和持久化约定，降低跨模块协同成本。
- 统一测试夹具和测试基类，降低模块级契约测试、集成测试的搭建成本。

## 边界约束

- `HJO2OA-Shared` 可以被 `Bootstrap` 和所有业务模块依赖，但不得反向依赖任何 `01-07` 业务模块。
- `HJO2OA-Shared` 提供的是抽象和公共实现，不拥有租户主数据、审计记录主数据、消息主数据、账号主数据或业务表结构。
- RabbitMQ、Redis、PostgreSQL、MinIO 等运行时接入的真实资源归 `Bootstrap + 00-平台基础设施` 装配和落地，`Shared` 只定义统一接入面。
- `shared.testing` 仅服务测试代码，不作为生产运行链路的必需依赖。

## 子模块划分

| 子模块 | 核心职责 | 边界约束 | 被依赖方式 |
|--------|----------|----------|------------|
| `shared.kernel` | `Result`、分页对象、基础异常、值对象基类、标识类型、通用常量 | 不处理 HTTP、鉴权、消息、数据库细节 | 所有模块的最低层编译期依赖 |
| `shared.web` | 统一响应体、全局异常处理、参数校验、请求上下文、请求头解析 | 不实现业务 Controller，不拥有业务 API 语义 | `Bootstrap` 注册全局 Web 组件；各模块接口层直接复用 |
| `shared.security` | 认证/授权抽象、JWT 解析、身份头模型、安全上下文、鉴权注解 | 不拥有账号、角色、权限主数据，不承担登录业务流程 | `Bootstrap` 装配安全链路；`01/02/04/06` 等模块按接口或注解接入 |
| `shared.tenant` | 租户上下文、租户解析 SPI、租户透传、MyBatis Plus 多租户接入约定 | 不拥有租户档案、套餐、配额、初始化流程 | `00-tenant` 提供主数据与策略；所有持久化模块按约定注入 `tenantId` |
| `shared.audit` | 审计注解、操作人上下文、审计事件模型、审计拦截扩展点 | 不保存审计记录，不提供审计检索界面 | 写操作模块通过注解或 SPI 接入；`00-audit` 负责持久化与查询 |
| `shared.messaging` | 事件信封、Outbox 抽象、发布器接口、消费者幂等抽象、链路追踪字段 | 不拥有 Broker 拓扑、队列策略和事件主数据 | 事件生产者/消费者统一依赖；`00-event-bus` 与 `06` 提供实现 |
| `shared.persistence` | 通用 BaseEntity/BaseMapper、审计字段填充、逻辑删除、乐观锁、分页约定 | 不拥有任何业务 Repository 实现和表结构 | 各模块 `infrastructure.persistence` 统一继承和复用 |
| `shared.testing` | 测试基类、测试夹具、Mock 上下文、契约测试辅助工具 | 不进入生产 Bean 装配，不承载运行时逻辑 | 各模块测试代码按 test scope 依赖 |

## 依赖与协作关系

| 协作方向 | 协作内容 | 说明 |
|----------|----------|------|
| `Bootstrap -> Shared` | 全局组件注册 | 启动模块负责把共享 Web、安全、租户、审计等能力装配进 Spring 容器 |
| `00 -> Shared` | 平台级实现落地 | 平台基础设施模块负责把 `Shared` 中的抽象落实为 RabbitMQ、Redis、审计落库、多租户策略等运行能力 |
| `01/02/04/06 -> Shared` | 统一编码约定 | 业务模块通过公共异常、上下文、持久化基类和事件抽象避免重复建设 |
| `测试代码 -> shared.testing` | 测试统一底座 | 集成测试、契约测试和模块级测试复用同一套夹具与断言工具 |

## 一期交付边界

- 一期必须完整提供 `kernel/web/security/tenant/audit/messaging/persistence/testing` 八类共享能力骨架。
- 一期 `shared.security` 仅要求统一身份头、JWT、鉴权注解和上下文接入，不要求完整 SSO/生态登录能力。
- 一期 `shared.tenant` 以“共享数据库 + `tenantId` 字段注入”模式为主，不预设独立数据库路由实现。
- 一期 `shared.messaging` 以 RabbitMQ + Outbox 统一抽象为主，不扩展多 Broker 实现。
- 一期 `shared.testing` 以集成测试基类、Mock 上下文和契约测试辅助为主，不建设完整测试平台。

## 子模块目录索引

| 子模块 | 包路径 | 说明 |
|--------|--------|------|
| `shared.kernel` | `src/main/java/com/hjo2oa/shared/kernel/` | 基础语义、异常、分页和值对象约定 |
| `shared.web` | `src/main/java/com/hjo2oa/shared/web/` | 统一接口响应、异常处理、请求上下文 |
| `shared.security` | `src/main/java/com/hjo2oa/shared/security/` | 安全上下文、JWT、鉴权扩展点 |
| `shared.tenant` | `src/main/java/com/hjo2oa/shared/tenant/` | 租户上下文与多租户接入约定 |
| `shared.audit` | `src/main/java/com/hjo2oa/shared/audit/` | 审计注解、审计上下文、审计事件模型 |
| `shared.messaging` | `src/main/java/com/hjo2oa/shared/messaging/` | 事件信封、Outbox、发布/消费抽象 |
| `shared.persistence` | `src/main/java/com/hjo2oa/shared/persistence/` | 通用持久化基类、审计字段与分页约定 |
| `shared.testing` | `src/main/java/com/hjo2oa/shared/testing/` | 测试基类、夹具和契约测试辅助工具 |
