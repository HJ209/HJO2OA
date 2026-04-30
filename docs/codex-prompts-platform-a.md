# HJO2OA Codex 平台窗口提示词 A

> 先复制 `docs/codex-prompts-shared.md` 的共享前置提示词，再复制本文件对应窗口内容。

---

## Window 00 总装配

```text
你是 Window 00，总装配窗口。你负责共享配置、共享路由、共享菜单、共享 Provider、统一验证，不负责深挖单一业务模块。你拥有共享文件修改权。

必须完成：
1. 收口其他窗口的集成移交项
2. 补齐 Bootstrap 共享配置：SQL Server、Flyway、OpenAPI、事件总线、审计、租户上下文、缓存等
3. 把各窗口新增 route module 接入 routes/
4. 把各窗口新增入口接入 AppShellNav 或菜单配置
5. 校验新增 Controller 是否被扫描、OpenAPI 是否可见
6. 运行阶段性总验证：相关后端 verify + npm run frontend:verify
7. 如可本地启动，抽查关键页面与关键接口

禁止偷懒：
- 不要只挂路由不验证页面
- 不要只加配置项不验证是否生效
- 不要回避共享文件冲突
```

---

## Window 01 EventBus

```text
你是 Window 01，负责 HJO2OA-EventBus，Flyway 号段 V50-V54。

先读：
- HJO2OA-Infrastructure/docs/development-tasks.md
- HJO2OA-Infrastructure/HJO2OA-EventBus/**
- docs/contracts/unified-event-contract.md
- frontend/apps/portal-web/src/features/infra-admin/pages/event-bus-page.tsx

目标：把“内存事件发布/订阅骨架”升级为“至少一次投递、可追踪、可回放、可管理”的事件总线闭环。

后端必须交付：
1. 事件注册表：事件类型注册、版本、启停用、查询
2. 可靠投递：Outbox、状态流转、失败原因、重试、死信
3. 订阅管理：精确匹配、通配符匹配、启停用、重试配置、回放开关
4. 回放与审计：事件投递记录、死信查询、按事件/时间范围回放
5. 事件信封字段符合统一事件契约

前端必须交付：
1. 事件类型注册列表
2. 订阅关系管理
3. 投递状态页
4. 死信队列页
5. 回放入口
6. 审计筛选与详情

测试必须覆盖：投递、重试、死信、回放、Controller 契约、前端至少一类页面交互

禁止偷懒：
- 不得只建 outbox 表，不做真实调度
- 不得只做后端，不补前端管理页
- 不得回放时绕过幂等
```

---

## Window 02 Dictionary + Cache

```text
你是 Window 02，负责 HJO2OA-Dictionary + HJO2OA-Cache，Flyway 号段 V55-V59。

先读：
- HJO2OA-Infrastructure/docs/development-tasks.md
- HJO2OA-Infrastructure/HJO2OA-Dictionary/**
- HJO2OA-Infrastructure/HJO2OA-Cache/**
- frontend/apps/portal-web/src/features/infra-admin/pages/dictionary-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/components/dict-type-list.tsx
- frontend/apps/portal-web/src/features/infra-admin/components/dict-item-table.tsx

目标：把“字典表结构 + CRUD 骨架”升级为真正可被业务引用的字典中心。

后端必须交付：
1. 字典类型/项完整 CRUD、层级、排序、启停用
2. 按 code 查询平铺项列表与树结构
3. `cacheable` 真正生效：DictionaryCacheService + 自动失效/刷新
4. 枚举注册机制：支持系统只读字典 + 可扩展业务字典
5. 批量导入导出：异步任务、错误明细、附件输出
6. 手动刷新字典缓存

前端必须交付：
1. 字典类型列表与字典项树/表
2. 分类筛选、租户筛选、搜索、启停用、排序调整
3. 导入导出入口与任务反馈
4. 缓存刷新入口
5. 一个通用字典选择器组件，接真实接口，不得写死选项

测试必须覆盖：缓存命中/失效、枚举注册、树查询、导入导出、字典页/选择器交互

禁止偷懒：
- 不得只存 cacheable 字段，不实现运行时缓存
- 不得把枚举注册写成一次性脚本
- 不得让导入导出变成假按钮
```

---

## Window 03 Config + Tenant

```text
你是 Window 03，负责 HJO2OA-Config + HJO2OA-Tenant，Flyway 号段 V60-V64。

先读：
- HJO2OA-Infrastructure/docs/development-tasks.md
- HJO2OA-Infrastructure/HJO2OA-Config/**
- HJO2OA-Infrastructure/HJO2OA-Tenant/**
- frontend/apps/portal-web/src/features/infra-admin/pages/config-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/pages/tenant-page.tsx

目标：把配置中心从“参数存储”升级为“运行时求值中心”，把租户模块从“档案 CRUD”升级为“真正隔离与覆盖中心”。

后端必须交付：
1. 系统参数定义与值管理
2. Feature Flag 求值：全局 / 租户 / 组织 / 角色
3. 求值优先级与回退链
4. 配置热更新：修改后刷新缓存，保留事件刷新入口
5. TenantContext 与租户拦截器，默认查询带 tenantId
6. 租户状态、套餐、配额、初始化骨架

前端必须交付：
1. 配置管理页：参数、Feature Flag、作用域可视化、最终生效值
2. 租户管理页：列表、状态、套餐、配额、初始化入口
3. 明确展示“值来源层级”与热更新反馈

测试必须覆盖：Feature Flag 求值、租户隔离、覆盖链、页面交互

禁止偷懒：
- 不得只做管理 CRUD，不做运行时求值
- 不得只加 tenantId 字段，不做真实隔离
- 不得靠 Controller 手写 tenant 条件
```

---

## Window 04 Scheduler + Audit

```text
你是 Window 04，负责 HJO2OA-Scheduler + HJO2OA-Audit，Flyway 号段 V65-V69。

先读：
- HJO2OA-Infrastructure/docs/development-tasks.md
- HJO2OA-Infrastructure/HJO2OA-Scheduler/**
- HJO2OA-Infrastructure/HJO2OA-Audit/**
- frontend/apps/portal-web/src/features/infra-admin/pages/scheduler-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/pages/audit-page.tsx
- frontend/apps/portal-web/src/features/infra-admin/components/scheduler-trigger-button.tsx
- frontend/apps/portal-web/src/features/infra-admin/components/audit-filter.tsx

目标：把“任务定义骨架 + 审计查询骨架”升级为真正可调度、可手动干预、可追溯的平台能力。

后端必须交付：
1. Quartz 或等价动态调度，不接受只用静态 @Scheduled 作为主方案
2. 任务注册、启停、Cron、时区、并发控制、超时控制
3. 执行记录：触发方式、开始结束时间、耗时、结果、错误摘要
4. 失败重试与手动触发/重跑
5. @Audited + AuditAspect 自动采集写操作
6. 审计查询：分页、过滤、资源筛选、变更前后对比

前端必须交付：
1. 调度管理页：任务列表、启停、手动触发、执行记录
2. 审计页：日志列表、筛选、变更详情、diff 展示
3. 手动触发必须有加载态、防重复点击、反馈提示

测试必须覆盖：调度执行、重试、审计切面、审计查询、触发按钮/筛选交互

禁止偷懒：
- 不得只做任务定义 CRUD，不做真实调度
- 不得把审计实现成 scattered logging
- 不得省略执行记录与失败信息
```
