# HJO2OA 全功能验证分析 — 实现缺口与任务清单

> 生成时间：2026-04-29
> 分析范围：全部 10 个父模块、47 个子模块的后端实现深度 vs 设计目标
> 方法论：逐模块对照 development-tasks.md + 行业 OA 标准，盘点已有代码、缺失功能、摆设字段

---

## 一、总体评估

### 1.1 实现深度分布

| 等级 | 定义 | 子模块数 | 典型代表 |
|------|------|---------|---------|
| **A-闭环** | 领域模型+应用服务+Controller+MyBatis持久化+测试+事件 | 12 | PortalModel, PortalDesigner, Personalization, MessageCenter, IdentityContext, TodoCenter, DataService, Connector, OpenApi, DataSync, Report, Governance |
| **B-骨架** | 有领域模型+应用服务+Controller，关键功能缺失或占位 | 18 | Dictionary, Config, Cache, Audit, Security, Tenant, Scheduler, EventBus, OrgStructure, RoleResourceAuth 等 |
| **C-空壳** | 有 Java 文件但无 Controller 或无持久化 | 5 | I18n, Timezone, DataI18n, ErrorCode, Attachment |
| **D-未启动** | 0 个 Java 文件 | 12 | Content 全部 6 子模块, Collaboration 全部 7 子模块（注：CrossAppLinkage 无 Java） |

### 1.2 核心问题总结

1. **字典模块是摆设**：`cacheable` 字段仅存 DB 无缓存逻辑；无批量导入导出；无级联字典查询 API；后端 190+ 枚举/常量未与字典关联
2. **基础设施层功能颗粒度粗**：配置中心无 Feature Flag 求值器、无热更新推送；定时任务无 Quartz 集成；审计日志无 AOP 自动采集；安全模块有脱敏规则模型但无运行时脱敏拦截器
3. **多租户无隔离拦截**：有 TenantProfile CRUD 但无 MyBatis-Plus 租户拦截器、无 TenantContext 透传
4. **事件总线无可靠投递**：有 InMemory 事件发布/订阅，但无 Outbox 投递、无重试/死信队列
5. **组织权限域缺关键闭环**：OrgStructure 无组织树查询 API；RoleResourceAuth 无权限计算/缓存；DataPermission 无行级/字段级拦截器
6. **流程域缺 Flowable 集成**：ProcessDefinition/ProcessInstance 有领域模型但未接入 Flowable 引擎
7. **Content/Collaboration 全部未启动**：12 个子模块 0 行 Java 代码
8. **前端页面均为骨架**：有页面组件占位但无真实业务交互

---

## 二、00-Infrastructure 基础设施域缺口

### 00.1 EventBus 事件总线（44 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 事件发布（同步/异步） | ✅ | - | - |
| 事件订阅（精确/通配符） | ✅ | - | - |
| 事件持久化与回放 | ❌ | 缺 EventStore + 回放 API | P1 |
| 可靠投递+重试+死信 | ❌ | 缺 Outbox 表 + OutboxScheduler + 重试+死信 | P1 |
| 事件审计日志 | ❌ | 缺投递记录表+查询 API | P2 |

### 00.2 Dictionary 数据字典（21 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 字典类型 CRUD+分类+层级 | ✅ | - | - |
| 字典项 CRUD+排序+启禁用 | ✅ | - | - |
| **字典数据缓存与刷新** | ❌ | `cacheable` 字段仅存 DB；缺 DictionaryCacheService：读取时按标记走 Redis/本地缓存，监听 DictionaryTypeUpdatedEvent 刷新 | P1 |
| 批量导入导出 | ❌ | 缺 import/export API + Excel 处理 | P2 |
| 级联字典查询 | ❌ | 缺 GET /dictionaries/{code}/tree | P2 |
| **枚举/常量与字典关联** | ❌ | 190+ 枚举硬编码；缺 @DictionaryEnum 注解 + 启动时自动同步到字典表 | P1 |
| 字典选择器查询接口 | ❌ | 缺 GET /dictionaries/{code}/items | P1 |

### 00.3 Config 配置中心（35 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 系统参数定义与值管理 | ✅ | - | - |
| **Feature Flag 求值器** | ⚠️ 有事件无求值 | 缺 FeatureFlagEvaluator：按 全局/租户/组织/角色 级别运行时求值 | P1 |
| 租户级配置覆盖 | ⚠️ 有 tenantId 无逻辑 | 缺配置覆盖求值链 | P2 |
| 配置热更新 | ❌ | 缺变更事件→推送→刷新本地缓存 | P2 |
| 配置变更审计与回滚 | ❌ | 缺配置版本表+回滚 API | P3 |

### 00.4 Scheduler 定时任务（33 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| **任务注册+Cron 调度** | ⚠️ 有模型 | 缺 Quartz/ThreadPoolTaskScheduler 集成 | P1 |
| 执行记录持久化 | ⚠️ 有模型 | 缺执行记录写入逻辑 | P1 |
| **手动触发** | ❌ | 缺 POST /scheduler/tasks/{id}/trigger | P1 |
| 时区感知触发 | ❌ | 缺 CronTrigger 时区参数 | P2 |
| 失败重试策略 | ❌ | 缺 RetryPolicy | P2 |
| 任务依赖 DAG | ❌ | 缺 DAG 解析 | P3 |

### 00.5 Audit 审计日志（25 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 审计日志记录+查询 | ✅ | - | - |
| 变更前后值对比 | ✅ | - | - |
| **AOP 自动采集** | ❌ | 缺 @Audited 注解 + AuditAspect 切面自动采集操作审计 | P1 |
| 审计策略配置 | ❌ | 缺哪些接口需要审计的配置能力 | P2 |
| 日志归档与导出 | ❌ | 缺归档策略+导出 API | P3 |

### 00.6 Cache 缓存管理（30 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 缓存策略配置 | ✅ | - | - |
| **统一缓存抽象层** | ⚠️ 有 Redis 仓库 | 缺 CacheService 统一读写抽象 | P1 |
| **事件驱动缓存刷新** | ⚠️ 有事件 | 缺事件监听器→自动清除对应缓存 | P1 |
| 手动刷新接口 | ❌ | 缺 POST /cache/policies/{id}/refresh | P2 |
| 缓存监控统计 | ❌ | 缺命中率/内存统计 API | P3 |

### 00.7 Tenant 多租户（33 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 租户 CRUD+状态+套餐 | ✅ | - | - |
| **数据隔离策略** | ❌ | 缺 MyBatis-Plus TenantLineInnerInterceptor + TenantContext ThreadLocal | P1 |
| 租户级配置覆盖 | ❌ | 见 Config 模块 | P2 |
| 租户配额管理 | ⚠️ 有事件 | 缺配额检查拦截器 | P2 |
| 新租户初始化 | ⚠️ 有事件 | 缺初始化脚本/模板数据 | P2 |

### 00.8 Security 安全工具（38 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| **加解密服务** | ❌ | 缺 CryptoService（AES/RSA）+ 密钥管理 | P1 |
| **运行时脱敏拦截** | ⚠️ 有 MaskingRule 模型 | 缺响应序列化时按规则自动脱敏的拦截器 | P1 |
| **密码策略** | ❌ | 缺 PasswordPolicyService（复杂度/有效期/历史） | P1 |
| 数字签名 | ❌ | 缺 SignatureService | P3 |
| IP 白名单+频率限制 | ❌ | 缺 RateLimitFilter + IpWhitelistFilter | P2 |
| 安全审计与异常检测 | ❌ | 缺异常登录检测 | P2 |

### 00.9 ErrorCode 错误码（22 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 错误码定义 CRUD | ⚠️ 有模型 | 缺 Controller 暴露 | P2 |
| 错误消息国际化 | ❌ | 缺 ErrorCodeI18n 集成 | P2 |
| 统一异常抛出 SDK | ✅ | - | - |

### 00.10 I18n 国际化（25 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 语言列表+资源包 CRUD | ⚠️ 有模型 | 缺 Controller + Accept-Language 集成 | P2 |
| 用户语言偏好 | ❌ | 缺 UserLanguagePreference | P2 |
| 后端消息国际化 | ❌ | 缺 MessageSource 集成 | P2 |

### 00.11 Timezone 时区管理（18 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 系统默认/租户/用户时区 | ⚠️ 有模型 | 缺 Controller + 时区转换工具 | P2 |
| UTC 存储策略 | ❌ | 缺统一 UTC 规范 | P2 |

### 00.12 DataI18n 多语言数据（18 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 多语言字段元数据 | ⚠️ 有模型 | 缺 Controller | P2 |
| locale 感知查询 | ❌ | 缺 DataI18nQueryService | P2 |

### 00.13 Attachment 附件中心（34 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| **文件上传/下载/删除** | ⚠️ 有模型 | 缺 Controller + 实际存储适配（MinIO/本地） | P1 |
| **存储策略** | ❌ | 缺 StorageProvider 接口 + MinIO 适配 | P1 |
| 分片上传 | ❌ | 缺分片上传协议 | P2 |
| 文件版本管理 | ⚠️ 有模型 | 缺版本管理 API | P2 |
| 文件级权限 | ❌ | 缺附件权限校验 | P2 |

---

## 三、01-OrgPerm 组织与权限域缺口

### 01.1 OrgStructure 组织与部门管理（20 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 组织/部门 CRUD | ⚠️ 有模型 | 缺 Controller 暴露 + 组织树查询 API | P1 |
| 组织树浏览+拖拽排序 | ❌ | 缺树形结构查询 API | P1 |
| 批量启停用 | ❌ | 缺批量操作 API | P2 |
| 发布 org.organization.* 事件 | ⚠️ 有事件定义 | 缺事件发布集成 | P1 |

### 01.2 PositionAssignment 岗位与任职管理（24 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 岗位定义+CRUD | ⚠️ 有模型 | 缺 Controller | P1 |
| 主岗/兼岗约束 | ⚠️ 有模型 | 缺主岗唯一校验 + 兼岗切换逻辑 | P1 |
| 岗位角色继承 | ❌ | 缺岗位→角色→权限继承链 | P1 |

### 01.3 PersonAccount 人员与账号管理（22 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 人员档案+账号 CRUD | ⚠️ 有模型 | 缺 Controller + 登录/密码策略 | P1 |
| 账号安全策略 | ❌ | 缺锁定/重置/安全日志 | P1 |
| 外部认证预留 | ❌ | 缺 LDAP/AD/企业微信接入点 | P2 |

### 01.4 RoleResourceAuth 角色与资源授权（26 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 角色+资源权限 CRUD | ⚠️ 有模型 | 缺 Controller | P1 |
| **权限计算+缓存** | ❌ | 缺 PermissionCalculator：人员→岗位→角色→资源权限 计算链 + 缓存 | P1 |
| 角色停用/继承失效 | ❌ | 缺权限失效重算 | P1 |
| 人员直授角色 | ❌ | 缺例外授权机制 | P2 |

### 01.5 DataPermission 数据权限（28 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 行级权限模型 | ⚠️ 有模型 | 缺 Controller | P1 |
| **行级权限拦截器** | ❌ | 缺 MyBatis-Plus DataPermissionInterceptor：按策略自动追加 WHERE 条件 | P1 |
| 字段级权限模型 | ⚠️ 有模型 | 缺运行时拦截 | P1 |
| **字段级权限拦截器** | ❌ | 缺序列化时按权限隐藏/脱敏字段 | P1 |
| 统一权限决策接口 | ❌ | 缺 PermissionDecisionService 组合判定 | P1 |

### 01.6 IdentityContext 身份切换（48 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 身份切换+上下文传递 | ✅ | - | - |
| 被动失效发布 | ⚠️ 有事件 | 缺任职失效/岗位停用→身份上下文失效的事件消费 | P1 |

### 01.7 OrgSyncAudit 组织同步与审计（56 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 同步任务框架 | ⚠️ 有模型+Controller | 缺实际同步执行逻辑 | P1 |
| 差异处理+补偿 | ⚠️ 有模型 | 缺差异检测+补偿执行 | P1 |
| 同步结果审计 | ⚠️ 有模型 | 缺审计查询 API | P2 |

---

## 四、02-WorkflowForm 流程与表单域缺口

### 02.1 ProcessDefinition 流程定义（27 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 流程定义+版本管理 | ⚠️ 有模型+Controller | 缺 Flowable 集成：部署到 Flowable 引擎 | P1 |
| 流程建模+节点路由 | ❌ | 缺 BPMN 解析/生成 | P2 |

### 02.2 ProcessInstance 流程实例（34 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 流程发起+任务流转 | ⚠️ 有模型+Controller | 缺 Flowable RuntimeService 集成 | P1 |
| 审批轨迹+归档 | ⚠️ 有模型 | 缺 Flowable HistoryService 集成 | P1 |

### 02.3 ActionEngine 审批动作引擎（36 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 统一审批动作 | ⚠️ 有模型+Controller | 缺与 Flowable TaskService 集成 | P1 |
| 规则判定+动态参与者 | ❌ | 缺规则引擎 | P2 |

### 02.4 FormMetadata 表单元数据（20 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 字段/布局/校验定义 | ⚠️ 有模型+Controller | 缺字典引用协议（字段关联字典 code） | P1 |
| 表单版本管理 | ❌ | 缺版本发布机制 | P2 |
| 节点权限映射 | ❌ | 缺流程节点→表单字段权限映射 | P2 |

### 02.5 FormRenderer 表单渲染器（17 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 渲染协议输出 | ⚠️ 有 Controller | 缺渲染协议定义（前端消费的 JSON Schema） | P2 |

### 02.6 TodoCenter 待办中心（49 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 待办/已办/我发起/抄送/草稿 | ✅ | - | - |
| 事件消费刷新视图 | ⚠️ 有事件消费 | 缺完整事件消费链 | P2 |

### 02.7 ProcessMonitor 流程监控（19 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 超时预警+催办 | ⚠️ 有 Controller | 缺实际预警调度 | P2 |
| 耗时/停滞分析 | ❌ | 缺分析 API | P2 |

---

## 五、04-Portal 门户与工作台域缺口

> Portal 域整体实现较深（A-闭环），缺口主要集中在前端

### 05.1~05.6 Portal 子模块

| 子模块 | 后端状态 | 前端缺口 | 优先级 |
|--------|---------|---------|--------|
| PortalModel | ✅ 闭环 | 缺门户管理页、页面区域配置 UI | P2 |
| WidgetConfig | ✅ 闭环 | 缺卡片配置页、数据源配置 UI | P2 |
| Personalization | ✅ 闭环 | 缺个性化设置页 UI | P2 |
| AggregationApi | ✅ 闭环 | 缺聚合数据展示组件 | P2 |
| PortalHome | ✅ 闭环 | 缺统一工作台首页 UI | P2 |
| PortalDesigner | ✅ 闭环 | 缺门户设计器 UI（左侧组件区+画布+属性区） | P3 |

---

## 六、06-Messaging 消息与生态域缺口

### 06.1 MessageCenter 统一消息中心（48 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 统一消息模型+CRUD | ✅ | - | - |
| 消息优先级+已读状态 | ✅ | - | - |
| 失败重试 | ⚠️ 有模型 | 缺重试调度 | P2 |

### 06.2 ChannelSender 多渠道发送（22 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 渠道模型+路由 | ⚠️ 有模型+Controller | 缺实际渠道适配器（邮件/短信/企微/钉钉） | P1 |
| 发送记录+送达状态 | ⚠️ 有模型 | 缺状态回调处理 | P2 |

### 06.3 EventSubscription 待办提醒与事件订阅（20 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 事件接入机制 | ⚠️ 有模型 | 缺实际业务事件→消息生成逻辑 | P1 |
| 订阅偏好+静默规则 | ❌ | 缺偏好管理 API | P2 |

### 06.4 MobileSupport 移动端接入（18 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 移动端聚合接口 | ⚠️ 有模型 | 缺轻量接口实现 | P2 |
| 设备绑定+安全 | ❌ | 缺 DeviceBinding + 安全机制 | P2 |

### 06.5 Ecosystem 第三方生态（22 文件）

| 设计目标 | 当前 | 缺口 | 优先级 |
|---------|------|------|--------|
| 生态接入框架 | ⚠️ 有模型 | 缺实际适配器（企微/钉钉/SSO/LDAP） | P2 |
| 回调验证+异常日志 | ❌ | 缺回调处理 | P2 |

---

## 七、07-DataServices 数据服务与集成域缺口

> DataServices 域后端 A-闭环，缺口在运行时集成和前端

### 07.1~07.6 DataServices 子模块

| 子模块 | 后端状态 | 缺口 | 优先级 |
|--------|---------|------|--------|
| DataService | ✅ 闭环 | 缺前端服务定义编辑器 | P2 |
| OpenApi | ✅ 闭环 | 缺前端接口列表页+凭证管理页 | P2 |
| Connector | ✅ 闭环 | 缺前端连接器管理页+连接测试 | P2 |
| DataSync | ✅ 闭环 | 缺前端同步任务管理页+差异对比 | P2 |
| Report | ✅ 闭环 | 缺前端分析页面+门户图卡嵌入 | P2 |
| Governance | ✅ 闭环 | 缺前端健康状态页+告警列表页 | P2 |

---

## 八、03-Content 内容与知识域 — 全部 D-未启动

| 子模块 | Java 文件 | 需从零实现 | 优先级 |
|--------|----------|-----------|--------|
| CategoryManagement | 0 | 栏目/分类/标签/专题模型+CRUD+权限+事件 | P2 |
| ContentLifecycle | 0 | 内容/版本/审核/发布模型+生命周期+流程接入 | P2 |
| ContentStorage | 0 | 正文/摘要/封面/附件存储+版本回溯 | P2 |
| ContentPermission | 0 | 按 组织/角色/岗位/身份/用户 可见范围规则 | P2 |
| ContentSearch | 0 | 全文检索索引+搜索 API+收藏/订阅 | P3 |
| ContentStatistics | 0 | 阅读/下载/收藏/热度/排行统计 | P3 |

---

## 九、05-Collaboration 协同办公域 — 全部 D-未启动

| 子模块 | Java 文件 | 需从零实现 | 优先级 |
|--------|----------|-----------|--------|
| DocumentMgmt | 0 | 收文/发文/编号/核稿/签发/归档+流程接入 | P2 |
| MeetingMgmt | 0 | 会议室/申请/审批/通知/签到/纪要 | P2 |
| ScheduleTask | 0 | 个人/团队日程+任务分配+提醒 | P2 |
| Attendance | 0 | 请假/出差/补卡/加班+班次规则+统计 | P2 |
| ContractAsset | 0 | 合同台账/到期提醒+资产领用/归还/盘点/报废 | P3 |
| BulletinFileshare | 0 | 公告+共享空间+文件预览 | P3 |
| CrossAppLinkage | 0 | 跨应用联动+统一体验规范 | P3 |

---

## 十、P1 优先任务清单（必须优先闭环）

> P1 = 阻塞其他模块或平台核心能力的关键缺口，共 42 项

### 10.1 基础设施域 P1（18 项）

| # | 模块 | 任务 | 依赖 |
|---|------|------|------|
| 1 | EventBus | 实现 Outbox 表 + OutboxScheduler 可靠投递 | Flyway V34 已有 outbox 表 |
| 2 | EventBus | 实现重试策略 + 死信队列 | 依赖 #1 |
| 3 | EventBus | 实现 EventStore 持久化 + 回放 API | 新增 Flyway migration |
| 4 | Dictionary | 实现 DictionaryCacheService：cacheable 标记→Redis/本地缓存+事件刷新 | 依赖 Cache 模块 |
| 5 | Dictionary | 实现 @DictionaryEnum 注解 + 启动时枚举自动注册为字典项 | 无外部依赖 |
| 6 | Dictionary | 实现 GET /dictionaries/{code}/items 字典选择器查询接口 | 无外部依赖 |
| 7 | Dictionary | 实现 GET /dictionaries/{code}/tree 级联字典查询接口 | 无外部依赖 |
| 8 | Config | 实现 FeatureFlagEvaluator：全局/租户/组织/角色级运行时求值 | 无外部依赖 |
| 9 | Scheduler | 集成 Quartz/ThreadPoolTaskScheduler 动态任务调度 | Flyway V18 已有表 |
| 10 | Scheduler | 实现执行记录写入逻辑 | 依赖 #9 |
| 11 | Scheduler | 实现手动触发 API POST /scheduler/tasks/{id}/trigger | 依赖 #9 |
| 12 | Audit | 实现 @Audited 注解 + AuditAspect AOP 自动采集操作审计 | 无外部依赖 |
| 13 | Cache | 实现 CacheService 统一缓存读写抽象 | 无外部依赖 |
| 14 | Cache | 实现事件监听器→自动清除对应缓存 | 依赖 #13 |
| 15 | Tenant | 实现 MyBatis-Plus TenantLineInnerInterceptor + TenantContext ThreadLocal | 无外部依赖 |
| 16 | Security | 实现 CryptoService（AES/RSA）+ 密钥管理 | 无外部依赖 |
| 17 | Security | 实现运行时脱敏拦截器：响应序列化时按 MaskingRule 自动脱敏 | 无外部依赖 |
| 18 | Security | 实现 PasswordPolicyService（复杂度/有效期/历史检查） | 无外部依赖 |
| 19 | Attachment | 实现 AttachmentController + StorageProvider 接口 + MinIO/本地适配 | Flyway V19 已有表 |

### 10.2 组织权限域 P1（14 项）

| # | 模块 | 任务 | 依赖 |
|---|------|------|------|
| 20 | OrgStructure | 实现 Controller 暴露 + 组织树查询 API | Flyway V21 已有表 |
| 21 | OrgStructure | 实现 org.organization.* 事件发布集成 | 依赖 EventBus |
| 22 | PositionAssignment | 实现 Controller | Flyway V23 已有表 |
| 23 | PositionAssignment | 实现主岗唯一校验 + 兼岗切换逻辑 | 依赖 #22 |
| 24 | PositionAssignment | 实现岗位→角色→权限继承链 | 依赖 RoleResourceAuth |
| 25 | PersonAccount | 实现 Controller + 登录/密码策略 | Flyway V22 已有表 |
| 26 | PersonAccount | 实现账号锁定/重置/安全日志 | 依赖 #25 |
| 27 | RoleResourceAuth | 实现 Controller | Flyway V24 已有表 |
| 28 | RoleResourceAuth | 实现 PermissionCalculator：人员→岗位→角色→资源权限 计算链 + 缓存 | 依赖 #24 |
| 29 | RoleResourceAuth | 实现角色停用/继承失效→权限重算 | 依赖 #28 |
| 30 | DataPermission | 实现 Controller | Flyway V25 已有表 |
| 31 | DataPermission | 实现行级权限 MyBatis-Plus 拦截器 | 依赖 #28 |
| 32 | DataPermission | 实现字段级权限序列化拦截器 | 依赖 #28 |
| 33 | DataPermission | 实现 PermissionDecisionService 组合判定 | 依赖 #31 + #32 |
| 34 | IdentityContext | 实现任职失效/岗位停用→身份上下文失效事件消费 | 依赖 EventBus |

### 10.3 流程表单域 P1（5 项）

| # | 模块 | 任务 | 依赖 |
|---|------|------|------|
| 35 | ProcessDefinition | 集成 Flowable：部署流程定义到引擎 | Bootstrap 已引入 flowable-spring-boot-starter |
| 36 | ProcessInstance | 集成 Flowable RuntimeService：发起+流转 | 依赖 #35 |
| 37 | ProcessInstance | 集成 Flowable HistoryService：审批轨迹+归档 | 依赖 #35 |
| 38 | ActionEngine | 集成 Flowable TaskService：审批动作执行 | 依赖 #35 |
| 39 | FormMetadata | 实现字典引用协议（字段关联字典 code） | 依赖 Dictionary #6 |

### 10.4 消息生态域 P1（2 项）

| # | 模块 | 任务 | 依赖 |
|---|------|------|------|
| 40 | ChannelSender | 实现邮件渠道适配器（JavaMail） | 无外部依赖 |
| 41 | EventSubscription | 实现业务事件→消息生成逻辑 | 依赖 EventBus |

---

## 十一、P2 优先任务清单（功能完善）

> P2 = 功能完善但不阻塞核心流程，共 35+ 项

### 基础设施域 P2

| 模块 | 任务 |
|------|------|
| EventBus | 事件审计日志查询 API |
| Dictionary | 批量导入导出 API + Excel 处理 |
| Config | 租户级配置覆盖求值链 |
| Config | 配置热更新推送 + 本地缓存刷新 |
| Scheduler | 时区感知 CronTrigger |
| Scheduler | 失败重试策略 RetryPolicy |
| Audit | 审计策略配置 |
| Cache | 手动刷新接口 POST /cache/policies/{id}/refresh |
| Tenant | 租户级配置覆盖（与 Config 联动） |
| Tenant | 租户配额检查拦截器 |
| Tenant | 新租户初始化脚本/模板数据 |
| Security | IP 白名单 + 频率限制 Filter |
| Security | 安全审计与异常登录检测 |
| ErrorCode | Controller 暴露 + 错误消息国际化 |
| I18n | Controller + Accept-Language 集成 + MessageSource |
| Timezone | Controller + 时区转换工具 |
| DataI18n | Controller + locale 感知查询 |
| Attachment | 分片上传 + 版本管理 API + 文件级权限 |

### 组织权限域 P2

| 模块 | 任务 |
|------|------|
| OrgStructure | 批量启停用 API |
| PersonAccount | LDAP/AD/企业微信认证接入点 |
| RoleResourceAuth | 人员直授角色（例外授权） |
| OrgSyncAudit | 同步结果审计查询 API |

### 流程表单域 P2

| 模块 | 任务 |
|------|------|
| ProcessDefinition | BPMN 解析/生成 |
| ActionEngine | 规则判定引擎 + 动态参与者计算 |
| FormMetadata | 版本发布机制 + 节点权限映射 |
| FormRenderer | 渲染协议 JSON Schema 定义 |
| TodoCenter | 完整事件消费链 |
| ProcessMonitor | 实际预警调度 + 耗时/停滞分析 API |

### 消息生态域 P2

| 模块 | 任务 |
|------|------|
| MessageCenter | 失败重试调度 |
| ChannelSender | 企微/钉钉适配器 + 状态回调 |
| EventSubscription | 订阅偏好管理 API |
| MobileSupport | 轻量接口实现 + 设备绑定 |
| Ecosystem | 企微/钉钉/SSO/LDAP 适配器 |

### Content 域 P2（全部从零启动）

| 模块 | 任务 |
|------|------|
| CategoryManagement | 栏目/分类/标签/专题 完整闭环 |
| ContentLifecycle | 内容生命周期 + 流程接入 |
| ContentStorage | 正文存储 + 版本回溯 |
| ContentPermission | 可见范围规则 |

### Collaboration 域 P2

| 模块 | 任务 |
|------|------|
| DocumentMgmt | 公文管理完整闭环 |
| MeetingMgmt | 会议管理完整闭环 |
| ScheduleTask | 日程与任务完整闭环 |
| Attendance | 考勤与请假完整闭环 |

### 前端 P2

| 模块 | 任务 |
|------|------|
| Portal 全部 | 门户管理页、卡片配置页、个性化设置页、工作台首页 UI |
| DataServices 全部 | 服务定义编辑器、接口列表页、连接器管理页、同步任务页、分析页、治理页 |
| Infra-admin 全部 | 字典管理页真实交互、配置管理页、调度管理页、审计查询页 |

---

## 十二、P3 优先任务清单（增强与优化）

| 模块 | 任务 |
|------|------|
| Config | 配置变更审计与回滚 |
| Scheduler | 任务依赖 DAG |
| Audit | 日志归档与导出 |
| Cache | 缓存监控统计 API |
| Security | 数字签名与验证 SignatureService |
| ErrorCode | 错误码文档生成 + 错误频率统计 |
| DataI18n | 翻译状态管理 + 翻译工作台 |
| ContentSearch | 全文检索 + 搜索 API + 收藏/订阅 |
| ContentStatistics | 阅读/下载/收藏/热度/排行统计 |
| ContractAsset | 合同台账 + 资产管理完整闭环 |
| BulletinFileshare | 公告 + 共享空间 + 文件预览 |
| CrossAppLinkage | 跨应用联动 + 统一体验规范 |
| PortalDesigner | 门户设计器 UI |

---

## 十三、建议执行路线图

### Phase 1：基础设施闭环（预计 3-4 周）

**目标**：让基础设施域从"骨架"升级为"可运行"，支撑上层业务模块

1. **Tenant 隔离** → #15 TenantLineInnerInterceptor（所有模块依赖租户隔离）
2. **EventBus 可靠投递** → #1~#3 Outbox + 重试 + 死信 + 持久化
3. **Dictionary 字典激活** → #4~#7 缓存 + 枚举注册 + 选择器 API + 级联查询
4. **Audit AOP** → #12 @Audited + AuditAspect
5. **Cache 统一抽象** → #13~#14 CacheService + 事件刷新
6. **Security 核心** → #16~#18 加解密 + 脱敏拦截 + 密码策略
7. **Attachment 上传** → #19 Controller + StorageProvider
8. **Config FeatureFlag** → #8 FeatureFlagEvaluator
9. **Scheduler 调度** → #9~#11 Quartz 集成 + 执行记录 + 手动触发

### Phase 2：组织权限闭环（预计 3-4 周）

**目标**：组织权限域从"骨架"升级为"可授权"，支撑流程和业务模块

1. **OrgStructure Controller + 树查询** → #20~#21
2. **PositionAssignment Controller + 主岗/兼岗** → #22~#24
3. **PersonAccount Controller + 安全** → #25~#26
4. **RoleResourceAuth Controller + 权限计算** → #27~#29
5. **DataPermission 拦截器** → #30~#33
6. **IdentityContext 事件消费** → #34

### Phase 3：流程引擎闭环（预计 2-3 周）

**目标**：流程域接入 Flowable，实现端到端审批

1. **ProcessDefinition Flowable 集成** → #35
2. **ProcessInstance Flowable 集成** → #36~#37
3. **ActionEngine Flowable 集成** → #38
4. **FormMetadata 字典引用** → #39

### Phase 4：消息与生态闭环（预计 2 周）

1. **ChannelSender 邮件适配器** → #40
2. **EventSubscription 消息生成** → #41

### Phase 5：Content + Collaboration 启动（预计 4-6 周）

1. Content 域 4 个 P2 子模块从零实现
2. Collaboration 域 4 个 P2 子模块从零实现

### Phase 6：前端全面落地（持续）

1. 基础设施管理页真实交互
2. 组织权限管理页
3. 门户工作台 UI
4. 流程设计器 + 表单设计器
5. 消息中心 + 待办中心 UI
6. Content/Collaboration 业务页

---

## 十四、关键设计决策待定

| # | 决策点 | 影响 | 建议 |
|---|--------|------|------|
| 1 | 枚举是否全部注册为字典项？ | Dictionary #5 | 系统枚举（如 DictionaryStatus）注册为只读字典；业务枚举（如请假类型）注册为可编辑字典 |
| 2 | 缓存选型：Redis vs 本地 Caffeine | Dictionary #4, Cache #13 | cacheable=true 且数据量大的走 Redis；cacheable=true 且高频访问的走本地 Caffeine + 事件刷新 |
| 3 | 租户隔离策略：共享DB+tenant_id vs 独立DB | Tenant #15 | 一期采用共享DB+tenant_id（TenantLineInnerInterceptor），预留独立DB扩展点 |
| 4 | Flowable 集成深度：直接用 vs 领域适配 | ProcessDefinition #35 | 领域模型保持独立，Flowable 作为基础设施适配器，通过 Repository/Service 桥接 |
| 5 | 脱敏拦截层级：Jackson 序列化 vs AOP | Security #17 | 优先 Jackson JsonSerializer 适配器，按 MaskingRule 在序列化时脱敏 |
| 6 | 行级权限实现：SQL 拦截 vs 领域过滤 | DataPermission #31 | MyBatis-Plus DataPermissionInterceptor 追加 WHERE 条件，性能优于领域层过滤 |
