# 03-内容与知识-开发任务

## 目标

完成内容管理与知识库底座建设，使平台具备栏目管理、内容编辑、审核发布、知识沉淀、全文检索和订阅收藏能力。

## 子模块开发任务

### 3.1 category-management 栏目与分类管理

**后端**
- 设计栏目、分类、标签、专题模型
- 建立栏目权限规则
- 发布 `content.category.changed`、`content.taxonomy.changed`、`content.category-permission.changed` 领域事件

**前端**
- 完成栏目管理页、分类管理页、标签管理页
- 支持栏目层级调整、权限预览和批量启停用

### 3.2 content-lifecycle 内容生命周期管理

**后端**
- 设计内容、版本、审核记录和发布记录模型
- 建立内容草稿、送审、审核、发布、下线、归档等生命周期管理能力
- 建立内容审核流程接入点
- 发布 `content.article.created`、`content.article.submitted`、`content.article.published`、`content.article.unpublished`、`content.article.archived` 领域事件

**前端**
- 完成内容编辑页、审核页、发布页
- 支持富文本编辑、附件挂载、封面配置、版本查看和审核操作
- 提供发布范围配置和定时发布配置能力

### 3.3 content-storage 内容正文与附件管理

**后端**
- 建立内容正文、摘要、封面、附件和扩展属性的存储策略
- 支持内容版本回溯、历史比较和操作留痕
- 发布 `content.version.created`、`content.attachment.bound`、`content.relation.changed` 领域事件

**前端**
- 完成正文编辑区、附件上传区、版本历史
- 优化 PC 与移动端附件预览体验

### 3.4 content-permission 权限与可见范围控制

**后端**
- 建立内容按组织、角色、岗位、身份和用户的可见范围规则
- 发布 `content.visibility.changed`、`content.scope-template.changed` 事件，并消费栏目权限和发布状态变化

**前端**
- 完成发布范围配置、可见范围提示
- 内容权限判定由后端主导，前端负责合理展示

### 3.5 content-search 搜索与推荐

**后端**
- 建立全文检索索引模型和更新机制
- 发布 `content.search-index.refreshed`、`content.subscription.changed`、`content.favorite.changed` 事件
- 消费 `content.article.published`、`content.article.unpublished`、`content.visibility.changed`、`content.category.changed` 事件刷新索引和个人入口

**前端**
- 完成搜索框、搜索结果页、推荐列表
- 支持全文搜索、标签筛选、排序、收藏、订阅和相关推荐

### 3.6 content-statistics 内容统计与运营分析

**后端**
- 建立阅读量、下载量、收藏量、热度排行和发布效果分析能力
- 发布 `content.engagement.snapshot.refreshed`、`content.read.threshold-reached` 事件
- 消费 `content.favorite.changed`、`content.article.published`、`content.article.unpublished` 事件更新统计快照

**前端**
- 完成内容阅读统计页、运营统计页
- 完成热点排行、订阅情况和发布效果分析页面
- 提供审核记录和版本变更记录查看入口

## 联调与协作任务

- 统一内容详情、内容列表、搜索结果和统计接口协议
- 明确内容权限判定逻辑与前端展示边界
- 明确内容与消息中心、门户工作台、搜索中心的联动规则

## 验收标准

- 内容可以按栏目完整创建、审核、发布和归档
- 不同组织和角色看到的内容范围正确
- 用户可通过搜索、栏目浏览和订阅获取信息
- 版本历史、审核记录和运营统计完整可查
- 移动端可流畅阅读主要内容和附件
