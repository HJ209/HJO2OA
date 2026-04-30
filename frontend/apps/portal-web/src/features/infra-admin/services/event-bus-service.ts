import { get, post } from '@/services/request'
import {
  buildIdempotencyKey,
  buildListParams,
} from '@/features/infra-admin/services/service-utils'
import type {
  EventBusEvent,
  EventBusEventDetail,
  EventBusQuery,
  EventBusReplayRequest,
  EventBusReplayResult,
  EventBusStatistics,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const EVENT_BUS_URL = '/v1/infra/event-bus'

function appendEventFilters(
  params: URLSearchParams,
  query: EventBusQuery = {},
): URLSearchParams {
  const filterKeys = [
    'eventId',
    'eventType',
    'aggregateType',
    'aggregateId',
    'tenantId',
    'traceId',
    'status',
    'occurredFrom',
    'occurredTo',
  ] as const

  for (const key of filterKeys) {
    const value = query[key]

    if (value) {
      params.set(key, value)
    }
  }

  return params
}

export const eventBusService = {
  listEvents(query: EventBusQuery = {}): Promise<InfraPageData<EventBusEvent>> {
    return get(`${EVENT_BUS_URL}/events`, {
      params: appendEventFilters(buildListParams(query), query),
    })
  },

  listFailedEvents(
    query: EventBusQuery = {},
  ): Promise<InfraPageData<EventBusEvent>> {
    return get(`${EVENT_BUS_URL}/events/failed`, {
      params: appendEventFilters(buildListParams(query), query),
    })
  },

  listDeadLetters(
    query: EventBusQuery = {},
  ): Promise<InfraPageData<EventBusEvent>> {
    return get(`${EVENT_BUS_URL}/dead-letters`, {
      params: appendEventFilters(buildListParams(query), query),
    })
  },

  detail(eventId: string): Promise<EventBusEventDetail> {
    return get(`${EVENT_BUS_URL}/events/${eventId}`)
  },

  retry(eventId: string, reason: string): Promise<EventBusEventDetail> {
    return post(
      `${EVENT_BUS_URL}/events/${eventId}/retry`,
      { reason },
      {
        dedupeKey: `event-bus:retry:${eventId}`,
        idempotencyKey: buildIdempotencyKey('event-bus:retry', eventId),
      },
    )
  },

  deadLetter(eventId: string, reason: string): Promise<EventBusEventDetail> {
    return post(
      `${EVENT_BUS_URL}/events/${eventId}/dead-letter`,
      { reason },
      {
        dedupeKey: `event-bus:dead-letter:${eventId}`,
        idempotencyKey: buildIdempotencyKey('event-bus:dead-letter', eventId),
      },
    )
  },

  replay(request: EventBusReplayRequest): Promise<EventBusReplayResult> {
    return post(`${EVENT_BUS_URL}/replay`, request, {
      dedupeKey: `event-bus:replay:${JSON.stringify(request)}`,
      idempotencyKey: buildIdempotencyKey('event-bus:replay', 'batch'),
    })
  },

  statistics(): Promise<EventBusStatistics> {
    return get(`${EVENT_BUS_URL}/statistics`)
  },
}
