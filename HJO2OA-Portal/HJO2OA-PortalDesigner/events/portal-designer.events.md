# portal-designer 事件合同

## 事件定位

`portal-designer` 是工具层，不发布独立领域事件。设计器的保存和发布动作最终都由 `portal-model` 对外发布正式领域事件；设计器自身只消费上游事件来刷新组件面板、版本状态和预览上下文。

## 发布事件

| 事件类型 | 说明 |
|----------|------|
| 无 | 设计器不是领域真相源，设计器动作只转化为 `portal-model` 的命令调用。 |

## 消费事件

| 事件类型 | 来源模块 | 处理动作 | 一期说明 |
|----------|----------|----------|----------|
| `portal.widget.updated` | `widget-config` | 刷新组件面板、属性 schema 和已引用组件的校验状态。 | 不自动改写画布，只提示刷新。 |
| `portal.widget.disabled` | `widget-config` | 将已停用卡片标记为不可继续使用，并提示修复已引用区域。 | 受影响布置需管理员手工处理。 |
| `portal.template.published` / `portal.template.deprecated` | `portal-model` | 刷新版本时间线、已发布标识和预览基线。 | 设计器仍以草稿为编辑对象。 |
| `portal.publication.activated` / `portal.publication.offlined` | `portal-model` | 刷新发布状态面板，显示当前线上版本。 | 不改变当前正在编辑的草稿内容。 |

## 幂等、快照刷新与投影说明

- `portal-designer` 不维护持久化投影，只维护组件面板、版本时间线和预览上下文等视图状态。
- 重复事件只覆盖最新视图状态，不应生成多份设计器草稿或重复执行发布。
- 设计器刷新组件面板和版本状态时，不直接触发 `portal-home` 页面刷新；正式刷新由 `portal-model`/`aggregation-api` 链路完成。
- 一期不实现多用户协同事件广播，设计器并发冲突通过保存接口的版本控制处理。
