# 自动化开发分析

快照日期：`2026-04-19`

## 1. 结论摘要

- HJO2OA 当前仍处于 **M0 工程骨架 + 架构收敛** 阶段，文档成熟度明显高于业务代码成熟度。
- 根聚合工程已可通过 `mvn -q test`，说明多模块 Maven 结构、最小测试链路和启动装配方式已经可用。
- 真实代码主要集中在 `HJO2OA-Shared/`、`HJO2OA-Infrastructure/HJO2OA-EventBus/`、`HJO2OA-OrgPerm/HJO2OA-IdentityContext/`。
- `HJO2OA-Content/`、`HJO2OA-Collaboration/`、`HJO2OA-DataServices/` 目前基本仍是文档与 POM 骨架，不适合作为自动化开发的第一批主战场。
- `frontend/` 目前仅有 `apps/`、`packages/`、`public/` 三层目录骨架，尚无可运行前端应用。

因此，自动化开发应优先选择：

1. 单模块闭环明显
2. 具有稳定契约文档
3. 对外部基础设施依赖较少
4. 可以用单元测试直接验证的任务

## 2. 当前工程现实

### 2.1 仓库层次

根目录已经形成清晰的 Maven 聚合结构：

- `HJO2OA-Shared/`：共享横切能力
- `HJO2OA-Bootstrap/`：Spring Boot 启动装配
- `HJO2OA-Infrastructure/`：基础设施父模块
- `HJO2OA-OrgPerm/`：组织与权限
- `HJO2OA-WorkflowForm/`：流程与表单
- `HJO2OA-Content/`：内容与知识
- `HJO2OA-Portal/`：门户与工作台
- `HJO2OA-Collaboration/`：协同办公应用
- `HJO2OA-Messaging/`：消息移动与生态
- `HJO2OA-DataServices/`：数据服务与集成

这与 [m0-project-skeleton-plan.md](./m0-project-skeleton-plan.md) 中的模块化单体方案一致。

### 2.2 代码密度快照

按 `2026-04-19` 的扫描结果，Java 源码主要分布如下：

- `HJO2OA-Infrastructure`：约 54 个 Java 文件
- `HJO2OA-OrgPerm`：约 36 个 Java 文件
- `HJO2OA-WorkflowForm`：约 28 个 Java 文件
- `HJO2OA-Portal`：约 24 个 Java 文件
- `HJO2OA-Messaging`：约 20 个 Java 文件
- `HJO2OA-Shared`：约 10 个 Java 文件
- `HJO2OA-Bootstrap`：1 个启动类
- `HJO2OA-Content / Collaboration / DataServices`：当前尚无实际 Java 实现

但需要注意，绝大多数文件仍是 `application/domain/infrastructure/interfaces` 分层占位类，真正包含可验证业务逻辑的文件目前主要有：

- `HJO2OA-Infrastructure/HJO2OA-EventBus/src/main/java/.../SpringDomainEventPublisher.java`
- `HJO2OA-OrgPerm/HJO2OA-IdentityContext/src/main/java/.../IdentityContextRefreshApplicationService.java`
- `HJO2OA-OrgPerm/HJO2OA-IdentityContext/src/main/java/.../IdentityContextInvalidatedEvent.java`

对应测试目前也主要集中在这两个点：

- `SpringDomainEventPublisherTest`
- `IdentityContextRefreshApplicationServiceTest`

### 2.3 前端状态

前端工作区目前只有目录骨架，没有 package manager 配置、构建脚本或应用入口。因此：

- 现阶段不适合优先做复杂前端页面
- 更适合先把后端模块边界、聚合接口和事件链路骨架打牢
- 前端开发应等待门户首页、identity-context、todo-center 等后端读接口具备稳定输出后再批量推进

## 3. 自动化开发适配度判断

### 3.1 适合优先自动化的任务类型

- 单模块内的骨架补齐
- 基于现有契约文档补 REST/事件接口
- 读模型投影、内存仓储、抽象仓储等低外部依赖实现
- 共享横切能力，如统一响应体、异常处理、基础 DTO
- 可用单元测试验证的服务、事件与控制器

### 3.2 暂不建议自动化主攻的任务类型

- 需要完整主数据体系支撑的复杂写模型
- 涉及多个业务域同时落地的大功能
- 需要真实 PostgreSQL / Redis / RabbitMQ / MinIO 联调才能完成的端到端方案
- 门户设计器、内容管理、协同办公应用等大体量二期或三期能力

## 4. 推荐主线

建议将自动化开发拆为四条连续主线，而不是在所有模块同时铺开：

### 4.1 主线 A：共享横切层

目标：补齐统一 Web 响应体、异常处理、基础 Result 模型。

原因：

- 后续所有 Controller 都会依赖该层
- 风险低，验证直接
- 可以显著减少后续模块重复样板代码

建议任务：`INFRA-WEB-001`

### 4.2 主线 B：身份上下文最小闭环

目标：在 `identity-context` 中补齐 `current / available / switch / reset-primary / refresh` 的最小会话骨架。

原因：

- `identity-context` 已经有真实事件和测试
- 一期其它主线都依赖当前身份上下文
- 契约文档最完整，自动化开发约束清晰

建议任务：`ORG-CORE-001`

### 4.3 主线 C：待办投影

目标：在 `todo-center` 中建立基于 `process.task.*` 的投影和查询骨架。

原因：

- 待办是一期平台闭环的核心读模型
- 待办模型天然适合事件驱动和幂等投影
- 门户工作台和消息提醒都依赖它，但它本身不要求完整流程写模型先落地

建议任务：`WF-TODO-001`

### 4.4 主线 D：门户聚合与消息触达

目标：在 `AggregationApi` 与 `MessageCenter` 中建立仅读聚合和事件消费骨架。

原因：

- 门户和消息模块在一期主要承担“消费其它模块结果”的职责
- 等待 `identity-context` 与 `todo-center` 初步稳定后再推进，可以减少返工

建议任务：

- `PORTAL-READ-001`
- `MSG-TOUCH-001`

## 5. 推荐任务顺序

结构化任务清单已落在：

- `tools/auto-dev.tasks.json`

本地查看入口：

- `powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command summary`
- `powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command next`
- `powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command show -TaskId ORG-CORE-001`
- `powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command prompt -TaskId WF-TODO-001`
- `powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command verify`

推荐优先级：

1. `INFRA-WEB-001`
2. `ORG-CORE-001`
3. `WF-TODO-001`
4. `PORTAL-READ-001`
5. `MSG-TOUCH-001`
6. `BOOT-LOCAL-001`

## 6. 验证基线

当前最可靠的统一验证方式是：

```powershell
mvn -q test
```

对单模块增量开发，建议优先执行对应模块的局部测试，例如：

```powershell
mvn -q -pl HJO2OA-OrgPerm/HJO2OA-IdentityContext -am test
```

这比直接尝试启动完整基础设施联调更适合当前阶段，因为：

- 业务代码仍以骨架为主
- 前端尚未形成可运行应用
- 外部基础设施模板还未统一固化

## 7. 风险与注意事项

- 不要把“文档完备”误判成“业务已实现”；当前很多模块只有分层占位类。
- 不要在没有主数据真相源的情况下直接补大型写模型，否则很容易把临时实现固化成错误边界。
- 不要让门户、消息、协同应用反向拥有组织、流程等主数据。
- 优先使用内存级或抽象级实现完成最小闭环，等边界稳定后再接 Redis、RabbitMQ、数据库与对象存储。

## 8. 结论

HJO2OA 当前最适合的自动化开发策略不是“全面铺开”，而是按 **共享横切层 -> 身份上下文 -> 待办投影 -> 门户聚合 / 消息触达** 的顺序渐进推进。

只要沿着这个顺序执行，既能持续把文档收敛成果转化为代码，又不会过早落入跨模块耦合和基础设施联调泥潭。
