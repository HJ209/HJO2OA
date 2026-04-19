# data-permission 前端契约

## 页面清单

| 页面/组件 | 端别 | 页面职责 | 备注 |
|-----------|------|----------|------|
| 行级权限配置页 | 管理端 | 配置授权主体、业务对象、范围类型和条件表达式 | 支持策略列表与表单联动 |
| 字段级权限配置页 | 管理端 | 配置字段动作矩阵和使用场景 | 推荐矩阵化展示字段动作 |
| 权限预览页 | 管理端 | 选择人员或身份预览最终行范围和字段矩阵 | 必须展示解释链 |
| 权限模板管理页 | 管理端 | 维护模板、应用模板和查看模板内容 | 模板是配置快照 |

## 页面职责

| 页面/组件 | 关键职责 | 依赖接口 |
|-----------|----------|----------|
| 行级权限配置页 | 查询、创建、编辑、删除行级策略 | `GET /data-permissions/row-policies`、`POST /data-permissions/row-policies`、`PUT /data-permissions/row-policies/{policyId}`、`DELETE /data-permissions/row-policies/{policyId}` |
| 字段级权限配置页 | 查询、创建、编辑、删除字段策略 | `GET /data-permissions/field-policies`、`POST /data-permissions/field-policies`、`PUT /data-permissions/field-policies/{policyId}`、`DELETE /data-permissions/field-policies/{policyId}` |
| 权限预览页 | 展示指定人员或身份的最终裁剪结果和冲突解释 | `POST /data-permissions/preview` |
| 权限模板管理页 | 展示模板、维护模板、应用模板 | `GET /data-permissions/templates`、`POST /data-permissions/templates`、`POST /data-permissions/templates/{templateId}/apply` |

## 关键交互

- 行级权限页应把授权主体、业务对象、范围类型、优先级和效果放在同一行，便于横向比较。
- 条件表达式编辑器必须提供基础校验和示例，避免管理员直接输入无法解析的表达式。
- 字段级权限页建议采用“字段 x 动作”矩阵，直观看到 `VISIBLE/EDITABLE/EXPORTABLE/DESENSITIZED/HIDDEN` 组合。
- 权限预览页必须同时显示最终结果和命中策略解释链，不能只显示“有权限/无权限”。
- 模板应用前应预览将新增或覆盖的策略数量，避免误覆盖生产配置。

## 权限与状态约束

- 页面权限至少区分 `data-permission.read`、`data-permission.write`、`data-permission.preview`、`data-permission.template-write`。
- 没有写权限的用户可查看策略和预览结果，但不能编辑、删除或应用模板。
- 预览页必须标明当前结果基于哪个身份上下文、哪些角色和哪些策略命中，便于审计。
- 前端不得把资源权限配置入口混入本模块页面，避免管理员误将菜单权限当成数据权限设置。

## 身份切换、移动端与管理端差异

- 本模块仅提供管理端配置与预览页面，不提供门户端或移动端配置入口。
- 身份切换不会直接改变配置页结构，但预览页应允许按当前身份或指定人员重新计算。
- 门户端和移动端只消费最终字段矩阵和数据范围条件，不展示策略配置细节。
