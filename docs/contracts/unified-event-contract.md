# 统一事件契约

## 1. 文档目的

本文档定义 HJO2OA 平台所有跨模块事件的统一契约规范，包括事件命名、事件载荷、消费关系、事件版本、投递保证、补偿机制和异常处理。所有模块的事件设计必须遵循本契约。

对应架构决策编号：D12（建立统一事件契约）、D13（最终一致性）、D16（待办与消息分工）。

## 2. 总体原则

- 所有跨模块**业务联动**（状态变更、权限重算、消息触发等）必须通过事件总线完成，禁止模块间直接同步调用业务逻辑。
- 跨模块**只读查询**（如获取人员信息、获取角色列表）允许通过内部服务接口同步调用，但必须满足：仅限查询、不触发写操作、调用方不依赖被调用方的内部模型。
- 事件发布者不关心消费者的存在和数量，消费者不关心事件的发布者。
- 事件载荷必须自包含，消费者无需回查发布者即可完成业务处理。
- 事件必须可追溯、可审计、可重放。
- 事件投递保证至少一次，消费端必须幂等。

## 3. 事件命名规范

### 3.1 命名格式

 ```
 {模块前缀}.{事件主题}.{动作}
 ```

 全部小写，点号分隔。

- `事件主题` 可以是单层主题（如 `article`），也可以是多层语义主题（如 `engagement.snapshot`）。

### 3.2 模块前缀约定

| 模块 | 前缀 |
|------|------|
| 00-平台基础设施 | `infra` |
| 01-组织与权限 | `org` |
| 02-流程与表单 | `process` / `form` / `todo` |
| 03-内容与知识 | `content` |
| 04-门户与工作台 | `portal` |
| 05-协同办公应用 | `biz` |
| 06-消息移动与生态 | `msg` |
| 07-数据服务与集成 | `data` |

### 3.3 动作命名

| 动作 | 含义 | 示例 |
|------|------|------|
| created | 实体创建 | `org.organization.created` |
| updated | 实体更新 | `org.person.updated` |
| deleted | 实体删除 | `infra.attachment.deleted` |
| disabled | 实体停用 | `org.organization.disabled` |
| published | 定义发布 | `process.definition.published` |
| deprecated | 定义废弃 | `process.definition.deprecated` |
| started | 流程/实例启动 | `process.instance.started` |
| completed | 流程/任务完成 | `process.task.completed` |
| terminated | 流程/任务终止 | `process.instance.terminated` |
| suspended | 流程挂起 | `process.instance.suspended` |
| resumed | 流程恢复 | `process.instance.resumed` |
| claimed | 任务认领 | `process.task.claimed` |
| transferred | 任务转办 | `process.task.transferred` |
| overdue | 超时 | `process.task.overdue` |
| hierarchy-changed | 层级变更 | `org.organization.hierarchy-changed` |
| primary-changed | 主岗变更 | `org.assignment.primary-changed` |
| expired | 到期 | `org.assignment.expired` |
| locked | 锁定 | `org.account.locked` |
| unlocked | 解锁 | `org.account.unlocked` |
| login-succeeded | 登录成功 | `org.account.login-succeeded` |
| login-failed | 登录失败 | `org.account.login-failed` |
| bound / unbound | 绑定/解绑 | `org.role.position-bound` / `org.role.position-unbound` |
| granted / revoked | 授权/撤销 | `org.role.person-granted` / `org.role.person-revoked` |
| changed | 聚合状态或配置发生变更 | `content.visibility.changed` |
| assigned | 任务分配 | `biz.task.assigned` |
| requested | 申请提交 | `biz.attendance.requested` |
| activated | 启用/激活 | `data.service.activated` |
| alerted | 告警产生 | `data.governance.alerted` |
| invalidated | 运行时上下文或缓存资格失效 | `org.identity-context.invalidated` |
| sent | 消息发送 | `msg.notification.sent` |
| delivered | 消息送达 | `msg.notification.delivered` |

### 3.4 命名约束

- 事件名称使用现在时态动词（created 而非 creation）。
- 动作部分统一使用**全小写 + 连字符**分隔（如 `hierarchy-changed`、`login-succeeded`），禁止 camelCase。
- 事件名称不可修改，废弃后标记 deprecated。
- 新增事件必须在本文档中注册。
- 本文档只登记**跨模块总线事件**；模块内部领域事件仍在各父模块与子模块 `events/*.events.md` 中维护。
- `06-消息移动与生态` 对外跨模块口径统一注册 `msg.notification.*`；`msg.message.*` 与 `msg.channel.*` 仅作为模块内部领域事件使用。

## 4. 事件载荷规范

### 4.1 载荷信封

所有事件必须包含以下信封字段：

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "org.organization.created",
  "eventVersion": "1",
  "source": "org-structure",
  "timestamp": "2026-04-18T01:30:00.000Z",
  "correlationId": "corr-550e8400",
  "operatorAccountId": "account-001",
  "operatorPersonId": "person-001",
  "tenantId": "tenant-001",
  "traceId": "trace-abc123",
  "payload": { ... },
  "metadata": { ... }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | UUID | 是 | 事件唯一标识，全局唯一 |
| eventType | String | 是 | 事件类型，与命名规范一致 |
| eventVersion | String | 是 | 事件载荷版本，语义化版本（仅主版本） |
| source | String | 是 | 事件源，发布者模块/子模块标识 |
| timestamp | String | 是 | 事件发生时间，ISO 8601 UTC |
| correlationId | String | 是 | 关联 ID，同一业务操作的多个事件共享 |
| operatorAccountId | UUID | 是 | 触发事件的账号 ID（用于审计与安全判定） |
| operatorPersonId | UUID | 是 | 触发事件的人员 ID（用于权限重算与业务联动） |
| tenantId | UUID | 是 | 租户 ID，用于消费端数据隔离 |
| traceId | String | 是 | 链路追踪 ID |
| payload | Object | 是 | 事件业务载荷 |
| metadata | Object | 否 | 扩展元数据 |

### 4.2 载荷设计原则

- **自包含**：payload 必须包含消费者完成业务处理所需的全部信息，不依赖回查发布者。
- **最小化**：payload 只包含必要字段，不传递完整实体快照。
- **变更字段规则**：当 payload 使用 `changedFields` 时，必须同时包含变更字段的**新值**（如 `{"name": {"old": "旧名", "new": "新名"}}`），确保消费者无需回查即可完成联动处理。仅当变更字段数量过多（>10）或包含大文本/附件时，可降级为仅传递 `changedFieldNames`，但需在事件注册表中标注 `payloadStrategy: delta-reference` 并说明回查接口。
- **稳定化**：payload 字段变更必须升级 eventVersion。
- **标准化**：ID 字段使用 UUID，时间字段使用 ISO 8601，枚举使用大写蛇形。

### 4.3 载荷变更规则

| 变更类型 | 是否需要升级版本 | 说明 |
|----------|------------------|------|
| 新增可选字段 | 否 | 消费者必须容忍未知字段 |
| 删除字段 | 是 | 不兼容变更 |
| 变更字段类型 | 是 | 不兼容变更 |
| 变更字段语义 | 是 | 不兼容变更 |
| 新增枚举值 | 否 | 消费者必须容忍未知枚举值 |

## 5. 事件目录

以下目录列示当前已注册的核心跨模块/跨子模块事件。模块内局部投影事件可以在子模块文档中进一步细化，但如果进入跨模块或跨子模块联动范围，必须先在此注册。

### 5.1 00-平台基础设施事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `infra.event.schema-registered` | 1 | eventType, eventVersion, modulePrefix, schemaChecksum | 00 |
| `infra.event.schema-deprecated` | 1 | eventType, eventVersion, deprecatedAt, sunsetAt | 00 |
| `infra.event.published` | 1 | eventId, eventType, source, tenantId, publishedAt | 00/06 |
| `infra.event.delivery-failed` | 1 | eventId, eventType, subscriberCode, attemptNo, errorCode | 00/06 |
| `infra.event.dead-lettered` | 1 | eventId, eventType, subscriberCode, deadLetteredAt | 00/06 |
| `infra.i18n.bundle-updated` | 1 | bundleCode, moduleCode, locale, bundleVersion | 00/01/02/03/04/05/06/07 |
| `infra.i18n.locale-changed` | 1 | scopeType, scopeId, oldLocale, newLocale | 04/06 |
| `infra.timezone.setting-changed` | 1 | scopeType, scopeId, timezoneId, effectiveFrom | 00/04/05/06/07 |
| `infra.timezone.user-changed` | 1 | personId, tenantId, oldTimezoneId, newTimezoneId | 04/06 |
| `infra.translation.updated` | 1 | entityType, entityId, fieldName, locale, status | 03/04/05/07 |
| `infra.data-i18n.translation-completed` | 1 | entityType, entityId, localeSet, completedAt | 03/04/05/07 |
| `infra.attachment.created` | 1 | attachmentId, fileName, fileSize, businessType, businessId | 02/03/05/07 |
| `infra.attachment.deleted` | 1 | attachmentId, businessType, businessId, deletedAt | 02/03/05/07 |
| `infra.attachment.version-created` | 1 | attachmentId, versionNo, checksum, createdAt | 02/03/05 |
| `infra.attachment.quota-warning` | 1 | tenantId, ownerType, ownerId, usedBytes, totalBytes, threshold | 00/06 |
| `infra.dictionary.updated` | 1 | dictionaryTypeId, dictionaryCode, locale, version | 02/03/05 |
| `infra.dictionary.type-created` | 1 | dictionaryTypeId, code, category | 02/03/05 |
| `infra.dictionary.item-changed` | 1 | dictionaryTypeId, itemId, itemCode, action | 02/03/05 |
| `infra.config.updated` | 1 | configKey, scopeType, scopeId, newValue | 01/02/03/04/05/06/07 |
| `infra.feature-flag.changed` | 1 | featureKey, enabled, scopeType, scopeId | 01/02/04/06/07 |
| `infra.audit.archived` | 1 | archiveBatchId, archivedAt, recordCount | 00 |
| `infra.cache.invalidated` | 1 | namespace, keyPattern, reason, invalidatedAt | 00/04/06 |
| `infra.tenant.created` | 1 | tenantId, tenantCode, tenantName | 00/01 |
| `infra.tenant.updated` | 1 | tenantId, changedFields, status | 00/01/07 |
| `infra.tenant.disabled` | 1 | tenantId, disabledAt, reason | 01/02/03/04/05/06/07 |
| `infra.tenant.initialized` | 1 | tenantId, initProfileCode, initializedAt | 00/01/07 |
| `infra.tenant.quota-warning` | 1 | tenantId, quotaType, usedValue, limitValue, warningThreshold | 00/06 |
| `infra.error-code.updated` | 1 | errorCode, severity, httpStatus, localeKey | 01/02/03/04/05/06/07 |
| `infra.scheduler.task-succeeded` | 1 | jobId, jobCode, executionId, finishedAt | 00/07 |
| `infra.scheduler.task-failed` | 1 | jobId, jobCode, executionId, errorCode, retryable | 00/06/07 |
| `infra.scheduler.task-retrying` | 1 | jobId, jobCode, executionId, nextRetryAt, attemptNo | 00/06/07 |
| `infra.security.policy-updated` | 1 | policyCode, policyType, version, changedAt | 00/06 |
| `infra.security.sensitive-operation` | 1 | policyCode, operationType, subjectType, subjectId, occurredAt | 00/06 |
| `infra.security.anomaly-detected` | 1 | policyCode, subjectType, subjectId, alertLevel, detectedAt | 00/06 |

### 5.2 01-组织与权限事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `org.organization.created` | 1 | organizationId, code, name, parentId | 02/04/06 |
| `org.organization.updated` | 1 | organizationId, changedFields | 02/04/06 |
| `org.organization.hierarchy-changed` | 1 | organizationId, oldParentId, newParentId, affectedDescendantIds | 01/02/04/06 |
| `org.organization.disabled` | 1 | organizationId | 01/02/04/06 |
| `org.department.created` | 1 | departmentId, organizationId, parentId | 02/04/06 |
| `org.department.updated` | 1 | departmentId, changedFields | 02/04/06 |
| `org.department.hierarchy-changed` | 1 | departmentId, affectedDescendantIds | 01/02/04/06 |
| `org.department.disabled` | 1 | departmentId | 01/02/04/06 |
| `org.position.created` | 1 | positionId, organizationId, departmentId | 01/06 |
| `org.position.updated` | 1 | positionId, changedFields | 01/06 |
| `org.position.disabled` | 1 | positionId | 01/06 |
| `org.assignment.created` | 1 | assignmentId, personId, positionId, type | 01/02/04/06 |
| `org.assignment.primary-changed` | 1 | personId, oldPositionId, newPositionId | 01/02/04/06 |
| `org.assignment.removed` | 1 | assignmentId, personId, positionId | 01/02/04/06 |
| `org.assignment.expired` | 1 | assignmentId, personId, positionId | 01/02/04/06 |
| `org.person.created` | 1 | personId, employeeNo, organizationId | 02/04/06 |
| `org.person.updated` | 1 | personId, changedFields | 02/04/06 |
| `org.person.disabled` | 1 | personId | 02/04/06 |
| `org.person.resigned` | 1 | personId | 02/04/06 |
| `org.account.created` | 1 | accountId, personId, username, authType | 06 |
| `org.account.locked` | 1 | accountId, reason, lockedUntil | 06 |
| `org.account.unlocked` | 1 | accountId | 06 |
| `org.account.login-succeeded` | 1 | accountId, loginIp | 00/06 |
| `org.account.login-failed` | 1 | accountId, reason | 00 |
| `org.role.created` | 1 | roleId, code, category | 01/02 |
| `org.role.updated` | 1 | roleId, changedFields | 01/02 |
| `org.role.disabled` | 1 | roleId | 01/02 |
| `org.role.position-bound` | 1 | positionId, roleId | 01/02 |
| `org.role.position-unbound` | 1 | positionId, roleId | 01/02 |
| `org.role.person-granted` | 1 | personId, roleId, reason | 01/02 |
| `org.role.person-revoked` | 1 | personId, roleId | 01/02 |
| `org.resource-permission.changed` | 1 | roleId, resourceTypes, permissionCount, version | 01 |
| `org.data-permission.row-changed` | 1 | policyId, subjectType, subjectId, businessObject, scopeType | 01/02 |
| `org.data-permission.field-changed` | 1 | policyId, subjectType, subjectId, businessObject, fieldCode | 01/02 |
| `org.identity.switched` | 1 | personId, fromPositionId, toPositionId | 02/04/06 |
| `org.identity-context.invalidated` | 1 | personId, accountId, invalidatedAssignmentId, fallbackAssignmentId, reasonCode, forceLogout | 02/03/04/06 |
| `org.sync.completed` | 1 | syncTaskId, sourceId, sourceType, syncMode, failedCount | 01 |
| `org.sync.failed` | 1 | syncTaskId, sourceId, sourceType, syncMode, errorCode | 01 |
| `org.audit.org-changed` | 1 | auditLogId, entityType, entityId, action, operatorId | 01 |
| `org.audit.auth-changed` | 1 | auditLogId, entityType, entityId, action, operatorId | 01 |
| `org.audit.account-changed` | 1 | auditLogId, entityType, entityId, action, operatorId | 01 |

### 5.3 02-流程与表单事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `process.definition.created` | 1 | definitionId, code, version | 04/06 |
| `process.definition.published` | 1 | definitionId, code, version | 04/06 |
| `process.definition.deprecated` | 1 | definitionId, code, version | 04/06 |
| `process.instance.started` | 1 | instanceId, definitionId, initiatorId, initiatorOrgId | 04/06 |
| `process.instance.completed` | 1 | instanceId, endTime | 03/04/05/06 |
| `process.instance.terminated` | 1 | instanceId, reason | 04/06 |
| `process.instance.suspended` | 1 | instanceId, reason | 04/06 |
| `process.instance.resumed` | 1 | instanceId | 04/06 |
| `process.task.created` | 1 | taskId, instanceId, nodeId, candidateType, candidateIds | 04/06 |
| `process.task.claimed` | 1 | taskId, assigneeId | 04/06 |
| `process.task.completed` | 1 | taskId, instanceId, actionCode | 03/04/05/06 |
| `process.task.terminated` | 1 | taskId, reason | 04/06 |
| `process.task.transferred` | 1 | taskId, fromPersonId, toPersonId | 04/06 |
| `process.task.overdue` | 1 | taskId, instanceId, dueTime | 06 |
| `form.metadata.created` | 1 | metadataId, code, version | 04 |
| `form.metadata.published` | 1 | metadataId, code, version | 04 |
| `form.metadata.deprecated` | 1 | metadataId, code, version | 04 |
| `todo.item.created` | 1 | todoId, taskId, assigneeId, type, category | 04/06 |
| `todo.item.completed` | 1 | todoId, taskId | 04/06 |
| `todo.item.cancelled` | 1 | todoId, reason | 04/06 |
| `todo.item.overdue` | 1 | todoId, dueTime | 06 |

### 5.4 03-内容与知识事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `content.category.changed` | 1 | categoryId, parentId, status, version | 03/04/06 |
| `content.taxonomy.changed` | 1 | taxonomyType, objectId, action | 03/04 |
| `content.category-permission.changed` | 1 | categoryId, ruleVersion, scopeHash | 03 |
| `content.article.created` | 1 | articleId, categoryId, creatorId | 00/03 |
| `content.article.submitted` | 1 | articleId, publicationId, reviewMode | 02/06 |
| `content.article.published` | 1 | articleId, categoryId, publicationId, visibleScope | 04/06/07 |
| `content.article.unpublished` | 1 | articleId, publicationId, reason | 04/06/07 |
| `content.article.archived` | 1 | articleId, publicationId | 04/06/07 |
| `content.visibility.changed` | 1 | publicationId, articleId, scopeVersion | 03/04 |
| `content.scope-template.changed` | 1 | templateId, action | 03 |
| `content.version.created` | 1 | articleId, versionId, sourceVersionId | 03 |
| `content.attachment.bound` | 1 | versionId, addedAttachmentIds, removedAttachmentIds | 00/03 |
| `content.relation.changed` | 1 | versionId, relationTypes | 03/04 |
| `content.search-index.refreshed` | 1 | scope, articleIds, refreshAt | 07 |
| `content.subscription.changed` | 1 | userId, targetType, targetId, action | 06 |
| `content.favorite.changed` | 1 | userId, articleId, action | 03 |
| `content.read.threshold-reached` | 1 | articleId, metricType, value | 06 |
| `content.engagement.snapshot.refreshed` | 1 | scope, window, snapshotAt | 07 |

### 5.5 04-门户与工作台事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `portal.template.created` | 1 | templateId, code, sceneType | 04 |
| `portal.template.published` | 1 | templateId, versionNo, sceneType | 04 |
| `portal.template.deprecated` | 1 | templateId, versionNo | 04 |
| `portal.widget.updated` | 1 | widgetId, code, changedFields | 04 |
| `portal.widget.disabled` | 1 | widgetId, code | 04 |
| `portal.publication.activated` | 1 | publicationId, templateId, sceneType, clientType | 04 |
| `portal.publication.offlined` | 1 | publicationId, templateId | 04 |
| `portal.personalization.saved` | 1 | profileId, personId, sceneType | 04 |
| `portal.personalization.reset` | 1 | profileId, personId, sceneType | 04 |
| `portal.snapshot.refreshed` | 1 | snapshotKey, cardType, refreshedAt | 04 |
| `portal.snapshot.failed` | 1 | snapshotKey, cardType, reason | 04 |

### 5.6 05-协同办公应用事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `biz.document.submitted` | 1 | documentId, documentType, initiatorId | 02/06 |
| `biz.document.approved` | 1 | documentId, documentType, documentNo | 03/06 |
| `biz.document.archived` | 1 | documentId, documentType, archivedAt | 03/04/06 |
| `biz.meeting.created` | 1 | meetingId, organizerId, startTime, endTime | 04/06 |
| `biz.meeting.cancelled` | 1 | meetingId, reason, cancelledAt | 04/06 |
| `biz.meeting.minutes-published` | 1 | meetingId, minutesId, publishedAt | 04/06 |
| `biz.schedule.created` | 1 | scheduleId, ownerType, ownerId, sourceType | 04/06 |
| `biz.schedule.updated` | 1 | scheduleId, changedFields, status | 04/06 |
| `biz.task.assigned` | 1 | taskId, assigneeId, dueAt, priority | 04/06 |
| `biz.task.completed` | 1 | taskId, assigneeId, completedAt | 04/06/07 |
| `biz.task.overdue` | 1 | taskId, assigneeId, dueAt | 04/06 |
| `biz.attendance.requested` | 1 | requestId, applicantId, requestType | 02/06 |
| `biz.attendance.result-changed` | 1 | personId, date, status | 04/06/07 |
| `biz.contract.expiring` | 1 | contractId, effectiveTo, ownerOrgId | 04/06 |
| `biz.asset.status-changed` | 1 | assetId, status, actionType | 04/06/07 |
| `biz.space.updated` | 1 | spaceId, changedFields, status | 04/06 |

### 5.7 06-消息移动与生态事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `msg.notification.sent` | 1 | notificationId, recipientId, channel, category | 00 |
| `msg.notification.delivered` | 1 | notificationId, channel, deliveredAt | 00 |
| `msg.notification.read` | 1 | notificationId, recipientId | 00 |

### 5.8 07-数据服务与集成事件

| 事件类型 | eventVersion | payload 关键字段 | 消费者 |
|----------|-------------|-----------------|--------|
| `data.service.activated` | 1 | serviceId, code, serviceType, permissionMode | 00/06/07 |
| `data.api.published` | 1 | apiId, code, version, path, httpMethod | 06/07 |
| `data.api.deprecated` | 1 | apiId, code, version, sunsetAt | 06/07 |
| `data.connector.updated` | 1 | connectorId, code, connectorType, status | 07 |
| `data.sync.completed` | 1 | syncTaskId, insertedCount, updatedCount, failedCount | 00/06/07 |
| `data.sync.failed` | 1 | syncTaskId, error, retryable | 00/06/07 |
| `data.report.refreshed` | 1 | reportId, code, snapshotAt, freshnessStatus | 04/07 |
| `data.governance.alerted` | 1 | governanceId, targetCode, alertLevel | 06/07 |

## 6. 事件投递保证

### 6.1 投递语义

- **至少一次**（at-least-once）：事件总线保证每个事件至少投递一次。
- 消费者必须实现幂等处理，容忍重复投递。

### 6.2 幂等策略

| 策略 | 适用场景 | 说明 |
|------|----------|------|
| eventId 去重 | 通用 | 消费者维护已处理 eventId 集合，重复 eventId 跳过 |
| 业务键去重 | 高频场景 | 使用业务唯一键（如 personId+roleId）判重 |
| 天然幂等 | 天然幂等操作 | 设置操作天然幂等（如状态置为同一值），无需额外去重 |

### 6.3 事件持久化

- 事件发布前必须持久化到事件表（outbox 模式）。
- 事件表与业务操作在同一事务中写入，保证原子性。
- 事件投递成功后标记为已发送，未投递的事件由定时任务补偿。

## 7. 事件消费规范

### 7.1 消费者注册

- 消费者必须声明订阅的事件类型和处理方法。
- 消费者必须声明并发度和重试策略。
- 消费者注册信息由事件总线统一管理。

### 7.2 消费确认

- 消费者处理成功后必须发送 ACK。
- 处理失败后发送 NACK，事件总线根据重试策略重新投递。
- 超过最大重试次数后进入死信队列。

### 7.3 消费顺序

- 同一 entityId 的事件保证顺序消费（分区键为 entityId）。
- 不同 entityId 的事件不保证顺序。
- 消费者不可依赖跨 entityId 的事件顺序。

## 8. 事件版本策略

### 8.1 版本号

- 事件载荷版本使用整数，从 1 开始。
- 版本号变更仅在不兼容变更时递增。

### 8.2 版本兼容

- 消费者必须声明支持的版本范围。
- 事件总线根据版本范围路由事件。
- 新增可选字段的兼容变更不升级版本号。
- 删除字段、变更类型的不兼容变更必须升级版本号。

### 8.3 版本迁移

- 旧版本事件至少保留 6 个月。
- 旧版本事件标记 deprecated 后，在 Sunset 日期后停止投递。
- 消费者必须在 Sunset 日期前完成版本迁移。

## 9. 补偿机制（D13）

### 9.1 最终一致性原则

所有跨模块异步链路必须具备重试、死信、补偿和审计入口。

### 9.2 重试策略

| 参数 | 默认值 | 说明 |
|------|--------|------|
| maxRetries | 5 | 最大重试次数 |
| initialInterval | 1s | 初始重试间隔 |
| multiplier | 2 | 间隔倍增因子 |
| maxInterval | 60s | 最大重试间隔 |

- 重试采用指数退避策略。
- 重试次数耗尽后进入死信队列。

### 9.3 死信队列

- 死信队列中的事件必须有人工处理入口。
- 死信事件包含原始事件、失败原因、重试次数和最后失败时间。
- 运维人员可查看、重试或丢弃死信事件。
- 死信事件的处理结果必须记录审计日志。

### 9.4 补偿操作

- 关键业务链路必须设计补偿操作（反向操作）。
- 补偿操作通过事件触发，如 `process.instance.terminated` 触发相关业务模块的回滚。
- 补偿操作必须幂等。
- 补偿操作的结果必须记录审计日志。

### 9.5 审计入口

- 所有事件的发布和消费必须记录审计日志。
- 审计日志包含：eventId、eventType、发布者、消费者、投递时间、消费时间、处理结果。
- 审计日志保留期至少 90 天。
- 审计日志可通过 `00-审计日志` 模块查询。

## 10. 异常处理规范

### 10.1 发布端异常

| 异常场景 | 处理方式 |
|----------|----------|
| 事件持久化失败 | 业务操作回滚，不发布事件 |
| 事件投递失败 | 保留在 outbox 表，由定时任务补偿投递 |
| 事件载荷序列化失败 | 记录错误日志，标记事件为异常，人工介入 |

### 10.2 消费端异常

| 异常场景 | 处理方式 |
|----------|----------|
| 事件载荷反序列化失败 | 进入死信队列，记录错误日志 |
| 业务处理失败（可重试） | NACK，按重试策略重新投递 |
| 业务处理失败（不可重试） | 进入死信队列，记录错误日志 |
| 幂等校验重复 | ACK，跳过处理 |
| 版本不兼容 | 进入死信队列，告警通知 |

## 11. 事件总线基础设施

### 11.1 技术选型

- 消息中间件：**RabbitMQ**（ADR-003 已确定）
- 选型依据：吞吐量满足一期需求、支持消息持久化与消费确认、死信队列与延迟消息支持完善、运维复杂度可控
- 事件总线基于 Spring AMQP 封装，提供统一发布/订阅 API

### 11.2 事件表设计（Outbox 模式）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 事件记录 ID |
| eventId | UUID | 事件唯一标识 |
| eventType | String | 事件类型 |
| eventVersion | String | 事件版本 |
| aggregateType | String | 聚合类型 |
| aggregateId | UUID | 聚合 ID |
| payload | JSON | 事件载荷 |
| metadata | JSON | 元数据 |
| status | Enum | 状态：PENDING / SENT / FAILED |
| publishedAt | Timestamp | 发布时间 |
| sentAt | Timestamp | 投递成功时间 |
| retryCount | Integer | 重试次数 |
| tenantId | UUID | 租户 ID |

### 11.3 定时补偿任务

- 扫描 status=PENDING 且 publishedAt 超过 30 秒的事件，重新投递。
- 扫描 status=FAILED 且 retryCount < maxRetries 的事件，按退避策略重试。
- 补偿任务由 `00-定时任务` 统一调度。
