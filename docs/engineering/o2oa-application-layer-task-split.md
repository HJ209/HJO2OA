# O2OA 对标应用层并行开发任务拆分

本文面向多 Codex 窗口并行开发，目标是围绕 O2OA 的表单引擎、流程引擎和脚本能力，把 HJO2OA 当前 `WorkflowForm` 能力拆成可并行推进、写入范围相对隔离的任务单。

参考背景：

- 项目级分析：`docs/o2oa-application-layer-analysis.md`
- 当前重点模块：`HJO2OA-WorkflowForm`
- 建议新增跨域能力：`HJO2OA-Infrastructure/HJO2OA-Scripting`
- O2OA 关键对标能力：表单组件协议、表单事件脚本、流程路由脚本、人工活动脚本、已读/待阅/参阅类工作项、流程运行日志与管理视图

## 1. 并行规则

每个 Codex 窗口开始前先执行：

```powershell
git status --short --branch
```

通用约束：

- 不回滚他人改动，不使用 `git reset --hard`、`git checkout --`。
- 只改任务单列出的写入范围；发现必须跨范围修改时，先在窗口内记录原因，不直接扩散修改面。
- 后端窗口优先跑自己模块的 `mvn -q -pl <module> -am test`，不要默认跑全量。
- 前端窗口优先跑 `npm run frontend:typecheck`、`npm run frontend:test`；涉及样式时补 `npm run frontend:stylelint`。
- 涉及父 POM、新模块、共享接口的任务必须先合入，再启动依赖它的集成任务。

推荐合入顺序：

1. `DOC-O2OA-001`、`SCRIPT-CORE-001`、`FORM-META-001`、`PROC-RULE-001`、`TODO-READ-001`
2. `SCRIPT-RUNTIME-001`
3. `FORM-RENDER-001`、`FLOWABLE-HOOK-001`
4. `FE-FORM-001`、`FE-WF-DESIGN-001`
5. `E2E-O2OA-001`

## 2. 第一批：可立即并行

### DOC-O2OA-001：应用层契约与集成总控

目标：维护对标口径、接口边界和各任务合入状态，避免多个窗口各自发明协议。

写入范围：

- `docs/o2oa-application-layer-analysis.md`
- `docs/engineering/o2oa-application-layer-task-split.md`
- `HJO2OA-WorkflowForm/docs/module-design.md`
- `HJO2OA-WorkflowForm/docs/domain-model.md`
- 必要时补充各子模块 `docs/*.overview.md`

禁止修改：

- 后端 Java 源码
- 前端源码
- Maven 或 npm 构建文件

验收标准：

- 文档明确脚本能力分层：脚本定义、脚本执行、表单事件、流程事件、运行日志。
- 文档明确 O2OA 对标但不复制 AGPL 源码实现。
- 每个跨模块接口有稳定字段名和责任归属。

建议验证：

```powershell
git diff -- docs HJO2OA-WorkflowForm/docs
```

可复制提示词：

```text
你负责 DOC-O2OA-001。当前项目对标 O2OA，需要完善应用层契约文档和任务合入总控。你不独占工作区，不要回滚他人改动。写入范围仅限 docs/o2oa-application-layer-analysis.md、docs/engineering/o2oa-application-layer-task-split.md、HJO2OA-WorkflowForm/docs/module-design.md、HJO2OA-WorkflowForm/docs/domain-model.md 及必要的子模块 docs/*.overview.md。不要改 Java、前端源码、POM 或 package.json。目标是把脚本定义、脚本执行、表单事件、流程事件、运行日志的边界写清楚，并标注哪些任务依赖 SCRIPT-CORE-001。
```

### SCRIPT-CORE-001：脚本定义与版本核心模块

目标：新增平台级脚本定义模块，为表单和流程提供统一脚本资产管理，不在本任务内执行 JavaScript。

写入范围：

- `HJO2OA-Infrastructure/HJO2OA-Scripting/**`（新增）
- `HJO2OA-Infrastructure/pom.xml`
- 根 `pom.xml` 中必要的 dependencyManagement 条目
- 必要的模块级文档：`HJO2OA-Infrastructure/docs/*`

建议包职责：

- `domain/model`：`ScriptDefinition`、`ScriptVersion`、`ScriptDependency`、`ScriptScope`、`ScriptStatus`
- `application`：创建草稿、发布版本、停用、按 code/version/scope 解析依赖
- `infrastructure/persistence`：先用内存仓储或现有持久化模式；如加数据库，迁移文件必须归本任务
- `interfaces`：后台管理 API，可先提供最小 CRUD + publish

禁止修改：

- `HJO2OA-WorkflowForm/**`
- `frontend/**`
- Flowable 集成代码

验收标准：

- Maven 聚合能识别新模块。
- 脚本定义支持 tenant、code、name、scope、language、text、dependency list、version、status。
- 发布后版本不可变；草稿可修改。
- 依赖解析能检测循环依赖。
- 不引入任意脚本执行能力。

建议验证：

```powershell
mvn -q -pl HJO2OA-Infrastructure/HJO2OA-Scripting -am test
```

可复制提示词：

```text
你负责 SCRIPT-CORE-001。请新增 HJO2OA-Infrastructure/HJO2OA-Scripting 模块，实现脚本定义和版本管理核心能力，但不要执行 JavaScript。你不独占工作区，不要回滚他人改动。写入范围仅限 HJO2OA-Infrastructure/HJO2OA-Scripting/**、HJO2OA-Infrastructure/pom.xml、根 pom.xml 中必要 dependencyManagement、HJO2OA-Infrastructure/docs 中必要说明。不要改 WorkflowForm 和前端。完成后运行 mvn -q -pl HJO2OA-Infrastructure/HJO2OA-Scripting -am test。
```

### FORM-META-001：表单元数据协议升级

目标：扩展表单元数据，使其能表达 O2OA 类表单组件、事件和脚本绑定，但不在本任务内执行脚本。

写入范围：

- `HJO2OA-WorkflowForm/HJO2OA-FormMetadata/**`
- `HJO2OA-WorkflowForm/HJO2OA-FormMetadata/docs/**`
- 必要数据库迁移或测试资源

建议能力：

- 字段组件属性：`componentProps`、`dataBinding`、`mobileLayout`
- 字段事件声明：`events`，如 `onLoad`、`onChange`、`onValidate`、`onBeforeSubmit`
- 脚本引用声明：`scriptRefs`，仅保存 code/version/scope，不执行
- 选项来源：静态选项、字典选项、接口选项、脚本选项声明
- 保持旧 `fieldSchema`、`layoutSchema`、`validations`、`fieldPermissionMap` 兼容

禁止修改：

- `HJO2OA-WorkflowForm/HJO2OA-FormRenderer/**`
- `HJO2OA-WorkflowForm/HJO2OA-ProcessInstance/**`
- `frontend/**`

验收标准：

- 老表单元数据能继续发布和读取。
- 新字段协议能完整往返：创建、更新、发布、详情查询。
- DTO 和 domain 校验能拒绝重复 field code、非法事件名、非法脚本引用格式。
- 测试覆盖兼容旧协议和新协议。

建议验证：

```powershell
mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-FormMetadata -am test
```

可复制提示词：

```text
你负责 FORM-META-001。请升级 HJO2OA-WorkflowForm/HJO2OA-FormMetadata 的表单元数据协议，支持 componentProps、dataBinding、mobileLayout、events、scriptRefs、optionsProvider 等声明，但不要实现脚本执行，也不要改 FormRenderer、ProcessInstance 或前端。你不独占工作区，不要回滚他人改动。保持旧 fieldSchema/layoutSchema/validations/fieldPermissionMap 兼容。完成后运行 mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-FormMetadata -am test。
```

### PROC-RULE-001：流程路由与参与者规则表达式

目标：增强流程定义与实例运行中的路由条件、参与者规则，使其具备后续接脚本引擎的接口。

写入范围：

- `HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition/src/main/java/**/domain/model/**`
- `HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition/src/main/java/**/application/**`
- `HJO2OA-WorkflowForm/HJO2OA-ProcessInstance/src/main/java/**/application/**`
- 对应测试目录
- 对应子模块文档

建议能力：

- `WorkflowRouteCondition` 支持 `SIMPLE`、`EXPRESSION`、`SCRIPT_REF` 三类。
- `WorkflowParticipantRule` 支持固定人员、发起人、组织角色、表单字段、表达式、脚本引用的统一模型。
- `ProcessInstanceApplicationService` 中的路由匹配从只支持 `EQ/NE/PRESENT` 扩展为独立 `RouteConditionEvaluator`。
- 参与者解析从当前接口扩展上下文，不直接耦合脚本模块；可预留 `ScriptParticipantResolverPort`。

禁止修改：

- Flowable BPMN 生成器
- `HJO2OA-FormMetadata/**`
- `frontend/**`

验收标准：

- 旧流程 JSON 仍可解析和发起。
- 新 route condition 和 participant rule 能被解析、校验、单测覆盖。
- 表达式执行必须是确定性、受限能力；脚本引用在脚本模块未接入时返回清晰业务错误或走 no-op 测试替身。
- 路由选择失败、默认路由、参与者为空的错误语义保持清晰。

建议验证：

```powershell
mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition,HJO2OA-WorkflowForm/HJO2OA-ProcessInstance -am test
```

可复制提示词：

```text
你负责 PROC-RULE-001。请增强 WorkflowRouteCondition 和 WorkflowParticipantRule，使流程路由和参与者规则支持 SIMPLE、EXPRESSION、SCRIPT_REF 等类型，并在 ProcessInstance 中抽出 RouteConditionEvaluator。你不独占工作区，不要回滚他人改动。写入范围仅限 ProcessDefinition/ProcessInstance 的 domain、application、测试和文档。不要改 Flowable BPMN 生成器、FormMetadata 或前端。完成后运行 mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition,HJO2OA-WorkflowForm/HJO2OA-ProcessInstance -am test。
```

### TODO-READ-001：已读、待阅、参阅工作项模型

目标：补齐 O2OA 类 `Read/ReadCompleted/Review` 工作项，让待办中心不只覆盖待办/已办/抄送。

写入范围：

- `HJO2OA-WorkflowForm/HJO2OA-TodoCenter/**`
- 必要数据库迁移
- `HJO2OA-WorkflowForm/HJO2OA-TodoCenter/docs/**`
- 如前端已有独立 todo 类型文件，可只补类型，不改页面交互

建议能力：

- 工作项类型扩展：`TASK`、`TASK_COMPLETED`、`READ`、`READ_COMPLETED`、`REVIEW`、`CC`
- 读项状态流转：创建待阅、标记已阅、批量已阅、审阅保留
- 幂等投影：同一业务对象、接收人、类型重复事件不生成重复工作项
- 查询维度：待阅、已阅、参阅、来源流程、发起人、到达时间

禁止修改：

- `HJO2OA-ProcessInstance/**` 主流程推进逻辑
- `HJO2OA-ActionEngine/**`
- 大范围前端页面重构

验收标准：

- 原待办/已办查询不破坏。
- 待阅/已阅/参阅有独立状态和查询测试。
- 幂等键覆盖 processInstanceId + activityId + receiverId + itemType。
- API 命名与现有 TodoCenter 风格一致。

建议验证：

```powershell
mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-TodoCenter -am test
```

可复制提示词：

```text
你负责 TODO-READ-001。请在 HJO2OA-WorkflowForm/HJO2OA-TodoCenter 中补齐 READ、READ_COMPLETED、REVIEW 类工作项模型和查询能力，保持原待办/已办兼容。你不独占工作区，不要回滚他人改动。写入范围限 TodoCenter、必要数据库迁移和 TodoCenter 文档；不要改 ProcessInstance 主流程推进逻辑、ActionEngine 或大范围前端页面。完成后运行 mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-TodoCenter -am test。
```

## 3. 第二批：依赖第一批接口

### SCRIPT-RUNTIME-001：脚本执行沙箱与运行日志

依赖：`SCRIPT-CORE-001`

目标：在脚本定义模块上增加受控执行能力，为流程和表单提供统一执行口。

写入范围：

- `HJO2OA-Infrastructure/HJO2OA-Scripting/**`
- 必要根 POM dependencyManagement
- 必要模块文档

建议能力：

- `ScriptExecutionService`、`ScriptExecutionRequest`、`ScriptExecutionResult`
- `ScriptContext` 白名单：tenant、operator、formData、process、task、businessKey、now
- 超时、最大输出大小、异常捕获、运行日志
- 先支持受限表达式；如引入 GraalVM，必须封装为可替换 adapter，默认禁止 IO、线程、反射、HostAccess

禁止修改：

- 表单渲染器
- 流程实例
- Flowable
- 前端

验收标准：

- 成功、异常、超时、依赖加载、循环依赖、输出截断均有测试。
- 外部模块只依赖应用服务接口，不感知具体引擎。
- 运行日志不记录敏感字段原文，至少支持脱敏 hook。

建议验证：

```powershell
mvn -q -pl HJO2OA-Infrastructure/HJO2OA-Scripting -am test
```

可复制提示词：

```text
你负责 SCRIPT-RUNTIME-001，必须基于已合入的 SCRIPT-CORE-001。请在 HJO2OA-Infrastructure/HJO2OA-Scripting 中实现受控脚本执行接口、上下文白名单、超时、异常捕获和运行日志。你不独占工作区，不要回滚他人改动。写入范围仅限 Scripting 模块和必要 POM dependencyManagement。不要改表单、流程、Flowable 或前端。完成后运行 mvn -q -pl HJO2OA-Infrastructure/HJO2OA-Scripting -am test。
```

### FORM-RENDER-001：表单运行时联动与校验执行

依赖：`FORM-META-001`，可选依赖 `SCRIPT-RUNTIME-001`

目标：让 FormRenderer 消费升级后的表单协议，执行确定性联动、默认值、只读/必填/显隐、选项来源和校验。

写入范围：

- `HJO2OA-WorkflowForm/HJO2OA-FormRenderer/**`
- 必要的 FormRenderer 文档和测试资源
- 如已合入 Scripting，可新增对 Scripting 应用接口的依赖；否则使用本模块 port + no-op adapter

建议能力：

- `FormRuntimeContext`：tenant、operator、locale、nodeCode、formData、process variables
- 字段状态计算：visible、editable、required、defaultValue、options
- 联动规则执行顺序：默认值 -> 权限 -> 可见/只读/必填 -> 校验
- 脚本引用只通过 `ScriptExecutionService` 或本模块 port 调用
- 校验结果返回字段级错误和全局错误

禁止修改：

- `HJO2OA-FormMetadata/**` 协议定义
- `HJO2OA-ProcessInstance/**`
- 前端

验收标准：

- 旧表单渲染结果保持兼容。
- 新协议的显隐、只读、必填、默认值、选项来源可单测验证。
- 脚本执行失败不吞异常，转换为可展示错误。
- 不在 FormRenderer 内直接解析任意 JavaScript。

建议验证：

```powershell
mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-FormRenderer -am test
```

可复制提示词：

```text
你负责 FORM-RENDER-001，必须基于 FORM-META-001。请让 HJO2OA-WorkflowForm/HJO2OA-FormRenderer 消费新版表单协议，实现默认值、显隐、只读、必填、选项来源和校验的运行时计算。你不独占工作区，不要回滚他人改动。写入范围仅限 FormRenderer、测试和文档。不要改 FormMetadata 协议、ProcessInstance 或前端。如 Scripting 已合入，只通过应用服务接口调用；否则保留 port + no-op adapter。完成后运行 mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-FormRenderer -am test。
```

### FLOWABLE-HOOK-001：Flowable 生命周期 Hook 桥接

依赖：`PROC-RULE-001`，可选依赖 `SCRIPT-RUNTIME-001`

目标：在 Flowable 部署和运行阶段加入平台事件 hook，为人工活动、服务活动、路由前后脚本留出执行点。

写入范围：

- `HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition/src/main/java/**/infrastructure/flowable/**`
- `HJO2OA-WorkflowForm/HJO2OA-ProcessInstance/src/main/java/**/infrastructure/flowable/**`
- 对应测试目录
- 对应子模块文档

建议能力：

- BPMN 生成时为 userTask/serviceTask/process 增加可配置 listener。
- listener 转换为平台 hook 事件：`PROCESS_START`、`TASK_CREATE`、`TASK_COMPLETE_BEFORE`、`TASK_COMPLETE_AFTER`、`ROUTE_EVALUATE`、`PROCESS_END`
- hook 通过接口调用脚本执行服务或 no-op adapter。
- hook 失败策略可配置：阻断、记录后继续。

禁止修改：

- `ProcessInstanceApplicationService` 主业务规则，除非只接入已抽出的 port
- `HJO2OA-FormRenderer/**`
- 前端

验收标准：

- BPMN XML 测试能断言 listener 被生成。
- listener 单测能验证上下文转换、成功、失败阻断、失败继续。
- 没有脚本模块时仍可部署和运行普通流程。

建议验证：

```powershell
mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition,HJO2OA-WorkflowForm/HJO2OA-ProcessInstance -am test
```

可复制提示词：

```text
你负责 FLOWABLE-HOOK-001，必须基于 PROC-RULE-001。请在 Flowable BPMN 生成和 Flowable 运行 adapter 中加入平台生命周期 hook，覆盖 PROCESS_START、TASK_CREATE、TASK_COMPLETE_BEFORE、TASK_COMPLETE_AFTER、ROUTE_EVALUATE、PROCESS_END。你不独占工作区，不要回滚他人改动。写入范围仅限 ProcessDefinition/ProcessInstance 的 infrastructure/flowable、测试和文档。不要改 FormRenderer 或前端。没有 Scripting 时使用 no-op adapter。完成后运行 mvn -q -pl HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition,HJO2OA-WorkflowForm/HJO2OA-ProcessInstance -am test。
```

### FE-FORM-001：前端表单运行时组件壳

依赖：`FORM-META-001`，最好等待 `FORM-RENDER-001` API 稳定

目标：前端消费后端表单运行时结果，形成 O2OA 类动态表单渲染基础。

写入范围：

- `frontend/apps/portal-web/src/features/workflow/types/form.ts`
- `frontend/apps/portal-web/src/features/workflow/services/**form**`
- `frontend/apps/portal-web/src/features/workflow/components/form-runtime/**`（新增）
- `frontend/apps/portal-web/src/features/workflow/pages/form-designer-page.tsx` 中必要的小范围入口调整
- 对应测试文件

建议能力：

- 组件 registry：文本、数字、日期、选择、多选、人员、组织、附件、富文本、明细表。
- 运行时字段状态：visible、editable、required、errors、options。
- 数据代理：统一处理字段值变更、脏状态、提交 payload。
- 不在浏览器执行任意后端脚本；只消费后端计算结果。

禁止修改：

- `frontend/apps/portal-web/src/features/workflow/pages/workflow-page.tsx` 的流程设计主体
- 后端代码
- 全局 UI 基础组件，除非确有复用 bug

验收标准：

- 表单运行时组件有单元测试。
- TypeScript 类型覆盖新版元数据和运行时状态。
- `npm run frontend:typecheck`、`npm run frontend:test` 通过。
- UI 不引入营销式页面，保持当前后台工作台风格。

建议验证：

```powershell
npm run frontend:typecheck
npm run frontend:test
```

可复制提示词：

```text
你负责 FE-FORM-001。请在 frontend/apps/portal-web/src/features/workflow 中实现表单运行时组件壳，消费新版表单元数据/运行时字段状态，支持文本、数字、日期、选择、多选、人员、组织、附件、富文本、明细表等基础组件。你不独占工作区，不要回滚他人改动。写入范围仅限 workflow/types/form.ts、workflow/services 中表单相关文件、新增 workflow/components/form-runtime/**、form-designer-page.tsx 的小范围入口调整和测试。不要改 workflow-page.tsx 的流程设计主体，不要改后端。浏览器端不要执行任意后端脚本。完成后运行 npm run frontend:typecheck 和 npm run frontend:test。
```

### FE-WF-DESIGN-001：前端流程设计规则面板

依赖：`PROC-RULE-001`

目标：让流程设计界面能维护增强后的路由条件、参与者规则、活动 hook 配置。

写入范围：

- `frontend/apps/portal-web/src/features/workflow/types/workflow.ts`
- `frontend/apps/portal-web/src/features/workflow/services/**workflow**`
- `frontend/apps/portal-web/src/features/workflow/components/workflow-designer/**`（新增）
- `frontend/apps/portal-web/src/features/workflow/pages/workflow-page.tsx` 的小范围集成
- 对应测试文件

建议能力：

- 节点属性面板：活动类型、参与者规则、表单权限、hook script refs。
- 路由属性面板：条件类型、表达式、默认路由、目标节点。
- JSON 预览与校验错误展示。
- 保存时仍走现有流程定义 API。

禁止修改：

- `form-designer-page.tsx` 的表单设计主体
- 后端代码
- 表单运行时组件目录

验收标准：

- TypeScript 类型与 `PROC-RULE-001` 的 JSON 协议一致。
- 单测覆盖参与者规则、路由条件、默认路由序列化。
- `npm run frontend:typecheck`、`npm run frontend:test` 通过。

建议验证：

```powershell
npm run frontend:typecheck
npm run frontend:test
```

可复制提示词：

```text
你负责 FE-WF-DESIGN-001，必须基于 PROC-RULE-001 的流程规则协议。请在 workflow 前端中补充流程设计规则面板，支持节点参与者规则、路由条件、默认路由和 hook script refs 的配置。你不独占工作区，不要回滚他人改动。写入范围仅限 workflow/types/workflow.ts、workflow/services 中流程相关文件、新增 workflow/components/workflow-designer/**、workflow-page.tsx 小范围集成和测试。不要改 form-designer-page.tsx 的主体、不要改后端、不要改表单运行时组件目录。完成后运行 npm run frontend:typecheck 和 npm run frontend:test。
```

## 4. 第三批：集成验收

### E2E-O2OA-001：表单 + 流程 + 脚本集成样例

依赖：第一批和第二批全部关键任务

目标：做一个最小业务样例，证明表单联动、流程路由、参与者解析、生命周期 hook、待阅/参阅能串起来。

写入范围：

- 后端测试资源和集成测试
- 前端 workflow 相关测试
- `docs/o2oa-application-layer-analysis.md` 的验收记录
- 必要 seed/demo 数据

禁止修改：

- 已稳定的核心协议字段名，除非发现阻断性缺陷并同步所有依赖任务
- 与 O2OA 对标无关的模块

建议样例：

- 表单字段：申请金额、申请部门、是否加急、审批意见、附件、明细表。
- 表单联动：金额超过阈值显示财务字段；加急必填原因。
- 流程路由：金额小于阈值走部门审批，大于阈值走财务审批。
- 参与者：部门负责人、财务角色、发起人回填。
- Hook：提交前校验，任务完成后写运行日志。
- 工作项：审批人为待办，抄送人为待阅，流程结束进入参阅。

验收标准：

- 集成测试覆盖 happy path 和至少两个异常 path。
- 前端能渲染样例表单运行时状态。
- 文档记录当前与 O2OA 的剩余差距。

建议验证：

```powershell
mvn -q -pl HJO2OA-WorkflowForm -am test
npm run frontend:typecheck
npm run frontend:test
```

可复制提示词：

```text
你负责 E2E-O2OA-001，必须在 SCRIPT-CORE-001、SCRIPT-RUNTIME-001、FORM-META-001、FORM-RENDER-001、PROC-RULE-001、FLOWABLE-HOOK-001、TODO-READ-001、FE-FORM-001、FE-WF-DESIGN-001 的关键接口合入后开始。请实现一个最小集成样例和测试，覆盖表单联动、流程路由、参与者解析、生命周期 hook、待阅/参阅。你不独占工作区，不要回滚他人改动。写入范围限测试资源、集成测试、workflow 前端相关测试、docs/o2oa-application-layer-analysis.md 验收记录和必要 demo 数据。完成后运行 mvn -q -pl HJO2OA-WorkflowForm -am test、npm run frontend:typecheck、npm run frontend:test。
```

## 5. 窗口分配建议

如果一次开启 5 个窗口：

| 窗口 | 任务 | 备注 |
|------|------|------|
| A | `SCRIPT-CORE-001` | 最先合入，后续脚本运行依赖它 |
| B | `FORM-META-001` | 可独立推进，不等脚本模块 |
| C | `PROC-RULE-001` | 可独立推进，先用 port/no-op |
| D | `TODO-READ-001` | 与脚本、表单低耦合 |
| E | `DOC-O2OA-001` | 持续维护接口口径和合入状态 |

如果一次开启 8 个窗口：

| 窗口 | 任务 | 启动条件 |
|------|------|----------|
| A | `SCRIPT-CORE-001` | 立即 |
| B | `FORM-META-001` | 立即 |
| C | `PROC-RULE-001` | 立即 |
| D | `TODO-READ-001` | 立即 |
| E | `DOC-O2OA-001` | 立即 |
| F | `SCRIPT-RUNTIME-001` | 等 A 合入 |
| G | `FORM-RENDER-001` | 等 B 合入，可先读代码不写 |
| H | `FLOWABLE-HOOK-001` | 等 C 合入，可先读代码不写 |

前端两个任务建议在 B、C 接口稳定后再开，否则容易反复改类型：

- `FE-FORM-001` 等 `FORM-META-001` 和 `FORM-RENDER-001`
- `FE-WF-DESIGN-001` 等 `PROC-RULE-001`

## 6. 冲突热点

容易冲突的文件：

- 根 `pom.xml`
- `HJO2OA-Infrastructure/pom.xml`
- `HJO2OA-WorkflowForm/pom.xml`
- `frontend/apps/portal-web/src/features/workflow/types/form.ts`
- `frontend/apps/portal-web/src/features/workflow/types/workflow.ts`
- `frontend/apps/portal-web/src/features/workflow/pages/form-designer-page.tsx`
- `frontend/apps/portal-web/src/features/workflow/pages/workflow-page.tsx`
- `docs/README.md`

处理建议：

- 父 POM 只允许 `SCRIPT-CORE-001` 或明确集成窗口改。
- 前端两个窗口不要同时改同一个 page 主体；新增组件目录优先。
- 文档索引由 `DOC-O2OA-001` 或主控窗口统一合入。
- 如果必须改同一类型文件，先约定字段名，再由后合入窗口做最小 rebase 修复。

