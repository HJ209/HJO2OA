import { useQuery } from '@tanstack/react-query'
import { eventBusService } from '@/features/infra-admin/services/event-bus-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useEventBusSubscriptions(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'event-bus-subscriptions', query],
    queryFn: () => eventBusService.listSubscriptions(query),
  })
}

export function useEventBusDeliveryStatus(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'event-bus-delivery-status', query],
    queryFn: () => eventBusService.listDeliveryStatus(query),
  })
}
