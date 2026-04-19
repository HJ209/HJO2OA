# 错误码体系领域事件

事件信封统一遵循 `D:\idea-workspace\local\HJO2OA\docs\contracts\unified-event-contract.md`。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `infra.error-code.updated` | 错误码定义新增、元数据调整、废弃或替代关系变更后 | `errorCodeId`、`code`、`moduleCode`、`changeType`、`severity`、`httpStatus`、`deprecated` | 通知前后端目录、网关映射和缓存刷新。 |

## 消费事件

| 事件类型 | 消费目的 | 关键载荷字段 | 约束 |
|----------|----------|--------------|------|
| `infra.i18n.bundle-updated` | 刷新错误码文档中的国际化消息预览 | `bundleCode`、`locale`、`version` | 仅更新文档投影和展示缓存，不修改错误码定义本身。 |

## 幂等、补偿与投影约束

- 错误码事件必须带齐统一事件信封字段，并包含 `code` 作为稳定业务键。
- 前端目录缓存、SDK 映射和文档页按 `eventId` 去重；同一 `code + changeType` 重复投递应幂等。
- 若下游刷新失败，补偿方式是重新查询错误码目录，不是修改已抛出的业务异常实例。
- 错误码目录、文档页面和使用统计都属于投影结果；`ErrorCodeDefinition` 才是统一定义真相源。
- 本模块不根据错误码事件反向处理业务异常，仅提供定义更新通知。
