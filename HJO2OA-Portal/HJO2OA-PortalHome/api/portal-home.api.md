# portal-home API 合同

## 当前已落地接口

- `GET /api/v1/portal/home/page?sceneType=...`
  返回指定场景的门页装配结果。
- `GET /api/v1/portal/home/refresh-state?sceneType=...`
  返回指定场景的刷新状态。

当前 page 装配会优先复用 `portal-model` 的 source template 解析结果；当可用的 source canvas 存在时，页面 layout、region 顺序和 card 顺序按 source canvas 装配；当 source state 或 canvas 缺失时，仍安全回退到静态模板骨架。

## API 定位

`portal-home` 不拥有独立领域主数据接口，本文件定义的是页面装配与前端集成所需的组合查询契约。其核心目标是把模板解析、个性化视图和聚合卡片拼装成页面渲染上下文。

## 接口分组

| 组合契约 | 依赖接口 | 合同要求 |
|----------|----------|----------|
| 门户页面装配上下文 | `portal-model` + `personalization` | 根据 `sceneType`、`clientType`、`pageCode` 获取最终页面结构和个性化结果。 |
| 首页卡片批量加载 | `aggregation-api` | 根据装配上下文批量请求首页卡片数据。 |
| 办公中心工作区加载 | `portal-model` + `aggregation-api` | 获取办公中心导航结构、分类标识和工作区卡片数据。 |
| 定向刷新与重装配 | `aggregation-api` + 前端状态管理 | 在身份切换、个性化保存或单卡刷新后只更新受影响区域。 |

## 边界口径

- 模板建模与发布归 `portal-model`，`portal-home` 只消费解析结果。
- 个性化配置归 `personalization`，`portal-home` 不保存用户偏好。
- 卡片数据归 `aggregation-api`，`portal-home` 不绕过聚合层直连待办、消息等业务接口。
- 本模块不建设独立门户业务 API，不承担设计器接口或开放 API 职责。

## 分页与筛选约定

| 场景 | 分页参数 | 主要筛选条件 |
|------|----------|--------------|
| 页面装配上下文 | 不分页 | `sceneType`、`clientType`、`pageCode` |
| 办公中心列表型区域 | 透传 `pageNo`、`pageSize` 给 `aggregation-api` | `categoryCode`、`keyword`、`sortBy` |
| 首页局部刷新 | 不分页 | `regionCode`、`cardType` |

## 缓存、新鲜度、权限与审计约束

| 主题 | 合同要求 |
|------|----------|
| 缓存 | 页面壳可短时缓存，但必须带上当前发布版本和个性化视图指纹；身份切换后立即失效。 |
| 新鲜度 | 个性化保存、发布切换、单卡刷新后，前端应触发定向重装配，不依赖 WebSocket 推送。 |
| 权限 | 仅允许当前登录身份读取自身可见的门户页面和办公中心数据。 |
| 审计 | 页面装配查询本身不做高频业务审计；真正的写操作审计由上游模块承担。 |

## 错误场景

| 场景 | 处理要求 |
|------|----------|
| 当前身份没有可用发布模板 | 返回空态/引导态，不伪造默认页面。 |
| `pageCode` 不存在或与当前模板不匹配 | 返回页面不存在错误。 |
| 卡片快照部分失败 | 允许局部降级，不阻塞页面其余区域渲染。 |
| 身份切换后仍使用旧页面壳 | 强制丢弃旧上下文并重新装配。 |
| 试图通过 portal-home 获取原始业务明细接口 | 拒绝受理，要求走 `aggregation-api` 或原业务模块。 |

## Published Snapshot Consumption Notes

- `portal-home` now resolves runtime structure from the active template's published canvas snapshot instead of the mutable draft canvas.
- After a template is published, continuing to save draft changes does not affect the current live page structure.
- Live structure changes only after the next successful publish of that template. When no published snapshot is available, `portal-home` falls back to the static skeleton template.
