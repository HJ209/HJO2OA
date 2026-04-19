# form-renderer API

`form-renderer` 本身无独立后端写模型，本文档约束的是渲染器所依赖的输入接口契约。接口仍然来自 `form`、`process`、`infra` 和 `org` 模块。

## 接口分组

| 分组 | 依赖接口 | 说明 |
|------|----------|------|
| 渲染协议读取 | `GET /api/v1/form/render-schemas/{code}/versions/{version}` | 获取标准表单渲染协议。 |
| 运行态上下文读取 | `GET /api/v1/process/tasks/{taskId}`、`GET /api/v1/process/instances/{instanceId}` | 获取当前模式、节点、已有表单值和只读信息。 |
| 辅助选择器与附件 | `GET /api/v1/infra/dictionaries/{code}/items`、`GET /api/v1/org/persons`、`GET /api/v1/org/organizations`、`POST /api/v1/infra/attachments` | 字典、组织/人员选择和附件上传。 |

## 关键查询/写入接口

| 方法 | 路径 | 提供方 | 用途 |
|------|------|--------|------|
| `GET` | `/api/v1/form/render-schemas/{code}/versions/{version}` | `form-metadata` | 渲染字段、布局、校验和字段权限映射。 |
| `GET` | `/api/v1/process/tasks/{taskId}` | `process-instance` | 获取审批态任务上下文和当前表单值。 |
| `GET` | `/api/v1/process/instances/{instanceId}` | `process-instance` | 获取查看态实例上下文。 |
| `POST` | `/api/v1/infra/attachments` | `00-附件中心` | 上传小文件附件。 |

## 分页与筛选

- 渲染协议和实例上下文接口不分页。
- 人员、组织、字典选择器查询遵循统一分页与筛选协议，推荐使用：
  - `page`、`size`、`sort`
  - `filter[name]like`
  - `filter[status]`
  - `filter[organizationId]` / `filter[parentId]`
- 大数据量选择器必须走远程搜索，不允许前端一次性拉全量主数据。

## 幂等

- 渲染器自身不拥有独立写接口，因此不定义额外幂等规则。
- 附件上传、草稿保存、动作提交分别遵循其提供方模块的幂等规范。
- 前端需避免同一按钮连续点击导致重复提交，但最终幂等以服务端为准。

## 审批动作、草稿、归档边界

- 发起草稿保存与提交属于 `process-instance`。
- 审批动作执行属于 `action-engine`。
- 归档查询属于 `process-instance` / `todo-center`。
- 渲染器只负责把当前表单值和变更补丁交给上游接口，不承担状态推进。

## 权限与审计约束

- 渲染器只消费服务端返回的字段状态，不根据前端角色自行放开编辑权限。
- 所有真正落库的写操作审计由上游服务负责；前端只保留临时交互状态。
- 当实例/任务上下文缺失时，渲染器必须降级为只读或阻断提交，而不是猜测权限。

## 错误场景

| 场景 | 处理要求 |
|------|----------|
| 渲染协议不存在或版本未发布 | 页面阻断进入编辑态，提示重新选择流程或联系管理员。 |
| 返回了不支持的字段类型 | 渲染器显示占位错误组件并记录前端错误日志。 |
| 字段权限上下文缺失 | 页面降级为只读并阻止保存/提交。 |
| 字典、组织、人员选择器加载失败 | 允许重试，不得缓存过期选择结果继续提交。 |
| 附件上传失败 | 保留已填写表单输入，单独提示附件错误。 |
