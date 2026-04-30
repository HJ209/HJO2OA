# HJO2OA Codex 多窗口共享提示词

## 使用方式

- 每个 Codex 窗口先复制本文的“共享前置提示词”。
- 再复制对应工作包文档中的窗口提示词。
- Window 00 负责共享文件与最终集成，其余窗口尽量只改各自模块目录。

## 共享硬约束

- 仓库根目录：`d:\idea-workspace\local\HJO2OA`
- 后端：`Java 17` / `Spring Boot 3.3.6` / `MyBatis-Plus` / `SQL Server 2017` / `Flyway`
- 前端：`React 18` / `TypeScript` / `Vite`
- 接口契约：`docs/contracts/unified-api-contract.md`
- 事件契约：`docs/contracts/unified-event-contract.md`
- 后端规范：`.windsurf/rules/backend-rules.md`
- 前端规范：`.windsurf/rules/frontend-rules.md`
- 数据库改动必须走 Flyway migration
- 后端统一使用 `ApiResponse<T>`、`@UseSharedWebContract`、`ResponseMetaFactory`
- 前端请求必须传递 tenant、requestId、language、timezone；写操作考虑 idempotency key
- 写操作必须进入审计链路
- 时间统一 UTC 存储，前端按用户时区展示

## 多窗口冲突规避

### Window 00 独占文件

- `HJO2OA-Bootstrap/src/main/resources/application*.yml`
- `HJO2OA-Bootstrap/src/main/java/**`
- `frontend/apps/portal-web/src/app/**`
- `frontend/apps/portal-web/src/routes/**`
- `frontend/apps/portal-web/src/app/AppShellNav.tsx`

### 其他窗口原则

- 只改自己负责模块目录
- 如确实需要修改共享文件，不要直接改，在最终回复里输出“集成移交清单”
- 不要跨窗口抢同一模块目录

## Flyway 号段分配

当前最高版本号为 `V43`，后续并行开发统一分配：

| 窗口 | 号段 |
|------|------|
| Window 01 EventBus | V50-V54 |
| Window 02 Dictionary + Cache | V55-V59 |
| Window 03 Config + Tenant | V60-V64 |
| Window 04 Scheduler + Audit | V65-V69 |
| Window 05 Security + Attachment | V70-V74 |
| Window 06 I18n + Timezone + DataI18n + ErrorCode | V75-V79 |
| Window 07 Org 基础主数据 | V80-V84 |
| Window 08 Org 权限与上下文 | V85-V89 |
| Window 09 Workflow 引擎核心 | V90-V94 |
| Window 10 Form + Todo + Monitor | V95-V99 |
| Window 11 Messaging | V100-V104 |
| Window 14 Content | V105-V114 |
| Window 15 Collaboration | V115-V124 |

## 共享前置提示词

```text
你正在 HJO2OA 单仓库中工作，根目录为 d:\idea-workspace\local\HJO2OA。

你的目标不是补文档、补 DTO、补 stub，而是交付“可运行、可测试、可验证、前后端闭环”的真实实现。你必须遵守以下要求：

1. 开工前必须先读：
- docs/implementation-gap-analysis.md
- docs/contracts/unified-api-contract.md
- docs/contracts/unified-event-contract.md
- .windsurf/rules/backend-rules.md
- .windsurf/rules/frontend-rules.md
- 当前工作包所在父模块的 docs/development-tasks.md

2. 后端必须做到：
- 使用 DDD 分层：interfaces / application / domain / infrastructure
- Controller 统一 ApiResponse<T> + @UseSharedWebContract + ResponseMetaFactory
- 写接口考虑幂等、租户隔离、审计、错误码、422 业务规则失败
- 数据库变更只走 Flyway SQL Server migration
- 跨模块写联动只走事件总线
- 新接口、新事件必须符合统一契约
- 不能只做表结构或 Controller 壳子，必须有领域、应用、持久化、测试

3. 前端必须做到：
- 新功能优先落在 frontend/apps/portal-web/src
- 页面不能直接散落请求，必须走 services/
- 必须有 loading / empty / error / retry / success 状态
- 写操作要有重复提交防护和幂等键透传
- 时间按时区展示，错误提示映射后端错误码
- 不能只做静态页面或假数据页面

4. 工作方式：
- 先输出 8-15 步实施计划，按“数据库 / 后端 / 前端 / 测试 / 验证”组织
- 然后实施，不要停在分析阶段
- 若需要共享文件改动，只写入“集成移交清单”，交给 Window 00

5. 完成定义：
- 后端：模型、应用服务、Controller、Repository、Flyway、测试齐全
- 前端：页面、组件、服务、状态、测试齐全
- 最终回复必须包含：变更文件、migration、API、事件、前端页面、测试结果、风险、给 Window 00 的集成移交清单

6. 禁止偷懒：
- 禁止只做后端不做前端（如果当前工作包含前端）
- 禁止只建表不接业务逻辑
- 禁止只做 happy path
- 禁止把复杂需求简化成“先留 TODO 后续再做”
- 禁止用假数据代替真实接口，除非提示词明确允许临时模拟
```

## 统一收尾格式

```text
1. 已完成内容
2. 变更文件清单
3. 新增/修改的数据库 migration
4. 新增/修改的 API 清单
5. 新增/修改的事件清单
6. 前端页面/组件/服务清单
7. 已执行测试与结果
8. 未解决风险
9. 给 Window 00 的集成移交清单
```
