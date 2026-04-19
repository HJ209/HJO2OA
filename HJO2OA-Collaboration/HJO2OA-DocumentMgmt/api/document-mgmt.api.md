# document-mgmt API 说明

## 接口分组

| 分组 | 关键接口 | 关键查询/写入 | 分页与筛选 | 幂等 | 权限与审计 | 错误处理 |
|------|----------|---------------|------------|------|------------|----------|
| 公文台账查询 | `GET /documents` `GET /documents/{id}` | 查询收文/发文主档、详情、流程摘要、签章与传阅摘要。 | 支持 `page`、`size`、`sort`；筛选 `documentType`、`status`、`documentNo`、`title`、`sponsorOrgId`、`drafterId`、`confidentialityLevel`、`issuedAt`。 | 查询天然幂等。 | 按组织、岗位、密级和数据权限控制可见范围；列表与详情访问写审计日志。 | `403` 无可见范围，`404` 公文不存在。 |
| 公文草稿与提交 | `POST /documents` `PUT /documents/{id}` `POST /documents/{id}/submit` `POST /documents/{id}/void` | 创建/修改草稿，提交审批，作废未生效公文。 | 写接口不分页；查询回显按单据主键。 | 创建/提交要求 `Idempotency-Key`；同一公文重复提交按 `documentId + processTemplateId` 去重。 | 仅拟稿人、主办组织文书或授权管理员可写；保存、提交、作废均落审计。 | `400` 参数非法，`409` 状态冲突或文号重复，`422` 缺正文/附件引用。 |
| 传阅与签章 | `POST /documents/{id}/circulations` `POST /documents/{id}/circulations/{recordId}/read` `POST /documents/{id}/seal-records` `GET /documents/{id}/seal-records` | 发起传阅、确认阅读、登记签章记录、查询签章历史。 | 传阅记录支持按 `receiverId`、`readStatus`、`receivedAt` 查询。 | 阅读确认按 `recordId` 幂等；签章登记按 `documentId + sealType + sealedAt` 去重。 | 仅公文责任人或被授权文书可发起传阅/登记签章；所有变更保留操作人、时间、身份上下文。 | `403` 越权传阅，`404` 记录不存在，`409` 已归档后禁止新增签章。 |
| 归档与审批回写 | `POST /documents/{id}/archive` `POST /documents/process-callback` | 归档生效公文，接收 `02` 流程完成/终止回调并回写业务状态。 | 归档列表支持 `status`、`archivedAt`、`sponsorOrgId`、`documentType` 筛选。 | 回调按 `processInstanceId + callbackState` 幂等；归档按 `documentId` 幂等。 | 归档仅档案管理员或规则授权角色可执行；回调接口仅系统间调用并写系统审计。 | `409` 非生效态禁止归档，`422` 流程实例状态不匹配，`500` 回写失败需告警。 |

## 通用约束

| 主题 | 说明 |
|------|------|
| 认证与权限 | 统一复用 `01` 身份上下文与数据权限，不在公文模块单独维护账号体系。 |
| 审批接入 | 需要审批的写操作只提交到 `02`，公文模块只保存 `processInstanceId` 与审批摘要。 |
| 审计要求 | 草稿保存、提交、作废、传阅、签章、归档、回调处理均必须记录审计日志。 |
| 错误口径 | 统一使用参数错误、权限错误、对象不存在、状态冲突、依赖失败五类错误语义。 |
| 外部开放 | 对外开放 API 与第三方同步统一经 `07` 暴露，本模块不直接对外提供私有接口。 |
