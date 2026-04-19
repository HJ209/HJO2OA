# M0工程骨架方案

对应架构决策：D01（模块化单体）、D11（统一接口契约）、D12（统一事件契约）、D13（最终一致性）、D19（工程启动条件）、D20（一期实施范围）。

## 1. 文档目的

本文档定义 HJO2OA 一期 `M0：工程骨架与基础设施` 的推荐落地方式，用于把当前已经完成的架构收敛、命名统一和一期范围收敛转化为**真正可启动开发**的工程骨架。

本文档不处理 M1/M2 的完整业务建模，而是先回答以下问题：

- 后端工程应如何组织，才能符合“模块化单体 + 独立基础设施服务”
- 当前仓库根目录下的业务模块目录应如何与实际可运行代码协同
- 一期必须先落哪些基础设施能力，哪些能力只预留接入点
- 如何保证后续进入编码时不再因包名、目录、边界和依赖方向产生二次分歧

## 2. 设计结论

M0 推荐采用以下工程策略：

- **部署形态**：一个可部署的后端主应用
- **代码组织方式**：每个根目录业务模块是独立 Maven 子模块，内部按子模块包严格分层
- **文档与代码内聚方式**：每个业务模块同时承载设计文档、接口契约、事件说明和后端代码
- **构建工具策略**：采用 Maven 聚合构建，根 `pom.xml` 统一管理版本，各业务模块作为子模块独立构建
- **服务化策略**：M0 不拆微服务，不把每个子模块做成独立部署单元

简化理解：

> 每个根目录业务模块就是 Maven 子工程，文档和代码放在一起，不再维护独立的 `backend/` 目录。

这样做的原因是：

- 文档跟代码放一起，维护成本更低，不需要在两套目录间来回跳转
- 每个模块就是一个可构建单元，天然表达模块边界
- Maven 聚合构建既保证统一管理，又保留模块独立性
- 后续服务化拆分时，直接按模块拆出即可，不需要重新组织目录

## 3. 仓库层结构建议

仓库根目录结构：

```text
pom.xml                              # 根聚合 POM（packaging=pom）
HJO2OA-Shared/                       # 共享横切能力 jar
HJO2OA-Bootstrap/                    # Spring Boot 启动模块 jar
HJO2OA-Infrastructure/               # 业务模块父 POM（packaging=pom）
HJO2OA-OrgPerm/                      # 业务模块父 POM
HJO2OA-WorkflowForm/                 # 业务模块父 POM
HJO2OA-Content/                      # 业务模块父 POM（二期）
HJO2OA-Portal/                       # 业务模块父 POM
HJO2OA-Collaboration/                # 业务模块父 POM（二期）
HJO2OA-Messaging/                    # 业务模块父 POM
HJO2OA-DataServices/                 # 业务模块父 POM（二期）
docs/                                # 全局架构、契约、实施与工程文档
frontend/                            # 前端工作区骨架
```

### 3.1 业务模块目录的定位

每个业务模块是 Maven 父 POM 聚合层，不产出 jar；其下每个子模块是独立 Maven 子工程，产出 jar：

```text
<module>/
  pom.xml                           # 父 POM（packaging=pom），聚合子模块
  docs/
    module-overview.md
    development-tasks.md
    domain-model.md
    module-design.md
  <submodule>/
    pom.xml                         # 子模块 POM（packaging=jar）
    api/                            # 接口契约文档
    docs/                           # 子模块设计文档
    events/                         # 事件契约文档
    frontend/                       # 前端职责文档
    src/
      main/
        java/com/hjo2oa/<module-prefix>/<submodule>/
          application/
          domain/
          infrastructure/
          interfaces/
        resources/
      test/
        java/com/hjo2oa/<module-prefix>/<submodule>/
```

- `docs/`：模块级设计文档
- `<submodule>/pom.xml`：子模块独立构建配置
- `<submodule>/api/`、`events/`、`docs/`、`frontend/`：子模块级契约与职责文档
- `<submodule>/src/`：子模块实际 Java 源码，按 application/domain/infrastructure/interfaces 分层

### 3.2 共享层与启动模块

根目录下需要一个共享模块承载横切能力：

```text
HJO2OA-Shared/
  pom.xml
  src/
    main/java/com/hjo2oa/shared/
      kernel/
      web/
      security/
      tenant/
      audit/
      messaging/
      persistence/
      testing/
```

以及一个启动模块：

```text
HJO2OA-Bootstrap/
  pom.xml
  src/
    main/java/com/hjo2oa/bootstrap/
    main/resources/
      application.yml
      application-local.yml
      application-dev.yml
      application-test.yml
      application-prod.yml
```

- `HJO2OA-Shared/`：共享内核、Web、安全、租户、审计、消息、持久化、测试等横切能力
- `HJO2OA-Bootstrap/`：Spring Boot 启动类、自动配置装配、全局组件注册；默认装配一期最小闭环子模块，并通过 Maven Profile 扩展后置与二期模块，作为最终可部署单元

## 4. 推荐后端工程结构

### 4.1 业务模块父 POM 结构

以 `HJO2OA-OrgPerm` 为例：

```text
HJO2OA-OrgPerm/
  pom.xml                         # 父 POM（packaging=pom）
  docs/
    module-overview.md
    development-tasks.md
    domain-model.md
  HJO2OA-OrgStructure/
    pom.xml                       # 子模块 POM（packaging=jar）
    api/
    docs/
    events/
    frontend/
    src/
      main/java/com/hjo2oa/org/structure/
      test/java/com/hjo2oa/org/structure/
  HJO2OA-PositionAssignment/
    pom.xml
    api/
    docs/
    events/
    frontend/
    src/
      main/java/com/hjo2oa/org/position/assignment/
      test/java/com/hjo2oa/org/position/assignment/
  HJO2OA-PersonAccount/
    pom.xml
    api/
    docs/
    events/
    frontend/
    src/
      main/java/com/hjo2oa/org/person/account/
      test/java/com/hjo2oa/org/person/account/
  HJO2OA-RoleResourceAuth/
    pom.xml
    ...
  HJO2OA-DataPermission/
    pom.xml
    ...
  HJO2OA-IdentityContext/
    pom.xml
    ...
  HJO2OA-OrgSyncAudit/
    pom.xml
    ...
```

说明：

- 业务模块 `pom.xml` 是父 POM，`packaging=pom`，只做聚合，不产出 jar
- 每个子模块有自己的 `pom.xml`，`packaging=jar`，独立构建
- 子模块的 `api/`、`docs/`、`events/`、`frontend/` 继续保留为契约文档目录
- 子模块的 `src/` 承载实际 Java 源码
- 二期模块（`HJO2OA-Content`、`HJO2OA-Collaboration`、`HJO2OA-DataServices`）暂不落代码，但保留命名空间预留

### 4.2 模块内统一分层

每个模块/子模块代码统一采用以下分层：

```text
com.hjo2oa.<module>.<submodule>
  application/
  domain/
  infrastructure/
  interfaces/
```

职责如下：

- `application/`
  - 用例编排
  - 事务边界
  - 权限校验接入
  - DTO 转换
  - 命令/查询处理

- `domain/`
  - 聚合根、实体、值对象
  - 领域服务
  - 领域规则
  - 领域事件定义

- `infrastructure/`
  - MyBatis Mapper / Repository 实现
  - Redis / RabbitMQ / MinIO / PostgreSQL 适配
  - 外部服务客户端
  - Outbox、审计、缓存、第三方渠道实现

- `interfaces/`
  - REST Controller
  - 内部 Facade
  - 消息监听器
  - 请求/响应模型

### 4.3 典型子模块示例

以 `HJO2OA-TodoCenter` 为例：

```text
HJO2OA-WorkflowForm/HJO2OA-TodoCenter/
  pom.xml                           # packaging=jar
  api/
  docs/
  events/
  frontend/
  src/
    main/
      java/com/hjo2oa/todo/center/
        application/
          command/
          query/
          service/
        domain/
          model/
          service/
          event/
        infrastructure/
          persistence/
          messaging/
          cache/
        interfaces/
          rest/
          listener/
      resources/
    test/
      java/com/hjo2oa/todo/center/
```

## 5. 业务模块目录与 Maven 构件 / Java 包的映射规则

### 5.1 映射原则

`docs/engineering/engineering-naming-map.md` 是唯一映射依据。

目录 -> Maven artifactId -> Java 包映射：

| 子模块目录 | artifactId | Java 根包 |
|-----------|-----------|----------|
| `HJO2OA-OrgPerm/HJO2OA-OrgStructure/` | `HJO2OA-OrgStructure` | `com.hjo2oa.org.structure` |
| `HJO2OA-WorkflowForm/HJO2OA-FormMetadata/` | `HJO2OA-FormMetadata` | `com.hjo2oa.form.metadata` |
| `HJO2OA-WorkflowForm/HJO2OA-TodoCenter/` | `HJO2OA-TodoCenter` | `com.hjo2oa.todo.center` |
| `HJO2OA-Messaging/HJO2OA-MessageCenter/` | `HJO2OA-MessageCenter` | `com.hjo2oa.msg.message.center` |

所有子模块 `groupId` 统一为 `com.hjo2oa`。

### 5.2 约束

- 子模块 `api/`、`events/`、`docs/`、`frontend/` 继续保留，作为契约与职责文档
- 实际后端代码放在 `<module>/<submodule>/src/main/java/` 下，按 application/domain/infrastructure/interfaces 分层
- 子模块 jar 之间禁止循环依赖
- 子模块只能依赖 `HJO2OA-Shared` 和同模块内的兄弟子模块（需显式声明）
- 跨模块依赖必须通过接口/事件，禁止直接依赖其他业务模块的子模块 jar
- 后续若需要自动生成代码或接口文档，应以子模块目录中的契约文档为输入，以 `src/` 中的实现为输出

### 5.3 工程构建约束

- 根 `pom.xml` 统一维护 Java、插件和注解处理器版本，子模块不再各自漂移
- 全仓统一使用 Java 17、UTF-8、`-parameters` 编译参数和 Maven 3.9+ 基线
- Lombok、MapStruct 的 annotation processor 由根 POM 统一配置，避免子模块重复声明编译链
- `maven-enforcer-plugin` 负责在构建早期阻断错误的 Java / Maven 版本与缺失插件版本
- `maven-surefire-plugin` 统一使用 UTF-8 测试编码与关闭 module path，避免多模块测试环境差异

## 6. 一期必须落地的后端代码包

### 6.1 00-平台基础设施

M0 必须创建以下一级包：

```text
com.hjo2oa.infra.event.bus
com.hjo2oa.infra.audit
com.hjo2oa.infra.cache
com.hjo2oa.infra.config
com.hjo2oa.infra.dictionary
com.hjo2oa.infra.error.code
com.hjo2oa.infra.attachment
com.hjo2oa.infra.i18n
com.hjo2oa.infra.timezone
com.hjo2oa.infra.tenant
com.hjo2oa.infra.security
com.hjo2oa.infra.scheduler
com.hjo2oa.infra.data.i18n
```

其中 M0 真正要做到“可运行”的核心是：

- 事件总线框架
- 多租户上下文与自动注入
- 审计日志基础能力
- Redis 缓存接入
- 统一错误码与异常处理
- 附件中心基础抽象
- 定时任务框架

### 6.2 01-组织与权限

M0 只需要先完成**空骨架 + 统一横切接入点**：

```text
com.hjo2oa.org.structure
com.hjo2oa.org.position.assignment
com.hjo2oa.org.person.account
com.hjo2oa.org.role.resource.auth
com.hjo2oa.org.data.permission
com.hjo2oa.org.identity.context
```

要求：

- 具备统一模块扫描能力
- 具备基础 Controller / Application / Domain / Infrastructure 空层结构
- 为 M1 业务落地预留 Repository、事件发布和权限校验扩展点

### 6.3 02-流程与表单

M0 先建骨架，不在 M0 完成完整流程能力：

```text
com.hjo2oa.process.definition
com.hjo2oa.process.instance
com.hjo2oa.process.action.engine
com.hjo2oa.form.metadata
com.hjo2oa.form.renderer
com.hjo2oa.todo.center
```

要求：

- `process` / `form` / `todo` 三条 API 分组独立
- 预留 Flowable 封装层接入点
- 预留 `todo.item.*` 事件发布/消费扩展点

### 6.4 04-门户与工作台

```text
com.hjo2oa.portal.widget.config
com.hjo2oa.portal.aggregation.api
com.hjo2oa.portal.home
```

要求：

- 聚合层只能消费其他模块公开接口或事件投影，不直接写入其他模块主数据
- 首页聚合缓存与门户渲染模型在 M0 仅需预留骨架

### 6.5 06-消息移动与生态

```text
com.hjo2oa.msg.message.center
com.hjo2oa.msg.event.subscription
com.hjo2oa.msg.channel.sender
com.hjo2oa.msg.mobile.support
```

要求：

- `06` 不拥有待办主数据
- `06` 只消费 `todo.item.*`、`process.task.overdue` 等事件做提醒和触达
- 渠道发送与消息中心分层清晰，避免“消息模型”和“渠道适配器”混写

## 7. 共享层结构建议

`HJO2OA-Shared/` 建议进一步拆为以下子包：

```text
com.hjo2oa.shared.kernel
com.hjo2oa.shared.web
com.hjo2oa.shared.security
com.hjo2oa.shared.tenant
com.hjo2oa.shared.audit
com.hjo2oa.shared.messaging
com.hjo2oa.shared.persistence
com.hjo2oa.shared.testing
```

职责如下：

- `kernel`：通用 Result、分页、基础异常、值对象基类、标识类型
- `web`：统一响应体、异常处理器、参数校验、请求上下文
- `security`：认证、授权拦截器、JWT 解析、身份头处理
- `tenant`：租户上下文、租户解析、MyBatis Plus 多租户集成
- `audit`：审计注解、审计拦截、审计上下文
- `messaging`：事件信封、Outbox 抽象、发布器接口、消费者幂等抽象
- `persistence`：通用 BaseMapper、审计字段填充、逻辑删除、乐观锁
- `testing`：测试基类、测试夹具、契约测试辅助工具

## 8. M0 基础设施接入点

M0 必须至少打通以下接入：

### 8.1 PostgreSQL

- 主业务数据库
- 多租户字段注入
- 审计字段自动填充
- 逻辑删除与乐观锁基础配置

### 8.2 Redis

- 身份上下文缓存
- 权限缓存
- 幂等键缓存
- 聚合读模型缓存预留

### 8.3 RabbitMQ

- 统一事件发布器
- Outbox 扫描与发布
- 死信、重试、幂等消费抽象

### 8.4 MinIO / S3 兼容对象存储

- 附件中心抽象
- 上传、下载、预览链接生成
- 文件元数据与对象存储分离

### 8.5 Flowable

- M0 仅完成依赖接入与封装层预留
- M2 再落完整流程定义和实例能力
- 禁止直接将 Flowable 原生模型暴露给业务层

## 9. 依赖方向约束

### 9.1 模块内依赖方向

统一允许：

```text
interfaces -> application -> domain
infrastructure -> domain / application
```

统一禁止：

- `domain` 依赖 `interfaces`
- `domain` 依赖外部 SDK 具体实现
- `application` 直接操作其他模块数据库表
- `portal` / `msg` 反向拥有 `org` / `process` 主数据

### 9.2 跨模块依赖规则

- 跨模块**写联动**：必须通过事件总线
- 跨模块**只读查询**：允许通过内部 Facade / Application Service
- 跨模块禁止直接引用对方 `infrastructure.persistence` 层实现
- 跨模块禁止共享数据库表写入职责

### 9.3 一期高风险边界的硬约束

- `02-HJO2OA-TodoCenter` 持有待办投影数据所有权
- `06-HJO2OA-MessageCenter` 持有消息通知与送达记录所有权
- `04-HJO2OA-AggregationApi` 只做门户读模型聚合，不承担通用开放 API 职责
- `07` 即使提前启用同步框架，也不进入一期最小闭环验收

## 10. 配置分层建议

推荐配置分层：

```text
application.yml
application-local.yml
application-dev.yml
application-test.yml
application-prod.yml
```

按功能分组配置前缀：

- `hjo2oa.tenant.*`
- `hjo2oa.security.*`
- `hjo2oa.audit.*`
- `hjo2oa.messaging.*`
- `hjo2oa.storage.*`
- `hjo2oa.cache.*`
- `hjo2oa.flowable.*`

### 10.1 Bootstrap 装配策略

- 默认构建仅装配一期最小闭环子模块：`00`、`01`、`02`、`04`、`06` 的必做清单
- 通过 `bootstrap-with-phase1-deferred` Profile 显式接入一期后置子模块：`org-sync-audit`、`process-monitor`、`portal-model`、`personalization`、`portal-designer`、`ecosystem`
- 通过 `bootstrap-with-phase2-reserved` Profile 显式接入 `03`、`05`、`07` 二期预留子模块
- 若进入更大范围联调或二期建设，再按需要启用对应 Profile，避免默认装配提前引入非一期边界

## 11. M0 交付清单

M0 结束时，至少应交付：

- 一个可启动的 Spring Boot 主应用
- 一套统一响应体与全局异常处理机制
- 一套统一身份头、租户头、请求链路上下文解析机制
- MyBatis Plus 多租户、审计字段、逻辑删除、乐观锁基础配置
- RabbitMQ + Outbox 可运行骨架
- Redis 缓存与幂等键基础能力
- 附件中心基础抽象
- 各一期模块的空骨架包结构与模块扫描注册
- 一份可执行的本地开发配置模板

## 12. M0 不做的事情

M0 阶段不做：

- 不拆微服务
- 不实现完整 `01` 业务能力
- 不实现完整 Flowable 流程编排
- 不实现完整门户设计器
- 不实现生态登录深度对接
- 不实现 `03`、`05`、`07` 的完整业务能力
- 不把当前每个子模块目录都强制改造成独立部署单元

## 13. 后续衔接顺序

M0 之后建议顺序：

1. 先落 `01-组织与权限` 的基础模型与身份上下文
2. 再落 `02-流程与表单` 的流程定义、实例、待办投影
3. 再落 `04-门户与工作台` 的聚合接口与首页骨架
4. 再落 `06-消息移动与生态` 的消息中心与提醒链路

## 14. 结论

M0 的关键不是“代码写了多少”，而是要把以下三件事一次性钉死：

- **代码放在哪里**：每个业务模块的 `src/` 下，文档和代码内聚在同一模块目录
- **模块怎么分**：按 `docs/engineering/engineering-naming-map.md` 的模块前缀与子模块包分层
- **后续怎么接**：全局文档继续放 `docs/`，业务模块文档和代码继续放各模块目录，Maven 聚合构建统一管理

只要 M0 按本文档搭起来，后续 M1/M2 就能在不反复改目录、不反复改命名、不反复改边界的前提下进入实做。
