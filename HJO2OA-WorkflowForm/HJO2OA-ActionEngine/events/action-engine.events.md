# action-engine 领域事件

## 发布事件

一期不发布独立的 `process.action.*` 领域事件。

- 动作执行结果已经通过 `process.task.completed`、`process.task.terminated`、`process.task.transferred` 和相关 `process.instance.*` 事件对外表达。
- 若再发布 `process.action.*`，会与任务/实例事件形成重复语义，增加下游消费复杂度。

## 消费事件

一期无强制消费事件。

- 动作引擎在执行时同步读取任务、实例、定义和表单上下文，不通过异步事件回填主状态。
- 后续若引入规则缓存预热，可由基础设施层处理，不改变当前契约。

## 投影、幂等与补偿

- `proc_task_action` 是本子模块的权威审计表，不是跨模块投影。
- 动作记录写入与上游任务状态推进必须在同一业务事务内保证一致性；任一失败都视为整次动作失败。
- 动作执行通过 `X-Idempotency-Key` 去重，确保重复点击不会生成重复动作记录。
- 无独立发布事件，因此不存在动作事件重放补偿；下游联动统一依赖 `process.task.*` 与 `process.instance.*`。
