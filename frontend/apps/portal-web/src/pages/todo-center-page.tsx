import type { ReactElement } from 'react'
import { CheckCircle2, Clock3, ListTodo } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import type { TodoItemSummary } from '@/types/domain'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const COPY = {
  titleKey: 'todo.center.title',
  titleText: '待办中心',
  descriptionKey: 'todo.center.description',
  descriptionText: '统一处理流程待办、催办提醒与个人优先级工作项。',
  primaryActionKey: 'todo.center.primaryAction',
  primaryActionText: '批量处理占位',
} as const

const todoItems: TodoItemSummary[] = [
  {
    id: 'todo-001',
    title: '审批门户模板发布申请',
    status: 'PENDING',
    priority: 'HIGH',
    dueAtUtc: '2026-04-28T06:30:00.000Z',
    assigneeName: '门户管理员',
  },
  {
    id: 'todo-002',
    title: '核对身份上下文同步结果',
    status: 'IN_PROGRESS',
    priority: 'MEDIUM',
    dueAtUtc: '2026-04-28T10:00:00.000Z',
    assigneeName: '数字办公部',
  },
  {
    id: 'todo-003',
    title: '归档历史消息治理报告',
    status: 'COMPLETED',
    priority: 'LOW',
    assigneeName: '平台运营',
  },
]

function resolveBadgeVariant(
  priority: TodoItemSummary['priority'],
): 'default' | 'secondary' | 'success' {
  if (priority === 'HIGH') {
    return 'default'
  }

  if (priority === 'MEDIUM') {
    return 'secondary'
  }

  return 'success'
}

export default function TodoCenterPage(): ReactElement {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <Badge>{COPY.titleText}</Badge>
            <CardTitle className="mt-3 flex items-center gap-2 text-2xl">
              <ListTodo className="h-6 w-6 text-sky-600" />
              {COPY.titleText}
            </CardTitle>
            <CardDescription className="mt-2 text-base">
              {COPY.descriptionText}
            </CardDescription>
          </div>
          <Button>{COPY.primaryActionText}</Button>
        </CardHeader>
      </Card>

      <div className="grid gap-4">
        {todoItems.map((todoItem) => (
          <Card key={todoItem.id}>
            <CardContent className="flex flex-col gap-4 p-6 lg:flex-row lg:items-center lg:justify-between">
              <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-semibold text-slate-950">
                    {todoItem.title}
                  </h2>
                  <Badge variant={resolveBadgeVariant(todoItem.priority)}>
                    {todoItem.priority}
                  </Badge>
                </div>
                <p className="text-sm text-slate-500">
                  处理人：{todoItem.assigneeName}
                </p>
                <p className="text-sm text-slate-500">
                  截止时间：{formatUtcToUserTimezone(todoItem.dueAtUtc)}
                </p>
              </div>
              <div className="flex items-center gap-3 text-sm text-slate-500">
                {todoItem.status === 'COMPLETED' ? (
                  <CheckCircle2 className="h-5 w-5 text-emerald-500" />
                ) : (
                  <Clock3 className="h-5 w-5 text-amber-500" />
                )}
                <span>{todoItem.status}</span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
