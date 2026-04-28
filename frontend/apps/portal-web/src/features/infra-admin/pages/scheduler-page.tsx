import type { ReactElement } from 'react'
import { SchedulerTriggerButton } from '@/features/infra-admin/components/scheduler-trigger-button'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import {
  useSchedulerTasks,
  useTriggerSchedulerTask,
} from '@/features/infra-admin/hooks/use-scheduler'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function SchedulerPage(): ReactElement {
  const query = useSchedulerTasks({ page: 1, size: 20 })
  const triggerMutation = useTriggerSchedulerTask()

  return (
    <InfraPageSection description="定时任务配置和手动触发。" title="定时任务">
      <InfraTable
        columns={[
          { key: 'name', title: '任务', render: (item) => item.name },
          { key: 'cron', title: 'Cron', render: (item) => item.cron },
          {
            key: 'status',
            title: '状态',
            render: (item) => (
              <StatusPill active={item.status === 'enabled'}>
                {item.status}
              </StatusPill>
            ),
          },
          {
            key: 'nextRunAt',
            title: '下次执行',
            render: (item) => formatUtcToUserTimezone(item.nextRunAt),
          },
          {
            key: 'actions',
            title: '操作',
            render: (item) => (
              <SchedulerTriggerButton
                isLoading={triggerMutation.isPending}
                onTrigger={(taskId) => triggerMutation.mutate(taskId)}
                taskId={item.id}
              />
            ),
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
