# HJO2OA
## 当前阶段

- 当前仓库处于**架构与设计收敛阶段**，核心交付物是模块边界、契约文档、领域模型和工程骨架，而不是完整业务实现。
- 现有 Java 与前端目录主要用于验证模块化单体结构、命名规则和启动装配方式，不代表各业务能力已经落地完成。

## 当前工程基线

- Java 17
- Spring Boot 3.3.x
- Maven 3.9+
- SQL Server 2017 作为主数据库
- Redis 作为缓存与会话基础设施
- RabbitMQ 作为事件总线底层消息队列
- MinIO / S3 兼容对象存储作为附件底座
- React + TypeScript 作为前端规划技术栈

## 项目定位

HJO2OA 以 O2OA 作为核心对标对象，目标不是机械复刻其现有实现，而是在对齐其协同办公、流程管理、内容管理、门户管理、数据管理、服务管理、移动办公等核心能力的基础上，建设一套更现代、更易维护、更易扩展、更适合二次开发的企业协同办公平台。

## O2OA 调研结论

- O2OA 官方公开定位为 100% 开源的企业协同办公与低代码开发平台。
- 官方公开能力重点包括流程管理平台、内容管理平台、门户管理平台、数据管理中心、服务管理平台，以及开放 API 与扩展支持。
- 官方公开内置应用覆盖公文、考勤、日程、会议、工作管理、企业网盘、论坛社区、固定资产、合同、人事、信息发布、云笔记、CRM 等业务场景。
- 官方仓库说明显示其采用分布式架构，并提供前后端 API、表单定制、页面定制和业务数据服务能力。
- 官方 GitHub 仓库展示的开源协议为 AGPL-3.0，若直接参考其源码、接口设计或二次开发方式，需要提前评估许可证合规边界。

## 本项目的复刻范围

- 组织、用户、角色、岗位、部门、租户与权限体系
- 统一登录、单点登录、身份切换与访问控制
- 流程建模、表单引擎、流程流转、待办中心与流程监控
- 内容管理、信息发布、知识库、栏目管理与检索
- 门户管理、工作台、个性化首页、仪表盘与常用入口
- 协同办公应用，包括公文、会议、日程、考勤、任务、合同、资产等能力
- 文件中心、在线预览、协作留痕、附件治理与归档
- 消息通知、站内信、邮件、短信、企业微信与钉钉等生态接入
- 数据服务、开放接口、第三方集成、报表分析与审计能力
- 移动端办公、PWA/H5、移动审批与消息触达

## 相比 O2OA 的重点优化方向

- 架构上优先采用模块化单体加领域拆分方式启动，降低早期分布式复杂度，后续按业务压力逐步拆分服务。
- 当前一期以模块化单体作为主形态，只有在满足独立扩缩容、独立发布节奏、故障隔离和清晰数据边界等条件后，才进入服务拆分评估。
- 后端技术栈升级为现代 Java 体系，强化可测试性、可观测性、容器化部署与自动化运维能力。
- 前端统一采用 React、TypeScript 和组件化设计体系，提升复杂表单、工作台和门户页面的维护效率。
- 流程、表单、内容、门户四大平台能力统一元数据模型，减少平台能力割裂。
- 建立统一搜索、统一待办、统一消息、统一附件中心，避免多个业务模块重复建设。
- 将权限、审计、日志、通知、文件、字典、组织等沉淀为平台级共享能力。
- 优先支持国产化环境、主流数据库、对象存储、缓存和消息队列，便于政企交付。
- 预留 AI 助手、知识问答、流程推荐、智能摘要、制度检索等增强能力，但不与核心事务链路强耦合。

## 当前收敛口径

- 当前仓库仍处于架构与设计收敛阶段，在补齐关键落地文档前不直接进入多人并行编码。
- `03-内容与知识` 拥有统一内容主模型和统一检索模型，`05-协同办公应用` 中的公告与文件协同场景必须优先复用 `03` 与 `00-附件中心` 的底座能力。
- `06-消息移动与生态` 负责用户触达、消息渠道、移动入口和生态登录接入；`07-数据服务与集成` 负责开放 API、连接器、数据同步、报表与服务治理。
- `04-门户与工作台` 的聚合接口仅服务门户首页和办公中心场景，属于用户侧聚合读模型，不承担通用数据开放职责。
- 多租户与组织权限必须分离：租户是系统隔离边界，组织是业务管理结构，二者不得混用。

## 文档现状

- 项目级架构、契约、实施与工程文档已收敛到 `docs/` 目录，由 `docs/README.md` 统一导航。
- `01-07` 七个业务域的子模块 `api/docs/events/frontend` 契约文档已统一到同一模板，可直接作为后续接口、事件和前端拆分输入。
- `00-HJO2OA-Infrastructure` 父模块文档已闭环，当前剩余工作重点转向顶层索引治理、统一事件契约扩充和少量历史示例口径清理。

## 文档导航

### 项目级文档

- `docs/README.md`：项目级文档总索引
- `docs/architecture/backend-architecture-spec.md`
- `docs/architecture/frontend-architecture-spec.md`
- `docs/contracts/unified-api-contract.md`
- `docs/contracts/unified-event-contract.md`
- `docs/contracts/identity-context-protocol.md`
- `docs/implementation/phase-1-implementation-slice.md`
- `docs/implementation/eventual-consistency-and-compensation.md`
- `docs/implementation/portal-aggregation-read-model.md`
- `docs/implementation/data-open-layering.md`
- `docs/decisions/architecture-convergence-decisions.md`
- `docs/decisions/adr-key-technology-selection.md`
- `docs/engineering/engineering-naming-map.md`
- `docs/engineering/m0-project-skeleton-plan.md`
- `docs/engineering/design-document-roadmap.md`
- `docs/engineering/automated-development-analysis.md`

## 自动化开发入口

仓库新增了面向自动化代理的任务清单和本地入口脚本：

- `docs/engineering/automated-development-analysis.md`：项目当前成熟度、风险和自动化开发主线分析
- `tools/auto-dev.tasks.json`：结构化任务清单，包含优先级、依赖、验收标准和建议提示词
- `tools/Invoke-AutoDev.ps1`：本地入口脚本

示例：

- 查看项目摘要：`powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command summary`
- 查看当前可执行任务：`powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command next`
- 查看任务详情：`powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command show -TaskId ORG-CORE-001`
- 输出自动化提示词：`powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command prompt -TaskId WF-TODO-001`
- 校验构建基线：`powershell -ExecutionPolicy Bypass -File tools/Invoke-AutoDev.ps1 -Command verify`

## 本地联调

`HJO2OA-Bootstrap` 现在提供了本地联调模板：

- [HJO2OA-Bootstrap/docs/local-development-template.md](/d:/idea-workspace/local/HJO2OA/HJO2OA-Bootstrap/docs/local-development-template.md)
- `HJO2OA-Bootstrap/src/main/resources/application-local.yml`
- `HJO2OA-Bootstrap/.env.local.example`
- `HJO2OA-Bootstrap/docker-compose.local.yml`

常用命令：

- 启动本地基础设施：`docker compose --env-file HJO2OA-Bootstrap/.env.local.example -f HJO2OA-Bootstrap/docker-compose.local.yml up -d`
- 以 `local` profile 启动主应用：`mvn -pl HJO2OA-Bootstrap -am spring-boot:run -Dspring-boot.run.profiles=local`
- 在无真实外部服务时验证 Bootstrap 装配：`mvn -q -pl HJO2OA-Bootstrap -am test`

### 模块级导航

| 模块 | 父模块文档目录 | 子模块契约目录 | 当前状态 |
|------|----------------|----------------|----------|
| `HJO2OA-Shared` | `HJO2OA-Shared/docs/` | - | 共享横切能力文档已闭环 |
| `HJO2OA-Bootstrap` | `HJO2OA-Bootstrap/docs/` | - | 启动装配文档已闭环 |
| `HJO2OA-Infrastructure` | `HJO2OA-Infrastructure/docs/` | `HJO2OA-Infrastructure/*/{api,docs,events,frontend}/` | 父模块文档已闭环，待做历史示例与契约口径回归 |
| `HJO2OA-OrgPerm` | `HJO2OA-OrgPerm/docs/` | `HJO2OA-OrgPerm/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |
| `HJO2OA-WorkflowForm` | `HJO2OA-WorkflowForm/docs/` | `HJO2OA-WorkflowForm/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |
| `HJO2OA-Content` | `HJO2OA-Content/docs/` | `HJO2OA-Content/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |
| `HJO2OA-Portal` | `HJO2OA-Portal/docs/` | `HJO2OA-Portal/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |
| `HJO2OA-Collaboration` | `HJO2OA-Collaboration/docs/` | `HJO2OA-Collaboration/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |
| `HJO2OA-Messaging` | `HJO2OA-Messaging/docs/` | `HJO2OA-Messaging/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |
| `HJO2OA-DataServices` | `HJO2OA-DataServices/docs/` | `HJO2OA-DataServices/*/{api,docs,events,frontend}/` | 父模块与子模块契约已统一模板 |

### 关键阅读顺序

1. 先读 `docs/README.md`、`docs/engineering/design-document-roadmap.md`、`docs/contracts/unified-event-contract.md`
2. 再读目标父模块的 `module-overview.md / domain-model.md / module-design.md / development-tasks.md`
3. 最后进入对应子模块的 `api/`、`docs/`、`events/`、`frontend/` 契约文件

### 模块文档结构

```
pom.xml                        # Maven 聚合根 POM
HJO2OA-Shared/                 # 共享横切能力 jar
HJO2OA-Bootstrap/              # Spring Boot 启动模块 jar
HJO2OA-Infrastructure/         # 业务模块父 POM
  HJO2OA-EventBus/             # 子模块 jar
  HJO2OA-I18n/                 # 子模块 jar
  ...
HJO2OA-OrgPerm/                # 业务模块父 POM
  HJO2OA-OrgStructure/         # 子模块 jar
  HJO2OA-PersonAccount/        # 子模块 jar
  ...
HJO2OA-WorkflowForm/           # 业务模块父 POM
  HJO2OA-ProcessDefinition/    # 子模块 jar
  HJO2OA-TodoCenter/           # 子模块 jar
  ...
HJO2OA-Content/                # 业务模块父 POM（二期）
HJO2OA-Portal/                 # 业务模块父 POM
HJO2OA-Collaboration/          # 业务模块父 POM（二期）
HJO2OA-Messaging/              # 业务模块父 POM
HJO2OA-DataServices/           # 业务模块父 POM（二期）
docs/                          # 全局架构、契约、实施与工程文档
frontend/                      # 前端工作区骨架
```

每个业务模块是 Maven 父 POM 聚合层（`packaging=pom`），拥有自己的 `docs/` 目录，模块级文档采用路径关联规则，如 `HJO2OA-OrgPerm/docs/domain-model.md`。

每个子模块是独立 Maven 子工程（`packaging=jar`），拥有自己的 `pom.xml` 和 `src/` 目录。子模块下保留 `api/`、`docs/`、`events/`、`frontend/` 四个契约文档目录，并采用 `<submodule>.<aspect>.md` 命名，如 `todo-center.api.md`、`todo-center.events.md`。

当前仓库已新增标准项目骨架目录：

- `docs/`：只保留全局架构、契约、实施与工程文档
- 根目录 8 个业务模块：每个模块是 Maven 父 POM，聚合其下独立子模块 jar
- `HJO2OA-Shared/`：共享横切能力 jar
- `HJO2OA-Bootstrap/`：Spring Boot 启动模块 jar，默认装配一期最小闭环子模块，并通过 Maven Profile 扩展后置与二期模块
- `frontend/`：前端工作区骨架

模块级文档约定如下：

- 父模块 `docs/`：模块定位、领域模型、模块设计、开发任务
- 子模块 `api/`：接口与能力边界契约
- 子模块 `docs/`：子模块功能概览
- 子模块 `events/`：领域事件与消费关系
- 子模块 `frontend/`：前端职责、页面与交互边界

## 推荐实施顺序

- 第一阶段聚焦 `00`、`01`、`02`、`04`、`06`，完成统一组织权限、流程表单、门户工作台和消息移动的最小平台闭环。
- 第二阶段完成 `03-内容与知识`，接入统一内容底座、统一检索和内容型发布能力。
- 第三阶段上线 `05-协同办公应用` 中的公文、会议、日程、任务、考勤、合同与资产等高频业务应用。
- 第四阶段完成 `07-数据服务与集成`，建设开放接口、连接器、数据同步、报表分析与服务治理能力，并评估候选模块的服务拆分时机。
- 第五阶段引入 AI 助手、知识问答、智能推荐和运营分析能力。

## 开发与交付原则

- 业务功能对齐 O2OA，但技术实现不必照搬历史方案。
- 平台能力优先于具体应用，先做底座，再做业务。
- 所有跨模块能力都要优先抽象为共享服务或共享组件。
- 前后端接口、权限模型、字典模型、附件模型、审计模型必须统一。
- 在进入工程搭建前，先补齐 `01` 与 `02` 领域模型、统一接口契约、统一事件契约和一期实施切片文档。
- 先满足企业协同办公主链路，再做行业化深度扩展。
- 所有核心业务均需具备审计留痕、权限校验、异常告警和操作回溯能力。

## 调研参考

- GitHub 仓库：https://github.com/o2oa/o2oa
- 官方网站：https://www.o2oa.net/
- 开发社区：https://www.o2oa.net/develop.html
- 使用手册：https://www.o2oa.net/handbook.html
- 核心能力总览：https://www.o2oa.net/core/gs.html

## 注意事项

- 若后续直接阅读、引用、改造 O2OA 源码，请务必审查 AGPL-3.0 协议要求。
- O2OA 官方公开资料明确提示正式环境不建议使用内置 H2 数据库，生产环境应选择稳定数据库并配置备份恢复方案。
- 本仓库当前首先交付的是产品与研发设计文档，后续再按任务清单逐步落地工程代码。
