# HJO2OA 对标 O2OA 应用层能力分析

> 分析日期：2026-04-30  
> 对标范围：O2OA 表单引擎、流程引擎、脚本能力、应用层设计时/运行时闭环  
> O2OA 资料基线：官方脚本 API `10.0-149-ad24a4364c`，O2OA `develop` 源码 commit `2b6193e89c7a80859eac1254dc3aa9c12f2f1bd0`（2026-04-22）  
> 本地 O2OA 参考源码：`D:\idea-workspace\local\_reference\o2oa`

## 1. 结论摘要

O2OA 的应用层优势不只是“有流程”和“有表单”，而是形成了三层闭环：

1. **设计时元模型**：流程应用、流程、表单、脚本、数据字典、查询视图、门户页面等都是可配置资产。
2. **运行时事务模型**：流程运行中沉淀 Work、Task、TaskCompleted、Read、ReadCompleted、Review、WorkCompleted、WorkLog、Data 等运行时对象。
3. **脚本运行时**：前端表单/页面/视图脚本、后端流程/服务/定时代理脚本、脚本库依赖和上下文 API 共同构成低代码扩展能力。

HJO2OA 当前已经具备流程表单域的现代化骨架：`ProcessDefinition`、`ProcessInstance`、`ActionEngine`、`FormMetadata`、`FormRenderer`、`TodoCenter`、`ProcessMonitor` 子模块，以及 Flowable 适配、表单元数据版本化、基础渲染校验、实例/任务/动作记录。但与 O2OA 喜欢的“表单引擎 + 流程引擎 + 脚本功能”相比，核心差距在 **脚本平台和组件事件模型**，其次是设计器闭环、复杂节点能力、阅知/抄送/流转控制、表单数据中心化能力。

建议调整本项目阶段目标：不要把“复杂脚本引擎”长期暂缓，而应把它拆成 **安全表达式层、受控脚本层、完整低代码脚本层** 三个阶段逐步落地。

## 2. O2OA 应用层能力拆解

### 2.1 设计时资产模型

O2OA 流程平台以应用为容器，核心设计资产包括：

| 资产 | O2OA 源码模型 | 关键能力 |
|------|---------------|----------|
| 流程应用 | `Application` | 流程、表单、脚本、字典等资源归属容器 |
| 流程定义 | `Process` | 版本、发起模式、事件脚本、权限脚本、数据追踪、自建表同步 |
| 表单定义 | `Form` | PC 表单、移动端表单、组件 JSON、属性 JSON、图标、分类 |
| 脚本库 | `Script` / `ScriptVersion` | 脚本文本、依赖脚本列表、校验状态、版本 |
| 流程活动 | `Begin`、`Manual`、`Choice`、`Agent`、`Delay`、`Service`、`Split`、`Merge` 等 | 节点类型、人员脚本、路由、活动事件脚本 |
| 路由 | `Route` | 条件、选择、扩展附签、路由脚本 |
| 应用字典 | `ApplicationDict` | 表单脚本可读取配置数据 |

O2OA 的流程定义不是单一 JSON，而是拆成流程、活动、路由、脚本、表单等多类持久化对象。这样做的直接收益是：设计器可以围绕每类元素做细粒度配置，脚本也能挂在流程、节点、路由、表单组件等不同位置。

### 2.2 运行时对象模型

O2OA 的流程运行时围绕 `Work` 和 `Job` 展开，并拆分出多个面向场景的对象：

| 对象 | 含义 | HJO2OA 对应情况 |
|------|------|----------------|
| `Work` | 运行中的工作实例 | `ProcessInstance` 部分覆盖 |
| `Task` | 当前待办 | `TaskInstance` 部分覆盖 |
| `TaskCompleted` | 已办记录 | 由 `TaskAction`/history 间接覆盖，但缺独立视图模型 |
| `Read` / `ReadCompleted` | 待阅/已阅 | 尚未形成独立模型 |
| `Review` | 阅办/可写阅办权限 | 尚未形成独立模型 |
| `WorkCompleted` | 已完成流程归档对象 | 当前以实例状态表达，归档对象不完整 |
| `WorkLog` / `Record` | 流转树、轨迹、操作记录 | `ProcessHistoryRepository` 初步覆盖 |
| `Data` / `DataItem` | 表单绑定业务数据 | 当前以 `formDataId` 引用和 `FormSubmission` 表达，低代码数据对象不足 |

O2OA 的一个重要经验是：待办、已办、待阅、已阅、阅办、归档不是同一个表里加状态那么简单，而是面向使用场景的运行时投影。HJO2OA 当前待办中心已经有读模型方向，但还需要补齐“阅知/抄送/传阅/归档/流转树”的运行时语义。

### 2.3 表单引擎

O2OA 表单引擎的核心是“组件 + 数据 + 脚本上下文”：

| 能力 | O2OA 表现 | HJO2OA 当前情况 |
|------|-----------|----------------|
| 组件库 | Textfield、Textarea、Select、Org、Attachment、Datatable、Documenteditor、Opinion、Monitor、ReadLog、Statement、View 等大量组件 | 当前 `FormFieldType` 覆盖基础字段、表格、附件、富文本、人员组织等，但缺组件级运行时 |
| 数据对象 | 前端脚本通过 `this.data` 直接读写业务数据，支持嵌套对象、数组、数据网格 | 当前 `FormRenderer` 以 `Map<String,Object>` 渲染和校验，缺脚本可操作数据代理 |
| 表单对象 | `this.form.get()`、`getField()`、`getData()`、`save()`、`verify()`、`process()`、`startProcess()` 等 | 当前没有前端脚本 API 和组件实例 API |
| 字段脚本 | 默认值、可选值、校验、显隐、可编辑、数据网格脚本等 | 当前 `linkageRules` 是透传字段，未执行 |
| 组件事件 | DOM 原生事件 + O2 扩展事件均可配置脚本 | 当前没有事件脚本模型 |
| 移动表单 | `Form` 同时有 `data` 与 `mobileData` | 当前只有统一 layout，移动端差异尚未建模 |

O2OA 的表单 API 文档明确提供 `this.form.getField(name)`、`this.form.getData()`、`this.form.save()`、`this.form.verify()` 等脚本入口；API 总览也列出大量 FormComponent。这说明它的“表单引擎”本质上是一个浏览器端可编程运行时，而不仅是后端输出 JSON Schema。

### 2.4 流程引擎

O2OA 流程引擎有两个值得重点借鉴的点：

1. **流程活动脚本密度高**：流程属性、活动节点、路由条件、人员/待阅/阅读人、时效、参数、响应、执行等位置都能挂脚本。
2. **运行时控制动作丰富**：流转、退回、重置、撤回、加签、分支拆分、待阅、阅办、暂停/恢复、删除、催办等动作都有运行时 API 或对象支撑。

HJO2OA 已经接入 Flowable，并有 `FlowableProcessDefinitionService` 把节点/路由 JSON 转成 BPMN；`ProcessInstanceApplicationService` 支持发起、认领、转办、完成、加签、终止、挂起/恢复；`ActionEngine` 支持动作定义和幂等执行。这是正确方向。

但当前实现更像“现代审批流核心”，还不是 O2OA 式“可编程流程平台”。关键差距：

- 路由条件只支持 `EQ`、`NE`、`PRESENT`，`expression` 字段存在但能力有限。
- 节点参与者解析已有抽象，但缺用户可配置脚本和组织 API 绑定。
- Flowable BPMN 生成只覆盖基础 start/userTask/gateway/serviceTask/end，缺监听器、边界事件、任务事件、脚本服务任务等扩展。
- 流程事件脚本、节点事件脚本、路由条件脚本尚未形成统一执行链。
- 运行时 Read/Review/WorkCompleted/WorkLogTree 等对象语义不足。

### 2.5 脚本体系

O2OA 脚本体系是它低代码能力的核心。官方 API 描述中明确区分：

- 前端脚本：运行在浏览器和移动 H5，使用 JavaScript。
- 后端脚本：V9 起基于 GraalVM，兼容 ECMAScript 2021。
- 脚本使用范围：脚本库、表单/页面/视图/查询视图/导入模型组件事件、默认值、校验、流程设计、服务管理接口、定时代理、查询配置、视图列显示脚本等。

源码上，前端 `Macro` 通过构造函数并 `Browser.exec` 执行脚本，`Environment` 注入 `data`、`form`、`Dict`、`org`、`include` 等上下文；`ScriptAction` 提供脚本和依赖脚本加载接口；`Script` 模型保存脚本文本和 `dependScriptList`。

HJO2OA 当前缺少这一层。只做表达式是不够的，因为用户喜欢的是 O2OA 的“到处可挂脚本 + 脚本里可访问平台对象 + 可 include 脚本库”的扩展体验。

## 3. HJO2OA 当前能力盘点

### 3.1 已经具备的基础

| 模块 | 当前能力 |
|------|----------|
| `process-definition` | 流程定义、版本、发布/废弃、节点/路由 JSON、Flowable BPMN 部署 |
| `process-instance` | 发起、认领、转办、完成、加签、终止、挂起/恢复、事件发布、审计、基础历史 |
| `action-engine` | 动作定义、可用动作、动作执行、幂等、Flowable/ProcessInstance 网关 |
| `form-metadata` | 表单字段、布局、校验、字段权限映射、版本、发布、派生版本 |
| `form-renderer` | 字段渲染、节点字段权限、i18n 名称解析、必填/类型/范围/正则/子表校验、草稿/提交 |
| `todo-center` | 待办中心方向已有实现和测试基础 |
| 前端 workflow | 类型、服务、流程页面、表单渲染页面已有基础 |

数据库层已经有 `proc_definition`、`proc_action_def`、`form_metadata`、`proc_instance`、`proc_task`、`proc_task_action`、`wf_form_submission` 等表，这为继续扩展提供了较好的落点。

### 3.2 关键短板

| 短板 | 影响 |
|------|------|
| 缺脚本定义与运行时 | 无法复刻 O2OA 的表单事件、默认值、校验、联动、流程节点脚本 |
| 缺脚本上下文 API | 不能在脚本中自然访问 `form/data/org/dict/workflow/actions` |
| 表单组件模型偏字段化 | 只能描述字段，不能表达完整组件生命周期、事件、属性脚本、设计器行为 |
| `linkageRules` 未执行 | 表单联动停留在协议字段，无法落地复杂交互 |
| 流程节点/路由脚本不足 | 动态参与者、动态路由、节点事件、超时脚本能力弱 |
| 表单数据中心化不足 | 低代码表单需要平台托管数据；当前主要是 `formDataId` 引用和提交快照 |
| 设计器闭环不足 | 没有可视化组件属性面板、脚本编辑器、预览调试、版本发布闭环 |
| 阅知/阅办/抄送语义不足 | O2OA 的协同办公流转体验还无法完整覆盖 |
| 安全治理待设计 | 用户脚本必须有沙箱、超时、权限白名单、审计和资源限制 |

## 4. 推荐目标架构

### 4.1 新增平台级脚本能力

建议新增平台级 `HJO2OA-Scripting` 能力，放在基础设施域或共享平台域；流程、表单、门户、内容、数据服务通过适配器注入各自上下文。

核心模型建议：

| 模型 | 字段建议 |
|------|----------|
| `ScriptDefinition` | id、code、name、scope、language、version、status、source、dependencyCodes、tenantId、checksum、timeoutMs、sandboxPolicy、createdBy、updatedBy |
| `ScriptVersion` | scriptId、version、source、checksum、publishedAt、publishedBy |
| `ScriptExecutionLog` | scriptCode、version、scope、triggerType、targetId、operatorId、tenantId、durationMs、status、errorMessage、requestId |
| `ScriptPermissionPolicy` | 允许访问的上下文对象、允许调用的服务、网络/文件/数据库限制 |

执行层建议分三档：

| 阶段 | 能力 | 技术建议 |
|------|------|----------|
| S1 | 安全表达式 | Aviator/MVEL/JEXL 或自研受限表达式，仅支持条件、取值、比较 |
| S2 | 后端受控 JS | GraalVM JavaScript，禁用危险宿主访问，注入白名单 API |
| S3 | 前端组件脚本 | Web Worker/iframe sandbox/受限 DSL，注入表单组件代理和数据代理 |

### 4.2 表单元数据升级

当前 `fieldSchema + layout + validations + fieldPermissionMap` 可以继续保留，但要扩展为组件运行时协议：

```json
{
  "componentId": "leaveDays",
  "componentType": "NUMBER",
  "dataBinding": "leave.days",
  "props": {
    "label": "请假天数",
    "min": 0,
    "max": 30
  },
  "events": {
    "onChange": {"scriptCode": "leave.recalculate"}
  },
  "scripts": {
    "defaultValue": {"type": "expression", "source": "0"},
    "validation": {"type": "script", "scriptCode": "leave.validateDays"},
    "visible": {"type": "expression", "source": "data.leaveType != 'OUTING'"},
    "options": {"type": "script", "scriptCode": "leave.options"}
  },
  "permissions": {
    "draft": {"visible": true, "editable": true, "required": true},
    "approve": {"visible": true, "editable": false}
  }
}
```

需要新增的运行时接口：

- `FormDataProxy`：类似 O2OA `this.data`，支持路径读写、子表行操作、变更追踪。
- `FormComponentProxy`：类似 `this.form.get("field")`，支持取值、设值、刷新 options、显示/隐藏、启用/禁用、校验。
- `FormScriptContext`：注入 `form`、`data`、`org`、`dict`、`actions`、`session`、`workflow`。
- `FormEventRuntime`：处理 `onLoad`、`beforeSave`、`afterSave`、`beforeSubmit`、`afterSubmit`、字段事件等。

### 4.3 流程模型升级

建议在现有 `nodes/routes` JSON 中增加标准 hook：

```json
{
  "nodeId": "deptApprove",
  "type": "USER_TASK",
  "participantRule": {
    "type": "SCRIPT",
    "scriptCode": "leave.resolveDeptApprover"
  },
  "hooks": {
    "beforeEnter": "leave.beforeDeptEnter",
    "afterEnter": "leave.afterDeptEnter",
    "beforeComplete": "leave.beforeDeptComplete",
    "afterComplete": "leave.afterDeptComplete",
    "timeout": "leave.timeoutNotify"
  },
  "formPermissionProfile": "approve",
  "actionCodes": ["approve", "return", "reject", "transfer"]
}
```

路由建议支持：

```json
{
  "routeId": "toHr",
  "sourceNodeId": "deptApprove",
  "targetNodeId": "hrApprove",
  "condition": {
    "type": "SCRIPT",
    "scriptCode": "leave.routeToHr"
  }
}
```

Flowable 集成建议：

- 继续使用 Flowable 承担状态机、任务、定时器、历史能力。
- BPMN 生成时注入统一 Listener/Delegate，由 Listener 调用 HJO2OA 脚本运行时。
- HJO2OA 自有表继续保存业务语义快照，Flowable 表作为引擎事实来源之一。
- 所有脚本执行要记录 `ScriptExecutionLog`，失败策略必须可配置：阻断、跳过、走维护人、转异常队列。

### 4.4 低代码数据策略

当前模块设计强调“业务主数据归业务域所有”，这对工程边界是正确的。但如果要对齐 O2OA 的低代码体验，需要补一个平台托管数据能力：

| 场景 | 建议 |
|------|------|
| 标准业务应用 | 仍由业务域持有主数据，流程只持 `formDataId` |
| 低代码表单应用 | 提供 `wf_form_data` / `data_record` 托管 JSON 数据 |
| 查询/报表 | 通过 DataServices 输出查询视图、统计视图 |
| 表单脚本 | 通过 `FormDataProxy` 统一访问，不直接暴露表结构 |

这样可以兼顾现代领域边界和 O2OA 式快速搭建能力。

## 5. 实施路线建议

### P0：先补“可编程能力契约”

1. 新增 `HJO2OA-Scripting` 设计文档：脚本定义、版本、依赖、沙箱、执行日志、上下文白名单。
2. 修订 `02-流程与表单` 设计文档，把“复杂脚本引擎暂缓”调整为分阶段落地。
3. 明确脚本 hook 清单：表单、字段、流程、节点、路由、动作、服务、定时任务。
4. 定义脚本失败策略：阻断、跳过、默认值、维护人兜底、异常审计。

### P1：表单引擎最小闭环

1. 扩展 `FormMetadata`：组件 `events/scripts/dataBinding/optionsProvider`。
2. 实现 `FormDataProxy` 和 `FormComponentProxy`。
3. 支持表达式型默认值、显隐、必填、可编辑、选项联动。
4. 前端表单渲染器执行联动规则，并支持组件刷新。
5. 增加脚本执行日志和表单预览调试日志。

### P2：流程脚本与 Flowable Listener

1. 流程节点支持 `participantScript`、`beforeEnter`、`afterEnter`、`beforeComplete`、`afterComplete`。
2. 路由支持 `conditionScript`，并保留简单表达式作为低风险默认。
3. BPMN 生成器注入统一 Flowable Listener/Delegate。
4. 动作引擎执行前后调用脚本 hook。
5. 补齐脚本超时、错误码、审计、幂等。

### P3：设计器与脚本编辑体验

1. 表单设计器：组件区、画布、属性区、事件/脚本编辑区、预览。
2. 流程设计器：节点、路由、参与者、动作、表单权限、脚本 hook 配置。
3. 脚本库：依赖管理、版本发布、引用搜索、影响分析。
4. 调试能力：模拟 `this.data`、当前用户、组织、节点、动作，查看执行结果。

### P4：O2OA 高阶办公体验

1. 待阅/已阅、阅办、抄送、传阅、催办。
2. 意见组件、手写签批、流程图组件、流转记录组件。
3. 附件/Office/文档版本与流程动作关联。
4. 数据视图、查询语句、导入模型与表单组件联动。

## 6. 优先级差距清单

| 优先级 | 差距 | 建议任务 |
|--------|------|----------|
| P0 | 无脚本平台 | 新增脚本定义、执行、日志、安全策略文档与最小实现 |
| P0 | 表单联动不可执行 | 将 `linkageRules` 从透传变为表达式执行 |
| P0 | 路由表达式能力弱 | 扩展 `WorkflowRouteCondition.expression` 执行器 |
| P1 | 表单组件事件缺失 | 表单元数据增加 `events`，渲染器支持字段事件 |
| P1 | 动态参与者不足 | ParticipantRule 增加 `SCRIPT` 类型 |
| P1 | 脚本上下文缺失 | 注入 `data/form/org/dict/workflow/session/actions` |
| P2 | Flowable hook 未闭环 | BPMN 生成器注入统一 Listener/Delegate |
| P2 | 阅知/阅办模型不足 | 增加 Read/Review/CC 读模型和事件 |
| P2 | 表单托管数据不足 | 增加低代码表单数据表或接入 DataServices |
| P3 | 设计器体验不足 | 表单/流程/脚本设计器联动 |

## 7. 合规与工程约束

O2OA 源码采用 AGPL-3.0。HJO2OA 可以学习能力模型和公开文档思路，但不建议复制源码、前端组件实现、脚本运行时实现细节或接口代码。应采用“能力对标 + 自主建模 + 自主实现”的方式：

- 文档中可引用公开事实和能力分类。
- 工程实现保持 HJO2OA 自有命名、模型、接口和代码。
- 若后续直接移植任何 O2OA 代码，需要单独做许可证合规评估。

## 8. 参考资料

- O2OA 脚本 API：https://samplev10.o2oa.net/api/
- O2OA form API：https://www.o2oa.net/api/module-form.html
- O2OA data API：https://www.o2oa.net/api/module-data.html
- O2OA org API：https://www.o2oa.net/api/module-org.html
- O2OA GitHub：https://github.com/o2oa/o2oa
- 本地参考源码：`D:\idea-workspace\local\_reference\o2oa`

