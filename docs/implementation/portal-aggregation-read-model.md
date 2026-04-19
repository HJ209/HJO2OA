# 门户聚合读模型文档

对应架构决策：D15（门户聚合策略）、D05（`04` 与 `07` 数据边界）。

## 1. 文档目的

定义 `04-门户与工作台` 聚合数据接口的读模型设计规范，包括聚合范围、数据来源、缓存策略、刷新机制和性能要求。明确门户聚合与通用数据开放的边界。

## 2. 边界定义

### 2.1 门户聚合的职责

- **仅服务门户首页和办公中心场景**，不承担通用数据开放职责。
- 聚合接口输出的是面向用户侧的**读模型**，不是业务主数据。
- 通用数据服务、开放 API、连接器和跨系统数据交换统一由 `07-数据服务与集成` 承担。

### 2.2 不属于门户聚合的范围

- 第三方系统通过 API 获取待办数量 → 由 `07-open-api` 承担
- 跨系统数据同步 → 由 `07-data-sync` 承担
- 报表统计分析 → 由 `07-report` 承担
- 平台级服务治理 → 由 `07-governance` 承担

## 3. 聚合读模型设计

### 3.1 待办聚合读模型

| 字段 | 类型 | 来源模块 | 说明 |
|------|------|----------|------|
| totalCount | Integer | 02-todo-center | 待办总数 |
| urgentCount | Integer | 02-todo-center | 紧急待办数 |
| categoryStats | Map<String, Integer> | 02-todo-center | 按分类统计（审批/公文/会议等） |
| topItems | List<TodoItem> | 02-todo-center | 最新待办列表（默认 5 条） |

### 3.2 消息聚合读模型

| 字段 | 类型 | 来源模块 | 说明 |
|------|------|----------|------|
| unreadCount | Integer | 06-message-center | 未读消息数 |
| channelStats | Map<String, Integer> | 06-message-center | 按渠道统计 |
| topItems | List<MessageItem> | 06-message-center | 最新消息列表（默认 5 条） |

### 3.3 内容聚合读模型

| 字段 | 类型 | 来源模块 | 说明 |
|------|------|----------|------|
| latestAnnouncements | List<ArticleItem> | 04-portal-home | 最新公告（默认 5 条） |
| hotContents | List<ArticleItem> | 04-portal-home | 热门内容（默认 5 条） |

> 一期无 03 模块，此读模型暂由 04 模块内置的公告管理提供简化实现。

### 3.4 日程与会议聚合读模型

| 字段 | 类型 | 来源模块 | 说明 |
|------|------|----------|------|
| todaySchedule | List<ScheduleItem> | 05-协同办公 | 今日日程 |
| upcomingMeetings | List<MeetingItem> | 05-协同办公 | 即将开始的会议 |

> 一期无 05 模块，此读模型暂不实现。

### 3.5 业务看板聚合读模型

| 字段 | 类型 | 来源模块 | 说明 |
|------|------|----------|------|
| processStats | ProcessStats | 02-process-instance | 流程统计（发起数/处理中/已完成等基础指标） |
| attendanceStats | AttendanceStats | 05-协同办公 | 考勤统计 |

> 一期仅实现基于 `02-process-instance` 的基础流程统计，不依赖 `02-process-monitor`；考勤统计暂不实现。

### 3.6 常用流程与应用聚合读模型

| 字段 | 类型 | 来源模块 | 说明 |
|------|------|----------|------|
| frequentProcesses | List<ProcessItem> | 02-process-definition | 常用流程列表 |
| quickApps | List<AppItem> | 04-widget-config | 常用应用入口 |

## 4. 缓存策略

### 4.1 缓存层级

| 层级 | 存储 | TTL | 说明 |
|------|------|-----|------|
| L1 | 进程内缓存（Caffeine） | 30 秒 | 高频读取，减少 Redis 访问 |
| L2 | Redis | 5 分钟 | 跨实例共享，支持失效通知 |
| L3 | 数据源（各业务模块） | - | 缓存未命中时回源查询 |

### 4.2 缓存 Key 设计

```
portal:agg:{tenantId}:{personId}:{positionId}:{cardType}
```

- `cardType`：`todo` / `message` / `announcement` / `schedule` / `dashboard` / `quick-access`
- 身份切换时旧 Key 失效，新 Key 懒加载

### 4.3 缓存失效触发

| 事件 | 失效范围 |
|------|----------|
| `process.task.created` / `process.task.completed` | 待办聚合缓存 |
| `msg.notification.sent` / `msg.notification.read` | 消息聚合缓存 |
| `content.article.published` | 内容聚合缓存 |
| `org.identity.switched` | 该用户所有聚合缓存 |
| `org.identity-context.invalidated` | 该用户所有聚合缓存，并按回退身份重建或终止当前会话的聚合读取 |
| `org.role.position-bound` / `org.role.person-granted` | 受影响用户的数据范围相关缓存 |

## 5. 聚合接口设计

### 5.1 批量获取接口

```
GET /api/v1/portal/aggregation/dashboard
```

- 一次请求返回当前用户所有卡片数据
- 响应体按卡片类型分组
- 支持指定只获取部分卡片（`?cards=todo,message,announcement`）

### 5.2 单卡片刷新接口

```
GET /api/v1/portal/aggregation/card/{cardType}
```

- 单独刷新指定卡片数据
- 前端可在卡片操作后定向刷新

### 5.3 权限控制

- 聚合接口基于当前身份上下文裁剪数据范围
- 待办只返回当前身份可见的任务
- 消息只返回当前身份相关的通知
- 公告只返回当前身份可见范围的内容

## 6. 性能要求

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首页聚合接口响应时间 | < 500ms（缓存命中）/ < 2s（缓存未命中） | 包含所有卡片 |
| 单卡片刷新响应时间 | < 200ms（缓存命中）/ < 1s（缓存未命中） | 单卡片查询 |
| 首页并发承载 | 100 QPS | 单实例 |
| 缓存命中率 | > 90% | 正常负载下 |

## 7. 一期简化策略

- 一期不实现内容聚合和日程会议聚合（无 03/05 模块）。
- 一期业务看板仅实现基础流程统计，不引入 `02-process-monitor` 独立子模块。
- 一期公告由 04 模块内置简化公告管理提供，不依赖 03 模块。
- 一期常用应用入口由 `04-widget-config` 维护，不引入 `04-personalization` 独立子模块。
- 一期缓存仅使用 Redis（L2），不引入进程内缓存（L1）。
- 一期不实现 WebSocket 推送刷新，采用前端轮询（30 秒）+ 操作后定向刷新。
