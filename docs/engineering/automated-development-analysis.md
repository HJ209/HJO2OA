# 自动化开发分析

快照日期：`2026-04-22`

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
- 需要真实 SQL Server / Redis / RabbitMQ / MinIO 联调才能完成的端到端方案
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

## 9. 最新进展与下一轮任务

截至 `2026-04-22`，portal 这一轮已确认完成并通过关键验证的 backlog 包括：

- `PORTAL-MODEL-DRAFT-VERSIONING-001`：portal-model 草稿/已发布版本边界、并发保存校验与已发布只读约束已经稳定。
- `PORTAL-DESIGNER-PUBLICATION-LIST-001`：designer 侧发布清单查询已经闭环，管理页不再需要直连 portal-model 拼装发布数据。
- `PORTAL-IDENTITY-CONTEXT-CONSISTENCY-001`：preview、publication 与 portal-home 的 identity-context 口径与回退优先级已经收敛。
- `PORTAL-MODEL-WIDGET-REFERENCE-ENFORCEMENT-001`：portal-model 已把挂件引用状态投影收紧到 draft save / publish 写路径。
- `PORTAL-MODEL-PUBLICATION-AUDIENCE-001`：publication audience 元数据与 identity-aware 匹配逻辑已经成为稳定 source truth。
- `PORTAL-RUNTIME-AUDIENCE-CONSUMER-001`：runtime 侧也已切换到消费 publication audience source truth，`personalization` 的 base publication 绑定与 `portal-home` 的运行态页面装配不再默认按租户级唯一发布处理。
- `PORTAL-HOME-PERSONALIZATION-OVERLAY-001`：live portal-home 装配已经开始实际应用 personalization overlay，`widgetOrderOverride` 与 `hiddenPlacementCodes` 不再只是存储态偏好，而会改变最终运行态页面的顺序与显隐，并已完成对应回退语义覆盖。
- `PORTAL-DESIGNER-PREVIEW-OVERLAY-CONSISTENCY-001`：designer preview 已完成对 live personalization overlay 语义的对齐，preview 侧排序与隐藏规则不再与运行态漂移，并且已经显式说明 preview 是否应用 overlay。

这意味着 portal backlog 在 `2026-04-22` 已经进一步完成了“source audience -> runtime consumer -> live overlay application -> preview overlay consistency”的这一轮收口。接下来的重点不再是 preview/live 是否继续对齐，而是把 **personalization overlay 的 save-time validation** 前移为下一项 ready，避免非法或过期的 overlay 引用在保存阶段继续进入运行态。

同时，上一项已完成回写为：

- `PORTAL-DESIGNER-PREVIEW-OVERLAY-CONSISTENCY-001`：`done`。preview consistency 已完成，designer preview 与 live portal-home 在 overlay 排序、隐藏和应用标记上的语义已完成收敛。

因此，下一项应推进的 ready backlog 调整为：

- `PORTAL-PERSONALIZATION-OVERLAY-VALIDATION-001`：`ready`。下一轮聚焦 save-time 校验，专门处理非法 placement code、必保留卡片隐藏和跨发布错配等保存边界，把 overlay 结构性错误前移到保存入口暴露。

这样整理后，`powershell -ExecutionPolicy Bypass -File tools\Invoke-AutoDev.ps1 -Command next` 应优先返回 save-validation；它保持了“runtime audience 已完成，live overlay 已完成，preview consistency 已完成，save-time validation 进入下一轮 ready”的顺序。
