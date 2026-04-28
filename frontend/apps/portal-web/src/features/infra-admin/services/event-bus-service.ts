import { get } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  EventBusDeliveryStatus,
  EventBusSubscription,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const SUBSCRIPTION_URL = '/v1/infra/event-bus/subscriptions'
const DELIVERY_URL = '/v1/infra/event-bus/delivery-status'

export const eventBusService = {
  listSubscriptions(
    query?: InfraListQuery,
  ): Promise<InfraPageData<EventBusSubscription>> {
    return get(SUBSCRIPTION_URL, { params: buildListParams(query) })
  },
  listDeliveryStatus(
    query?: InfraListQuery,
  ): Promise<InfraPageData<EventBusDeliveryStatus>> {
    return get(DELIVERY_URL, { params: buildListParams(query) })
  },
}
