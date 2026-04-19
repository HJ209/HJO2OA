# DataI18n 事件说明

`data-i18n` 负责发布业务字段翻译值和翻译完成状态事件。事件只表达共享翻译资源变化，不拥有业务对象生命周期。

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游用途 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `infra.translation.updated` | 某个翻译项新增、更新或审核状态变化后 | `entityType`、`entityId`、`fieldName`、`locale`、`translationStatus`、`updatedAt`、`tenantId` | 通知读模型、搜索投影和前端缓存刷新对应 locale 字段 | 以 `entityType + entityId + fieldName + locale + updatedAt` 幂等；重复投递只刷新同一条译文版本；事件只表示译文变化，不表示业务对象变化 |
| `infra.data-i18n.translation-completed` | 对象或批次达到约定完成度口径后 | `entityType`、`entityId`、`fieldNames`、`completedLocales`、`completionVersion`、`completedAt` | 通知业务域、治理台和前端工作台刷新完成度状态 | 以 `entityType + entityId + completionVersion` 幂等；完成后若需重开，必须通过新的完成版本或重开治理动作表达 |

## 消费事件

| 事件 | 来源 | 消费目的 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `infra.i18n.bundle-updated` | `HJO2OA-I18n` | 刷新可用语言集合、回退链和术语资源上下文 | `bundleCode`、`locale`、`bundleVersion`、`updatedAt` | 按 `bundleCode + locale + bundleVersion` 幂等；只更新语言解析投影，不修改现有翻译值 |
| 已接入的业务对象变更事件（如 `content.article.published`、`biz.document.approved`） | `03/05/07` 等业务域 | 生成待翻译候选、标记译文失效风险或刷新缺失项统计 | `entityType`、`entityId`、`changedFields`、`occurredAt`、`tenantId` | 按 `eventId` 或业务主键版本幂等；仅生成候选和投影，不接管业务对象生命周期 |

## 事件约束

| 项 | 说明 |
|----|------|
| 发布范围 | 一期只发布译文更新和完成状态事件，不发布每次解析命中、回退命中或页面渲染事件 |
| 补偿原则 | 误译、撤销审核和完成重开通过新的翻译版本或治理动作表达，不允许改写历史审计 |
| 投影限制 | locale 视图、缺失项统计和完成度面板都是运行投影，不替代 `TranslationEntry` 真相源 |
