# 04-门户与工作台-开发任务

## 目标

完成统一门户与工作台能力建设，使系统具备统一入口、角色化首页、个性化工作台和多模块信息聚合能力，并在前端布局上参考 O2OA 的门户首页、办公中心和门户设计器模式。

## 子模块开发任务

### 4.1 portal-model 门户页面模型管理

**后端**
- 设计门户、页面、子页面、布局区域、卡片组件、模板和发布状态模型
- 设计支持 O2OA 风格首页的三段式布局模型
- 设计支持办公中心双栏结构的页面模板
- 建立门户草稿、预览、发布和下线机制
- 建立模板复制和角色化配置能力
- 发布 `portal.template.created`、`portal.template.published`、`portal.template.deprecated`
- 发布 `portal.publication.activated`、`portal.publication.offlined`

**前端**
- 完成门户管理页、页面区域配置
- 支持门户版本发布和下线操作

### 4.2 widget-config 工作台组件配置中心

**后端**
- 建立卡片类型、数据源、展示策略和权限规则模型
- 发布 `portal.widget.updated`、`portal.widget.disabled`，用于模板引用校验、门户缓存与协议刷新

**前端**
- 完成卡片配置页、数据源配置
- 支持卡片类型选择、展示策略配置

### 4.3 personalization 个性化与角色化配置

**后端**
- 建立个性化配置存储能力，如卡片排序、常用应用和主题设置
- 发布 `portal.personalization.saved`、`portal.personalization.reset`，用于视图解析与缓存失效

**前端**
- 完成模板选择页、个性化设置页
- 支持卡片排序、常用应用维护和主题设置

### 4.4 aggregation-api 聚合数据接口

**后端**
- 建立待办、消息、公告、日程、会议、常用应用和统计卡片的聚合接口
- 建立门户访问权限控制和页面可见范围控制
- 消费业务事件与 Portal 配置事件更新缓存，并发布 `portal.snapshot.refreshed`、`portal.snapshot.failed`

**前端**
- 完成聚合数据展示组件
- 保证待办、公告、会议、任务等多源数据的展示一致性

### 4.5 portal-home 门户首页与办公中心

**后端**
- 无独立后端逻辑，渲染由 portal-model 和 aggregation-api 驱动
- 不发布领域事件，消费 `portal.publication.activated` / `portal.publication.offlined`、`portal.personalization.saved` / `portal.personalization.reset`、`portal.snapshot.refreshed` / `portal.snapshot.failed`

**前端**
- 完成统一工作台首页
- 首页布局支持顶部品牌区、中部上中下分段聚合区和底部说明区
- 支持待办、消息、公告、日程、快捷入口、统计图卡等卡片展示
- 完成左侧工作导航、右侧列表与处理区的办公中心布局
- 支持移动端工作台的简化布局

### 4.6 portal-designer 门户设计器

**后端**
- 无独立后端逻辑，设计器保存最终由 `portal-model` 发布正式领域事件
- 消费 `portal.widget.updated` / `portal.widget.disabled`、`portal.template.published` / `portal.template.deprecated`、`portal.publication.activated` / `portal.publication.offlined`

**前端**
- 完成门户设计器，布局采用左侧组件区、顶部工具栏、中部画布区、右侧属性区
- 支持页面、部件、脚本、资源文件和页面属性等配置分区
- 支持不同角色、组织和场景的模板切换和发布
- 支持实时预览、草稿保存和多端预览

## 联调与协作任务

- 明确聚合接口字段与各业务模块的数据责任边界
- 明确门户模板、用户个性化配置和发布机制之间的优先级
- 统一卡片组件的数据协议和跳转协议
- 明确首页布局模板、办公中心布局模板和设计器模板的复用边界

## 验收标准

- 用户登录后可看到统一工作台和与自己相关的信息聚合
- 门户首页可呈现符合 O2OA 风格的品牌区、聚合区和底部信息区
- 办公中心可通过左导航与右工作区高效处理待办与业务事项
- 管理员可配置多种门户模板并按角色或组织发布
- 用户可在允许范围内完成个性化设置
- 门户页面跳转链路顺畅，数据刷新及时准确
- 移动端可用工作台覆盖高频办公场景
