# HJO2OA Codex 平台窗口提示词 B

> 先复制 `docs/codex-prompts-shared.md` 的共享前置提示词，再复制本文件对应窗口内容。

---

## Window 05 Security + Attachment

```text
你是 Window 05，负责 HJO2OA-Security + HJO2OA-Attachment，Flyway 号段 V70-V74。

先读：
- HJO2OA-Infrastructure/docs/development-tasks.md
- HJO2OA-Infrastructure/HJO2OA-Security/**
- HJO2OA-Infrastructure/HJO2OA-Attachment/**
- frontend/apps/portal-web/src/features/infra-admin/pages/security-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/pages/attachment-page.tsx

目标：把安全工具和附件中心从“模型层占位”升级为可以服务全平台的真实基础设施。

后端必须交付：
1. CryptoService：至少支持 AES / RSA，加密密钥不能硬编码
2. PasswordPolicyService：复杂度、有效期、历史密码、重置规则
3. MaskingRule 运行时生效：接口响应自动脱敏，而不是只存规则
4. 附件中心：小文件直传、大文件分片上传、合并、下载、删除、预览
5. StorageProvider 抽象：至少支持本地与对象存储中的一种真实实现
6. 文件版本、业务对象绑定、基础权限校验、配额检查

前端必须交付：
1. 安全管理页：脱敏规则、密码策略、启停用、效果说明
2. 附件中心页面/组件：上传、进度、失败重试、列表、预览、版本历史
3. 权限不足时明确提示，不可静默失败

测试必须覆盖：加解密、脱敏、密码策略、分片上传、前端上传或附件列表交互

禁止偷懒：
- 不得只定义接口不接真实存储
- 不得把脱敏停留在 domain model
- 不得跳过分片上传协议
```

---

## Window 06 I18n + Timezone + DataI18n + ErrorCode

```text
你是 Window 06，负责 HJO2OA-I18n + HJO2OA-Timezone + HJO2OA-DataI18n + HJO2OA-ErrorCode，Flyway 号段 V75-V79。

先读：
- HJO2OA-Infrastructure/docs/development-tasks.md
- HJO2OA-Infrastructure/HJO2OA-I18n/**
- HJO2OA-Infrastructure/HJO2OA-Timezone/**
- HJO2OA-Infrastructure/HJO2OA-DataI18n/**
- HJO2OA-Infrastructure/HJO2OA-ErrorCode/**
- frontend/apps/portal-web/src/features/infra-admin/pages/i18n-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/pages/timezone-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/pages/data-i18n-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/pages/error-code-page.tsx

目标：把国际化、时区、多语言数据、错误码治理打通，形成平台统一语言能力。

后端必须交付：
1. I18n：语言列表、默认语言、回退语言、资源包 CRUD、模块分组
2. 接入 MessageSource 或等价机制，让 Accept-Language 真正影响返回消息
3. Timezone：系统/租户/用户三级时区配置与转换工具
4. DataI18n：按 {entityId, field, locale} 存储翻译值，提供回退策略
5. ErrorCode：错误码 CRUD、级别、HTTP 状态映射、国际化文案

前端必须交付：
1. I18n 管理页：语言、资源包、键值编辑、模块筛选
2. 时区管理页：默认时区、偏好展示与设置
3. 多语言数据页：语言 Tab、多语言字段编辑
4. 错误码页：列表、筛选、详情、文案查看

测试必须覆盖：Accept-Language、时区转换、locale 回退、错误码国际化、前端至少一类页面测试

禁止偷懒：
- 不得只做管理表，不接运行时消息解析
- 不得把时区逻辑甩给前端手工计算
- 不得让 DataI18n 只停留在存储
```

---

## Window 07 Org 基础主数据

```text
你是 Window 07，负责 HJO2OA-OrgStructure + HJO2OA-PositionAssignment + HJO2OA-PersonAccount，Flyway 号段 V80-V84。

先读：
- HJO2OA-OrgPerm/docs/development-tasks.md
- HJO2OA-OrgPerm/HJO2OA-OrgStructure/**
- HJO2OA-OrgPerm/HJO2OA-PositionAssignment/**
- HJO2OA-OrgPerm/HJO2OA-PersonAccount/**

目标：搭建“组织-部门-岗位-人员-账号”主数据底座。

后端必须交付：
1. 组织/部门：CRUD、状态管理、树结构、层级移动/排序、层级合法性校验
2. 岗位/任职：岗位 CRUD、主岗唯一、兼岗、失效规则
3. 人员/账号：人员与账号分层模型、账号状态、锁定、重置、安全挂点
4. 发布组织/部门/岗位/任职相关事件

前端必须交付：
1. 组织树与部门管理页
2. 岗位与任职页
3. 人员与账号页
4. 支持筛选、状态切换、弹窗编辑、身份关系查看

测试必须覆盖：组织树校验、主岗唯一、账号状态、前端至少一类交互

禁止偷懒：
- 不得只做平铺列表，不做树结构
- 不得只存 main flag，不做主岗唯一校验
- 不得只做账号表 CRUD，不做人员-账号分层
```

---

## Window 08 Org 权限与上下文

```text
你是 Window 08，负责 HJO2OA-RoleResourceAuth + HJO2OA-DataPermission + HJO2OA-IdentityContext + HJO2OA-OrgSyncAudit，Flyway 号段 V85-V89。

先读：
- HJO2OA-OrgPerm/docs/development-tasks.md
- HJO2OA-OrgPerm/HJO2OA-RoleResourceAuth/**
- HJO2OA-OrgPerm/HJO2OA-DataPermission/**
- HJO2OA-OrgPerm/HJO2OA-IdentityContext/**
- HJO2OA-OrgPerm/HJO2OA-OrgSyncAudit/**
- frontend/apps/portal-web/src/features/identity/**

目标：把组织权限域从“有表有模型”升级为“可计算、可拦截、可切换、可追溯”的平台授权底座。

后端必须交付：
1. 角色与资源授权：角色 CRUD、岗位绑定、人员直授、权限树、权限缓存
2. 数据权限：行级权限 SQL 拦截、字段级权限隐藏/脱敏、统一 PermissionDecisionService
3. 身份上下文：切换、刷新、失效事件消费
4. 组织同步审计：同步任务、差异对账、补偿、细粒度审计

前端必须交付：
1. 角色与授权管理页
2. 数据权限配置页
3. 同步审计页
4. 现有 identity switcher 的失效提示与刷新逻辑补强

测试必须覆盖：权限计算、数据权限拦截、身份失效、同步差异、前端至少一类页面交互

禁止偷懒：
- 不得只做配置 CRUD，不做运行时判定
- 不得把数据权限留给调用方 if/else 过滤
- 不得忽略缓存失效与上下文失效
```

---

## Window 09 Workflow 引擎核心

```text
你是 Window 09，负责 HJO2OA-ProcessDefinition + HJO2OA-ProcessInstance + HJO2OA-ActionEngine，Flyway 号段 V90-V94。

先读：
- HJO2OA-WorkflowForm/docs/development-tasks.md
- HJO2OA-WorkflowForm/HJO2OA-ProcessDefinition/**
- HJO2OA-WorkflowForm/HJO2OA-ProcessInstance/**
- HJO2OA-WorkflowForm/HJO2OA-ActionEngine/**
- frontend/apps/portal-web/src 中与 workflow 相关的 features/pages（若不存在则新建）

目标：把流程域从“领域骨架”升级为真正接入 Flowable 的审批引擎核心。

后端必须交付：
1. 流程定义：CRUD、版本、发布、停用、回滚，并与 Flowable 部署打通
2. 流程实例：发起、任务创建、状态推进、归档、业务单据绑定
3. 动作引擎：提交、通过、驳回、退回、转办、撤回、催办
4. 历史轨迹与 Flowable HistoryService 打通
5. 发布流程/任务事件供待办、消息、业务联动消费

前端必须交付：
1. 流程定义管理页
2. 流程实例处理页
3. 动作区：根据节点状态展示动作，不得只有一个提交按钮

测试必须覆盖：流程发布、发起、动作执行、轨迹查询、前端至少一类交互

禁止偷懒：
- 不得只保留领域模型，不接 Flowable
- 不得只做 happy path，不处理驳回/撤回等动作
- 不得只给后端接口，不做前端处理页
```

---

## Window 10 Form + Todo + Monitor

```text
你是 Window 10，负责 HJO2OA-FormMetadata + HJO2OA-FormRenderer + HJO2OA-TodoCenter + HJO2OA-ProcessMonitor，Flyway 号段 V95-V99。

先读：
- HJO2OA-WorkflowForm/docs/development-tasks.md
- HJO2OA-WorkflowForm/HJO2OA-FormMetadata/**
- HJO2OA-WorkflowForm/HJO2OA-FormRenderer/**
- HJO2OA-WorkflowForm/HJO2OA-TodoCenter/**
- HJO2OA-WorkflowForm/HJO2OA-ProcessMonitor/**
- frontend/apps/portal-web/src 中与 form / todo / monitor 相关的 features/pages（若不存在则新建）

目标：把表单元数据、渲染协议、待办中心、流程监控做成真正可被业务使用的平台层。

后端必须交付：
1. 表单元数据：字段、布局、校验、联动、默认值、权限、字典引用、版本、节点权限映射
2. 表单渲染协议：稳定 JSON Schema / ViewModel，覆盖基础字段、字典字段、组织/人员、附件字段
3. TodoCenter：待办、已办、我发起、抄送我、草稿、归档聚合
4. ProcessMonitor：超时预警、催办、耗时分析、停滞分析

前端必须交付：
1. 表单设计器/元数据管理页
2. 表单渲染器：只读态、编辑态、审批态
3. 待办中心页面与筛选
4. 流程监控页

测试必须覆盖：表单协议、待办投影、监控计算、前端至少一类页面交互

禁止偷懒：
- 不得把表单渲染写成 hardcode 页面
- 不得只做待办列表，不做分类与筛选
- 不得只做监控占位图，不接真实接口
```

---

## Window 11 Messaging

```text
你是 Window 11，负责 HJO2OA-MessageCenter + HJO2OA-ChannelSender + HJO2OA-EventSubscription + HJO2OA-MobileSupport + HJO2OA-Ecosystem，Flyway 号段 V100-V104。

先读：
- HJO2OA-Messaging/docs/development-tasks.md
- HJO2OA-Messaging/HJO2OA-MessageCenter/**
- HJO2OA-Messaging/HJO2OA-ChannelSender/**
- HJO2OA-Messaging/HJO2OA-EventSubscription/**
- HJO2OA-Messaging/HJO2OA-MobileSupport/**
- HJO2OA-Messaging/HJO2OA-Ecosystem/**
- frontend/apps/portal-web/src/features/messages/**

目标：形成“业务事件 -> 消息生成 -> 渠道路由 -> 送达追踪 -> PC/移动端消费”的统一消息闭环。

后端必须交付：
1. MessageCenter：统一消息模型、来源模块、优先级、已读/未读、批量已读、搜索筛选
2. ChannelSender：至少落地一种真实渠道（建议邮件）+ 一种平台内渠道，支持模板渲染、失败重试、送达状态回写
3. EventSubscription：订阅偏好、静默时间、频率控制、升级策略
4. MobileSupport：移动端聚合读取接口、设备绑定/续期扩展点
5. Ecosystem：至少一种第三方生态接入骨架，支持配置、连接测试、回调校验

前端必须交付：
1. 消息中心页面
2. 渠道配置与订阅偏好页面
3. 移动端视角/窄屏可用页面
4. 生态配置页

测试必须覆盖：消息生成、模板渲染、重试、订阅偏好、前端至少一类页面交互

禁止偷懒：
- 不得只做消息表 CRUD，不做真实事件接入
- 不得只做渠道配置，不做真实发送适配器
- 不得只做 PC 列表，不考虑移动端场景
```
