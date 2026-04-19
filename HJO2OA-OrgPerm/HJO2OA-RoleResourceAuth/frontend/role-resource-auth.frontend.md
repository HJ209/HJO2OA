# role-resource-auth 前端契约

## 页面清单

| 页面/组件 | 端别 | 页面职责 | 备注 |
|-----------|------|----------|------|
| 角色列表页 | 管理端 | 查询角色、分类筛选、状态治理和详情入口 | 角色管理主入口 |
| 角色新增/编辑弹窗 | 管理端 | 维护角色基础信息、类别、作用域和说明 | 不直接编辑资源树 |
| 岗位关联角色页 | 管理端 | 以岗位视角查看和配置绑定角色 | 强调岗位继承优先 |
| 人员直授角色页 | 管理端 | 维护例外直授角色和有效期 | 必须展示审计原因 |
| 资源授权树形配置页 | 管理端 | 以角色视角配置菜单、按钮、接口、页面、资源动作权限 | 支持树形或矩阵视图 |
| 角色模板管理页 | 管理端 | 维护模板、导入导出、模板应用 | 模板应用需返回影响摘要 |

## 页面职责

| 页面/组件 | 关键职责 | 依赖接口 |
|-----------|----------|----------|
| 角色列表页 | 展示角色列表、筛选条件、状态和绑定摘要 | `GET /roles`、`POST /roles/{roleId}/status` |
| 角色新增/编辑弹窗 | 创建或编辑角色基础字段 | `POST /roles`、`PUT /roles/{roleId}` |
| 岗位关联角色页 | 展示岗位已绑定角色、绑定和解绑操作 | `GET /positions/{positionId}/roles`、`POST /positions/{positionId}/roles`、`DELETE /positions/{positionId}/roles/{roleId}` |
| 人员直授角色页 | 展示直授角色、到期时间和撤销动作 | `GET /persons/{personId}/roles/direct`、`POST /persons/{personId}/roles/direct`、`POST /persons/{personId}/roles/{roleId}/revoke` |
| 资源授权树形配置页 | 展示和替换角色资源权限快照 | `GET /roles/{roleId}/resource-permissions`、`PUT /roles/{roleId}/resource-permissions` |
| 角色模板管理页 | 查询模板、创建模板、应用模板 | `GET /role-templates`、`POST /role-templates`、`POST /role-templates/{templateId}/apply` |

## 关键交互

- 角色列表页应突出显示角色类别、作用域、状态、绑定岗位数和直授人数，方便治理。
- 岗位关联角色页默认按岗位视角配置，人员直授页需显著提示“例外授权”，避免滥用。
- 资源授权页保存时建议采用整量替换模式，并在提交前展示新增、删除和变更动作摘要。
- 人员直授角色必须强制填写原因，并在页面上显示过期时间和撤销入口。
- 模板应用前应展示目标角色、将覆盖的资源权限数量和影响的岗位绑定数量摘要。

## 权限与状态约束

- 页面权限至少区分 `role.read`、`role.write`、`role.disable`、`role.bind-position`、`role.grant-person`、`resource-permission.write`、`role-template.write`。
- 停用角色仍可查看历史绑定和资源权限，但所有写操作默认禁用，除非先恢复角色状态。
- 没有岗位绑定权限的用户应只能查看岗位继承结果，不能修改绑定关系。
- 资源授权页必须阻止把数据权限概念混入角色资源权限配置界面。

## 身份切换、移动端与管理端差异

- 本模块仅提供管理端配置能力，不提供门户端和移动端授权维护入口。
- 当前管理员的身份切换只影响其可见角色范围和可配置资源范围，不改变已配置角色的业务语义。
- 门户和移动端只消费本模块产生的最终资源权限结果，不承载角色与授权编辑界面。
