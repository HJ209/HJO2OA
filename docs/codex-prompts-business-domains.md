# HJO2OA Codex 业务域窗口提示词

> 先复制 `docs/codex-prompts-shared.md` 的共享前置提示词，再复制本文件对应窗口内容。

---

## Window 12 Portal 前端落地

```text
你是 Window 12，负责 HJO2OA-Portal 全域的前端落地工作包，覆盖 PortalModel、WidgetConfig、Personalization、AggregationApi、PortalHome、PortalDesigner。不要改 Window 00 独占文件，但可以在 feature 层输出可被 Window 00 接入的 route module / menu config fragment。

先读：
- HJO2OA-Portal/docs/development-tasks.md
- HJO2OA-Portal 下各子模块后端代码与测试
- frontend/apps/portal-web/src 现有结构

目标：让 Portal 后端能力落到真正可操作的 React 前端，而不是继续停留在“页面占位 + 假卡片”。

前端必须交付：
1. Portal Home：统一工作台首页，接入待办、消息、公告/通知、快捷入口、统计卡片等真实聚合接口
2. WidgetConfig：卡片配置页、数据源配置、展示策略、启停用
3. PortalModel：模板/发布列表、模板详情、页面区域预览、发布状态查看
4. Personalization：卡片排序、常用应用、主题/布局偏好；若后端返回 422 BUSINESS_RULE_VIOLATION，前端必须正确提示
5. PortalDesigner：左侧部件区、顶部工具栏、中部画布、右侧属性区的基础可用设计器，支持草稿保存、预览、发布入口

后端协作要求：
- 若现有后端协议不足以支撑前端落地，可补充最小必要查询接口或 ViewModel，但不得破坏统一契约
- 不要擅自改 Window 00 共享路由文件，把接入说明写进集成移交清单

测试必须覆盖：Portal Home、Personalization、Designer 中至少两类交互

禁止偷懒：
- 不得只渲染假卡片或静态 mock 数据
- 不得只做一个首页，不落地配置、个性化、设计器
- 不得忽略 loading / empty / error / unauthorized 状态
```

---

## Window 13 DataServices 前端落地

```text
你是 Window 13，负责 HJO2OA-DataServices 全域的前端落地，并对已有后端进行必要补强，覆盖 DataService、OpenApi、Connector、DataSync、Report、Governance。不要改 Window 00 独占文件，但可以输出 route module / menu config fragment 给 Window 00。

先读：
- HJO2OA-DataServices/docs/development-tasks.md
- HJO2OA-DataServices 下各子模块后端代码与测试
- frontend/apps/portal-web/src 现有结构

目标：让数据服务与集成域从“后端较完整、前端缺失”升级为真正能管理、测试、执行、监控的数据平台。

前端必须交付：
1. DataService：列表、定义编辑器、参数配置、字段映射、测试调用
2. OpenApi：接口列表、凭证管理、调用日志、配额/限流信息、错误分布
3. Connector：连接器列表、参数编辑、连接测试、状态查看
4. DataSync：任务列表、启停、手工执行、差异对比、失败详情、重试
5. Report：报表分析页、时间/组织/业务维度切换、图表卡片、门户嵌入预览
6. Governance：接口版本、健康状态、异常告警、调用追踪摘要

后端协作要求：
- 若现有后端接口不足以支撑前端落地，补最小必要查询/视图接口，但不要重复实现已有逻辑
- 核查开放接口、安全、审计、调度等与 Bootstrap 装配的协作点

测试必须覆盖：至少两个业务页面的交互测试；如改后端，补对应 Controller/应用服务测试

禁止偷懒：
- 不得只做表格页面，不做编辑器、测试调用、差异查看
- 不得只做静态图表，不接真实数据
- 不得把所有页面堆在一个大组件里
```

---

## Window 14 Content 全栈启动

```text
你是 Window 14，负责 HJO2OA-Content 全域从零启动，Flyway 号段 V105-V114。不要改 Window 00 独占文件。

先读：
- HJO2OA-Content/docs/development-tasks.md
- 仓库中与 Content 相关的 architecture / module-design / domain-model 文档（若存在）
- docs/contracts/unified-api-contract.md
- docs/contracts/unified-event-contract.md

目标：从零搭建内容与知识域的第一期可运行闭环，不允许只建模块骨架。必须至少覆盖：CategoryManagement、ContentLifecycle、ContentStorage、ContentPermission；同时为 ContentSearch、ContentStatistics 预留真实扩展点。

后端必须交付：
1. CategoryManagement：栏目、分类、标签、专题完整建模与 CRUD，支持层级、排序、权限范围
2. ContentLifecycle：草稿、送审、审核、发布、下线、归档；内容版本、审核记录、发布记录
3. ContentStorage：正文、摘要、封面、附件绑定、扩展属性、版本回溯与比较
4. ContentPermission：按组织、角色、岗位、身份、用户控制可见范围
5. Search / Statistics 一期：至少建立索引刷新事件与统计刷新事件模型；若时间不足，可做最小可运行版本，但不能完全不建

前端必须交付：
1. 栏目/分类/标签管理页
2. 内容编辑页、审核页、发布页、详情页
3. 版本历史与审核记录展示
4. 发布范围/可见范围配置
5. 基础搜索入口与内容列表页
6. 基础统计页（至少阅读量/发布量/收藏量的一部分）

测试必须覆盖：生命周期、权限可见范围、版本比较、前端至少一类编辑/审核/搜索交互

禁止偷懒：
- 不得只建表和空 Controller
- 不得只做内容 CRUD，不做生命周期与权限
- 不得只做后台，不做前台编辑/浏览/审核页面
```

---

## Window 15 Collaboration 全栈启动

```text
你是 Window 15，负责 HJO2OA-Collaboration 全域从零启动，Flyway 号段 V115-V124。不要改 Window 00 独占文件。

先读：
- HJO2OA-Collaboration/docs/development-tasks.md
- docs/contracts/unified-api-contract.md
- docs/contracts/unified-event-contract.md

目标：先落地四个高频协同办公子模块的第一期闭环：DocumentMgmt、MeetingMgmt、ScheduleTask、Attendance；同时为 ContractAsset、BulletinFileshare、CrossAppLinkage 建立真实扩展基线。

后端必须交付：
1. DocumentMgmt：收文/发文/编号/核稿/签发/归档基础闭环，与流程、附件、消息保留接入点
2. MeetingMgmt：会议室、会议申请、审批状态、通知、签到、纪要基础闭环
3. ScheduleTask：个人日程、团队日程、任务分配、提醒、完成反馈
4. Attendance：请假、出差、补卡、加班基础申请闭环，班次规则和统计建立一期能力
5. ContractAsset、BulletinFileshare、CrossAppLinkage：至少建立领域边界、基础模型、事件命名与前端入口规划，不允许完全空白

前端必须交付：
1. 公文管理页：拟稿、审批、正文查看、归档查询
2. 会议管理页：会议列表、申请、详情、签到、纪要编辑
3. 日程与任务页：日历/列表、任务看板、提醒配置、反馈
4. 考勤与请假页：申请、统计、异常查看、班次配置基础页
5. 其他子模块至少提供清晰入口与非占位级骨架页

测试必须覆盖：各高频子模块至少一个核心后端用例，前端至少两个业务页面交互测试

禁止偷懒：
- 不得把四个高频模块简化成单表 CRUD
- 不得只做“申请单”而不做列表、详情、处理流转
- 不得完全忽略与附件、消息、流程的集成边界
```

---

## 建议批次

### Batch A：平台核心闭环

- Window 01
- Window 02
- Window 03
- Window 04
- Window 05
- Window 06
- Window 07
- Window 08
- Window 09
- Window 10
- Window 11

### Batch B：前端能力落地

- Window 12
- Window 13

### Batch C：新业务域启动

- Window 14
- Window 15

### Batch Z：总装配

- Window 00
