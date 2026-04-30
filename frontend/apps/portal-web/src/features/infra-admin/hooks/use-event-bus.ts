import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventBusService } from '@/features/infra-admin/services/event-bus-service'
import type {
  EventBusQuery,
  EventBusReplayRequest,
} from '@/features/infra-admin/types/infra'

const EVENT_BUS_QUERY_KEY = ['infra', 'event-bus'] as const

export function useEventBusEvents(query?: EventBusQuery) {
  return useQuery({
    queryKey: [...EVENT_BUS_QUERY_KEY, 'events', query],
    queryFn: () => eventBusService.listEvents(query),
  })
}

export function useEventBusFailedEvents(query?: EventBusQuery) {
  return useQuery({
    queryKey: [...EVENT_BUS_QUERY_KEY, 'failed', query],
    queryFn: () => eventBusService.listFailedEvents(query),
  })
}

export function useEventBusDeadLetters(query?: EventBusQuery) {
  return useQuery({
    queryKey: [...EVENT_BUS_QUERY_KEY, 'dead-letters', query],
    queryFn: () => eventBusService.listDeadLetters(query),
  })
}

export function useEventBusEventDetail(eventId?: string) {
  return useQuery({
    enabled: Boolean(eventId),
    queryKey: [...EVENT_BUS_QUERY_KEY, 'detail', eventId],
    queryFn: () => eventBusService.detail(eventId ?? ''),
  })
}

export function useEventBusStatistics() {
  return useQuery({
    queryKey: [...EVENT_BUS_QUERY_KEY, 'statistics'],
    queryFn: () => eventBusService.statistics(),
  })
}

export function useRetryEventBusEvent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ eventId, reason }: { eventId: string; reason: string }) =>
      eventBusService.retry(eventId, reason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EVENT_BUS_QUERY_KEY })
    },
  })
}

export function useDeadLetterEventBusEvent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ eventId, reason }: { eventId: string; reason: string }) =>
      eventBusService.deadLetter(eventId, reason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EVENT_BUS_QUERY_KEY })
    },
  })
}

export function useReplayEventBusEvents() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: EventBusReplayRequest) =>
      eventBusService.replay(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EVENT_BUS_QUERY_KEY })
    },
  })
}
