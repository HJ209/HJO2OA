# Bootstrap-启动装配 模块设计

## 1. 文档目的

本文档定义 `HJO2OA-Bootstrap` 模块的装配策略、Profile 规则、环境配置分层、与各模块的协作关系和一期交付边界。它用于回答“应用如何启动、默认装配什么、不同阶段如何扩展、环境配置如何分层”这四个关键问题。

对应架构决策：D01（模块化单体）、D11（统一接口契约）、D19（工程启动条件）、D20（一期实施范围）、ADR-006（参考设计策略）。

## 2. 设计策略

### 2.1 核心定位

`HJO2OA-Bootstrap` 是**装配层**，不是业务层。它的目标是把 `Shared` 与各业务模块按既定边界组装成一个可运行的后端应用，而不是重新定义业务模型。

### 2.2 设计原则

- **单一启动入口**：一期只维护一个 Spring Boot 主应用，统一承载最小平台闭环。
- **默认最小闭环**：默认构建仅装配 `00/01/02/04/06` 的必做清单。
- **构建期与运行期分离**：Maven Profile 控制装配范围，Spring Profile 控制环境配置。
- **装配不拥有业务**：`Bootstrap` 不拥有任何业务主数据、聚合读模型或业务规则。
- **模块边界前置于装配便利**：不能因为启动方便而在默认装配中提前吞并后置或二期模块。

### 2.3 核心边界原则

| 原则 | 说明 |
|------|------|
| `Bootstrap` 只负责装配与启动 | 业务语义、数据所有权和事件语义仍归各业务模块 |
| Maven Profile 与 Spring Profile 必须分治 | 前者解决“打包什么”，后者解决“运行在哪个环境” |
| 默认依赖图必须等于一期最小闭环 | 不能把 `03/05/07` 或一期后置模块放入默认装配 |
| 全局配置统一由 `Bootstrap` 托管 | 数据源、缓存、消息、存储、日志、Shared 相关前缀统一集中管理 |
| 模块只暴露装配接入点，不反向依赖启动模块 | 业务模块通过标准 Bean、接口、事件接入，被 `Bootstrap` 扫描和装配 |

## 3. 装配分层

`Bootstrap` 的设计更适合按“启动入口层、模块装配层、环境配置层、运行引导层”划分。

### 3.1 启动入口层（Bootstrap Entry Layer）

| 组件 | 说明 |
|------|------|
| `com.hjo2oa.bootstrap.Hjo2oaApplication` | Spring Boot 主启动类，负责启动应用上下文 |

该层只负责启动，不承载业务逻辑。

### 3.2 模块装配层（Module Assembly Layer）

负责决定哪些 jar 进入应用依赖闭包，以及各模块如何进入容器。

| 装配层次 | 范围 | 说明 |
|----------|------|------|
| 默认装配 | `Shared + 00/01/02/04/06` 必做子模块 | 对应一期最小闭环 |
| 一期后置装配 | `org-sync-audit`、`process-monitor`、`portal-designer`、`portal-model`、`personalization`、`ecosystem` | 通过 `bootstrap-with-phase1-deferred` 显式启用 |
| 二期预留装配 | `03/05/07` 子模块 | 通过 `bootstrap-with-phase2-reserved` 显式启用 |

### 3.3 环境配置层（Environment Configuration Layer）

负责把公共配置和环境差异从代码中抽离。

| 配置文件 | 职责 |
|----------|------|
| `application.yml` | 全局默认值、统一配置前缀、基础中间件入口 |
| `application-local.yml` | 本地开发数据库/Redis/RabbitMQ 地址与调试日志 |
| `application-dev.yml` | 开发环境连接参数与日志级别 |
| `application-test.yml` | 测试环境连接参数与日志级别 |
| `application-prod.yml` | 生产环境连接参数与更保守的日志级别 |

### 3.4 运行引导层（Runtime Wiring Layer）

负责在应用启动时把 Shared 与业务模块的公共能力串起来，包括但不限于：

- 全局 Web 异常处理与统一响应格式
- 安全上下文与身份头解析
- 多租户、审计、消息、持久化相关基础组件
- 数据源、Redis、RabbitMQ、对象存储等公共运行依赖

## 4. 启动顺序

推荐启动顺序如下：

```text
Hjo2oaApplication
    -> 加载 application.yml 与当前 Spring Profile 配置
    -> 初始化 Shared 公共能力
    -> 初始化 00-平台基础设施模块
    -> 初始化 01-组织与权限
    -> 初始化 02-流程与表单
    -> 初始化 04-门户与工作台
    -> 初始化 06-消息移动与生态
    -> 对外提供 REST / 事件消费 / 定时任务能力
```

约束如下：

- `Shared` 必须先于业务模块装配完成。
- `00` 的基础能力必须先于依赖它的业务模块进入稳定可用状态。
- `03/05/07` 只在显式开启二期 Profile 时参与启动。

## 5. Profile 设计

### 5.1 Maven Profile：控制装配范围

| Profile | 装配范围 | 使用场景 |
|---------|----------|----------|
| 默认无 Profile | `Shared + 00/01/02/04/06` 必做子模块 | 一期最小闭环、本地开发、主线 CI |
| `bootstrap-with-phase1-deferred` | 默认装配 + 一期后置子模块 | 项目明确需要门户增强、生态接入、流程监控等能力时 |
| `bootstrap-with-phase2-reserved` | 默认装配 + `03/05/07` 二期模块 | 二期扩展或大范围联调时 |

Maven Profile 解决的是“哪些模块进入包”，不解决数据库地址、日志级别等环境差异。

### 5.2 Spring Profile：控制环境差异

| Profile | 配置文件 | 目标环境 | 现有特征 |
|---------|----------|----------|----------|
| 默认 | `application.yml` | 所有环境公共基础 | 定义端口、数据源、Redis、RabbitMQ、MyBatis Plus 和 `hjo2oa.*` 前缀 |
| `local` | `application-local.yml` | 本地开发 | 指向本地库和本地中间件，`com.hjo2oa` 日志级别为 `DEBUG` |
| `dev` | `application-dev.yml` | 开发环境 | 指向 `dev-*` 基础设施地址，日志为 `INFO` |
| `test` | `application-test.yml` | 测试环境 | 指向 `test-*` 基础设施地址，日志为 `INFO` |
| `prod` | `application-prod.yml` | 生产环境 | 指向 `prod-*` 基础设施地址，日志为 `WARN` |

Spring Profile 解决的是“同一套装配在不同环境下如何运行”，不改变模块装配边界。

## 6. 环境配置设计

### 6.1 公共配置分组

`application.yml` 统一托管如下配置前缀：

- `server.*`
- `spring.application.*`
- `spring.datasource.*`
- `spring.data.redis.*`
- `spring.rabbitmq.*`
- `mybatis-plus.*`
- `hjo2oa.tenant.*`
- `hjo2oa.security.*`
- `hjo2oa.audit.*`
- `hjo2oa.messaging.*`
- `hjo2oa.storage.*`
- `hjo2oa.cache.*`

这些配置用于描述运行参数，不重新定义各模块的数据所有权。

### 6.2 配置覆盖规则

- 公共默认值放在 `application.yml`。
- 环境差异只放在对应的 `application-<profile>.yml` 中。
- 模块内部若需要读取公共配置，应通过统一前缀读取，而不是在各模块重复定义连接参数。
- 敏感信息在正式环境中应通过外部配置中心、环境变量或部署平台注入，不应长期保留默认示例值。

### 6.3 一期配置重点

- 数据源：SQL Server 2017
- 缓存：Redis
- 消息：RabbitMQ
- ORM：MyBatis Plus 逻辑删除与下划线转驼峰
- Shared 相关：租户、安全、审计、Outbox、对象存储、缓存

## 7. 与各模块关系

| 模块 | 装配关系 | `Bootstrap` 责任 | 模块自身责任 |
|------|----------|------------------|--------------|
| `Shared` | 直接依赖 | 注册全局横切组件 | 提供统一抽象和基础实现 |
| `00-平台基础设施` | 默认装配 | 初始化基础设施运行时能力 | 拥有租户、审计、事件、缓存、配置、安全等平台主数据与实现 |
| `01-组织与权限` | 默认装配 | 接入安全链路和身份上下文 | 拥有账号、组织、角色、权限与身份主数据 |
| `02-流程与表单` | 默认装配 | 让流程/表单/待办进入主应用 | 拥有流程、表单、待办主数据 |
| `03-内容与知识` | 二期预留 | 仅在二期 Profile 中装配 | 拥有内容主模型与检索模型 |
| `04-门户与工作台` | 默认装配 | 装配首页、聚合接口、工作台入口 | 拥有门户配置和聚合读模型 |
| `05-协同办公应用` | 二期预留 | 仅在二期 Profile 中装配 | 拥有公文、会议、日程等业务主数据 |
| `06-消息移动与生态` | 默认装配 | 装配消息、渠道、移动端能力 | 拥有消息、送达、订阅偏好等主数据 |
| `07-数据服务与集成` | 二期预留 | 仅在二期 Profile 中装配 | 拥有开放 API、连接器、同步和治理能力 |

## 8. 数据所有权

`HJO2OA-Bootstrap` 不拥有业务主数据，也不拥有平台级业务主数据。

| 数据/能力 | 真正所有者 | `Bootstrap` 角色 |
|-----------|------------|------------------|
| 共享横切约定 | `HJO2OA-Shared` | 负责装配 |
| 平台基础设施主数据 | `00` 各子模块 | 负责装配与环境配置 |
| 组织、流程、门户、消息等业务主数据 | 对应业务模块 | 负责启动并暴露运行环境 |
| 数据源、缓存、MQ、对象存储连接参数 | `Bootstrap` 配置层 | 负责统一加载与覆盖 |

## 9. 一期交付范围

### 9.1 一期包含

- 一个可启动的 Spring Boot 主应用。
- 默认装配 `Shared + 00/01/02/04/06` 的依赖闭包。
- `application.yml` 与 `local/dev/test/prod` 四套环境覆盖文件。
- Shared 横切能力与基础设施模块的统一装配入口。

### 9.2 一期简化

- 一期只维护单一主程序，不拆分 admin/public/api 多入口应用。
- 一期只维护一套统一环境配置体系，不建设复杂的多部署拓扑编排方案。
- 一期默认不装配一期后置和二期模块，只保留显式 Profile 接入能力。

### 9.3 一期暂缓

- 多运行时主程序拆分。
- 复杂配置中心联动、灰度发布编排和多地域部署拓扑。
- 面向不同租户、不同业务线的独立启动包矩阵。

## 10. 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| 默认装配不断膨胀，失去一期边界 | 严格以 `D20` 和一期切片为准，默认依赖图只覆盖最小闭环 |
| Maven Profile 与 Spring Profile 混用 | 明确前者管装配、后者管环境，并在文档和构建脚本中固化 |
| 业务模块反向依赖 `Bootstrap` | 强制维持单向依赖：`Bootstrap -> Shared/业务模块` |
| 环境配置散落到模块内部 | 统一收口到 `application*.yml` 和 `hjo2oa.*` 前缀 |
| 把启动模块写成“总业务模块” | 禁止在 `Bootstrap` 内编写业务服务、业务 Controller 和业务持久化逻辑 |

## 11. 模块目录索引

| 目录 | 分层 | 说明 |
|------|------|------|
| `src/main/java/com/hjo2oa/bootstrap/` | 启动入口层 | Spring Boot 主启动类与后续全局装配代码 |
| `src/main/resources/application.yml` | 环境配置层 | 公共默认配置 |
| `src/main/resources/application-local.yml` | 环境配置层 | 本地环境覆盖配置 |
| `src/main/resources/application-dev.yml` | 环境配置层 | 开发环境覆盖配置 |
| `src/main/resources/application-test.yml` | 环境配置层 | 测试环境覆盖配置 |
| `src/main/resources/application-prod.yml` | 环境配置层 | 生产环境覆盖配置 |
