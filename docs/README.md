# docs

`docs/` 目录用于承载项目级架构、契约、实施和工程启动文档。

当前仓库同时采用“项目级文档 + 模块级文档”双层组织方式：

- `docs/`：项目级架构、决策、契约、实施与工程启动文档
- `HJO2OA-*/docs/`：父模块级设计文档，如模块概览、领域模型、模块设计、开发任务
- `HJO2OA-*/*/{api,docs,events,frontend}/`：子模块契约与职责文档

## 当前状态

- `docs/` 目录已形成稳定项目级骨架，覆盖架构、契约、实施、决策和工程启动文档。
- `01-07` 七个业务域的子模块契约文档已统一到同一模板，当前重点转向索引治理与跨文档口径回归。
- `00-HJO2OA-Infrastructure`、`01-HJO2OA-OrgPerm`、`04-HJO2OA-Portal` 已完成一轮父模块事件口径回归，当前重点转向索引治理与 `06` 模块内部/外部事件边界说明。

## 目录结构

- `architecture/`
  - 总体前后端架构规范
- `contracts/`
  - 接口契约、事件契约、身份上下文协议
- `decisions/`
  - 架构收敛决策与 ADR
- `implementation/`
  - 一期实施切片、最终一致性、读模型与分层设计
- `engineering/`
  - 工程命名总表、M0 工程骨架方案、自动化开发分析
  - 前后端开发规范已迁移至 `.windsurf/rules/`（Windsurf IDE 规则，含通用约定 + 后端 + 前端三文件）

## 命名规则

- 顶层文档统一采用英文 kebab-case 文件名
- 文档路径优先表达文档类别，其次表达具体主题
- 文档引用统一使用相对路径或仓库根相对路径

## 当前索引

- `o2oa-application-layer-analysis.md`

### architecture

- `architecture/backend-architecture-spec.md`
- `architecture/frontend-architecture-spec.md`

### contracts

- `contracts/unified-api-contract.md`
- `contracts/unified-event-contract.md`
- `contracts/identity-context-protocol.md`

### decisions

- `decisions/architecture-convergence-decisions.md`
- `decisions/adr-key-technology-selection.md`

### implementation

- `implementation/phase-1-implementation-slice.md`
- `implementation/eventual-consistency-and-compensation.md`
- `implementation/portal-aggregation-read-model.md`
- `implementation/data-open-layering.md`

### engineering

- `engineering/engineering-naming-map.md`
- `engineering/m0-project-skeleton-plan.md`
- `engineering/design-document-roadmap.md`
- `engineering/automated-development-analysis.md`
- `.windsurf/rules/project-conventions.md`（项目通用约定，always_on）
- `.windsurf/rules/backend-rules.md`（后端开发规范，glob: **/*.java）
- `.windsurf/rules/frontend-rules.md`（前端开发规范，glob: frontend/**/*.{ts,tsx}）

## 模块级导航

| 模块 | 父模块文档目录 | 标准父模块文件 | 子模块契约状态 |
|------|----------------|----------------|----------------|
| `HJO2OA-Shared` | `../HJO2OA-Shared/docs/` | `module-overview.md`、`module-design.md` | 不适用 |
| `HJO2OA-Bootstrap` | `../HJO2OA-Bootstrap/docs/` | `module-overview.md`、`module-design.md` | 不适用 |
| `HJO2OA-Infrastructure` | `../HJO2OA-Infrastructure/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已完成事件口径回归 |
| `HJO2OA-OrgPerm` | `../HJO2OA-OrgPerm/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板 |
| `HJO2OA-WorkflowForm` | `../HJO2OA-WorkflowForm/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板 |
| `HJO2OA-Content` | `../HJO2OA-Content/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板 |
| `HJO2OA-Portal` | `../HJO2OA-Portal/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板 |
| `HJO2OA-Collaboration` | `../HJO2OA-Collaboration/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板 |
| `HJO2OA-Messaging` | `../HJO2OA-Messaging/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板；已区分内部/外部事件口径 |
| `HJO2OA-DataServices` | `../HJO2OA-DataServices/docs/` | `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md` | 已统一模板 |

## 模块级文档补充说明

- 各业务父模块 `docs/` 下统一维护 `module-overview.md`、`domain-model.md`、`module-design.md`、`development-tasks.md`
- 各子模块使用 `api/`、`docs/`、`events/`、`frontend/` 四类契约文件，命名采用 `<submodule>.<aspect>.md`
- 当项目级契约与父模块文档冲突时，应优先回归 `docs/contracts/` 与父模块 `module-design.md`，再同步修正子模块文档
- 若某父模块同时维护“内部领域事件”和“跨模块总线事件”，必须在父模块文档中显式区分；项目级 `docs/contracts/unified-event-contract.md` 仅登记跨模块总线事件
