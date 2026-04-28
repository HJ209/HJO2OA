import type { ReactElement } from 'react'
import { BellRing, Inbox } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import type { MessageNotification } from '@/types/domain'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const COPY = {
  titleKey: 'message.center.title',
  titleText: '消息中心',
  descriptionKey: 'message.center.description',
  descriptionText: '统一查看系统通知、审批提醒和业务播报。',
} as const

const notifications: MessageNotification[] = [
  {
    id: 'msg-001',
    title: '门户首页已完成 SPA 改造',
    summary:
      '当前门户端已切换为嵌套路由与应用壳布局，可继续扩展 feature 页面。',
    category: 'SYSTEM',
    createdAtUtc: '2026-04-28T02:50:00.000Z',
    read: false,
  },
  {
    id: 'msg-002',
    title: '待办审批有 2 条高优提醒',
    summary: '建议优先处理跨部门协同和门户发布审批事项。',
    category: 'APPROVAL',
    createdAtUtc: '2026-04-28T01:15:00.000Z',
    read: false,
  },
  {
    id: 'msg-003',
    title: '组织同步任务已完成',
    summary: '身份上下文缓存已刷新，可切换身份查看最新组织与角色。',
    category: 'NOTICE',
    createdAtUtc: '2026-04-27T22:40:00.000Z',
    read: true,
  },
]

export default function MessageCenterPage(): ReactElement {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>{COPY.titleText}</Badge>
          <CardTitle className="mt-3 flex items-center gap-2 text-2xl">
            <Inbox className="h-6 w-6 text-sky-600" />
            {COPY.titleText}
          </CardTitle>
          <CardDescription className="mt-2 text-base">
            {COPY.descriptionText}
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-4">
        {notifications.map((notification) => (
          <Card key={notification.id}>
            <CardContent className="flex flex-col gap-4 p-6 md:flex-row md:items-center md:justify-between">
              <div className="space-y-3">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-semibold text-slate-950">
                    {notification.title}
                  </h2>
                  <Badge variant={notification.read ? 'secondary' : 'default'}>
                    {notification.category}
                  </Badge>
                </div>
                <p className="text-sm text-slate-500">{notification.summary}</p>
              </div>
              <div className="flex items-center gap-3 text-sm text-slate-500">
                <BellRing
                  className={`h-5 w-5 ${notification.read ? 'text-slate-300' : 'text-sky-500'}`}
                />
                <span>
                  {formatUtcToUserTimezone(notification.createdAtUtc)}
                </span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
