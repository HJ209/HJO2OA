# portal-model 事件合同

## 事件定位

`portal-model` 是 `04` 内模板与发布事件的核心发布者，事件主要用于通知下游刷新模板解析、个性化基线和聚合快照。一期以发布事件为主，消费事件仅用于辅助校验卡片引用状态。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 下游用途 |
|----------|----------|--------------|----------|
| `portal.template.created` | 模板创建成功 | `templateId`、`code`、`sceneType` | 初始化设计器、模板目录和后续版本链。 |
| `portal.template.published` | 某个模板版本发布成功 | `templateId`、`versionNo`、`sceneType` | 通知下游刷新模板版本基线和相关缓存。 |
| `portal.template.deprecated` | 旧模板版本被废弃 | `templateId`、`versionNo` | 通知管理端和设计器更新版本状态。 |
| `portal.publication.activated` | 发布进入生效状态 | `publicationId`、`templateId`、`sceneType`、`clientType` | 驱动 `personalization` 重新绑定基线模板，驱动 `aggregation-api` 失效受影响快照。 |
| `portal.publication.offlined` | 发布下线或失效 | `publicationId`、`templateId` | 驱动模板解析结果和门户页面壳重新计算。 |

## 消费事件

| 事件类型 | 来源模块 | 处理动作 | 一期说明 |
|----------|----------|----------|----------|
| `portal.widget.updated` | `widget-config` | 失效受影响模板的卡片引用校验缓存和设计器预览缓存。 | 不自动改写模板结构，只更新校验结果。 |
| `portal.widget.disabled` | `widget-config` | 标记受影响草稿/发布在后续编辑和再发布时必须修复卡片引用。 | 不删除历史版本中的布置记录。 |

## 幂等、快照刷新与投影说明

- 模板事件以 `templateId + versionNo` 或 `publicationId` 作为幂等主键，重复投递不得生成重复版本状态变更。
- 本模块不维护门户首页快照，事件只驱动模板解析缓存、引用校验缓存和下游读模型失效。
- `portal.widget.updated`/`portal.widget.disabled` 只更新“模板引用状态”投影视图，不直接回写模板版本内容。
- 发布下线或废弃历史版本时，历史版本记录和审计记录仍需保留。
