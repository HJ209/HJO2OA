import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import {
  useEventBusDeliveryStatus,
  useEventBusSubscriptions,
} from '@/features/infra-admin/hooks/use-event-bus'

export default function EventBusPage(): ReactElement {
  const subscriptionsQuery = useEventBusSubscriptions({ page: 1, size: 20 })
  const deliveryQuery = useEventBusDeliveryStatus({ page: 1, size: 20 })

  return (
    <div className="space-y-4">
      <InfraPageSection description="事件订阅方和消费端点。" title="事件订阅">
        <InfraTable
          columns={[
            { key: 'topic', title: '主题', render: (item) => item.topic },
            {
              key: 'consumer',
              title: '消费者',
              render: (item) => item.consumer,
            },
            { key: 'endpoint', title: '端点', render: (item) => item.endpoint },
            {
              key: 'enabled',
              title: '状态',
              render: (item) => (
                <StatusPill active={item.enabled}>
                  {item.enabled ? '启用' : '停用'}
                </StatusPill>
              ),
            },
          ]}
          getRowKey={(item) => item.id}
          isLoading={subscriptionsQuery.isLoading}
          items={subscriptionsQuery.data?.items ?? []}
        />
      </InfraPageSection>
      <InfraPageSection description="事件投递状态查询。" title="投递状态">
        <InfraTable
          columns={[
            { key: 'topic', title: '主题', render: (item) => item.topic },
            { key: 'eventId', title: '事件ID', render: (item) => item.eventId },
            {
              key: 'consumer',
              title: '消费者',
              render: (item) => item.consumer,
            },
            { key: 'status', title: '状态', render: (item) => item.status },
          ]}
          getRowKey={(item) => item.id}
          isLoading={deliveryQuery.isLoading}
          items={deliveryQuery.data?.items ?? []}
        />
      </InfraPageSection>
    </div>
  )
}
