import { useQuery } from '@tanstack/react-query'
import {
  getPushPreference,
  listChannelEndpoints,
  listMessageTemplates,
  listMobileDevices,
  listRetryableDeliveryTasks,
  listRoutingPolicies,
  listSubscriptionPreferences,
  listSubscriptionRules,
} from '@/features/messages/services/message-service'
import type {
  ChannelEndpoint,
  DeliveryTask,
  MessageTemplate,
  MobileDeviceBinding,
  PushPreference,
  RoutingPolicy,
  SubscriptionPreference,
  SubscriptionRule,
} from '@/features/messages/types/message'

export const messageConfigQueryKeys = {
  all: ['messages', 'config'] as const,
  templates: () => [...messageConfigQueryKeys.all, 'templates'] as const,
  endpoints: () => [...messageConfigQueryKeys.all, 'endpoints'] as const,
  routingPolicies: () =>
    [...messageConfigQueryKeys.all, 'routing-policies'] as const,
  retryableTasks: () =>
    [...messageConfigQueryKeys.all, 'retryable-tasks'] as const,
  subscriptionRules: () =>
    [...messageConfigQueryKeys.all, 'subscription-rules'] as const,
  subscriptionPreferences: () =>
    [...messageConfigQueryKeys.all, 'subscription-preferences'] as const,
  pushPreference: () =>
    [...messageConfigQueryKeys.all, 'push-preference'] as const,
  mobileDevices: () =>
    [...messageConfigQueryKeys.all, 'mobile-devices'] as const,
}

export function useMessageTemplatesQuery() {
  return useQuery<MessageTemplate[]>({
    queryKey: messageConfigQueryKeys.templates(),
    queryFn: listMessageTemplates,
    staleTime: 15000,
  })
}

export function useChannelEndpointsQuery() {
  return useQuery<ChannelEndpoint[]>({
    queryKey: messageConfigQueryKeys.endpoints(),
    queryFn: listChannelEndpoints,
    staleTime: 15000,
  })
}

export function useRoutingPoliciesQuery() {
  return useQuery<RoutingPolicy[]>({
    queryKey: messageConfigQueryKeys.routingPolicies(),
    queryFn: listRoutingPolicies,
    staleTime: 15000,
  })
}

export function useRetryableDeliveryTasksQuery() {
  return useQuery<DeliveryTask[]>({
    queryKey: messageConfigQueryKeys.retryableTasks(),
    queryFn: listRetryableDeliveryTasks,
    staleTime: 15000,
  })
}

export function useSubscriptionRulesQuery() {
  return useQuery<SubscriptionRule[]>({
    queryKey: messageConfigQueryKeys.subscriptionRules(),
    queryFn: listSubscriptionRules,
    staleTime: 15000,
  })
}

export function useSubscriptionPreferencesQuery() {
  return useQuery<SubscriptionPreference[]>({
    queryKey: messageConfigQueryKeys.subscriptionPreferences(),
    queryFn: listSubscriptionPreferences,
    staleTime: 15000,
  })
}

export function usePushPreferenceQuery() {
  return useQuery<PushPreference>({
    queryKey: messageConfigQueryKeys.pushPreference(),
    queryFn: getPushPreference,
    staleTime: 15000,
  })
}

export function useMobileDevicesQuery() {
  return useQuery<MobileDeviceBinding[]>({
    queryKey: messageConfigQueryKeys.mobileDevices(),
    queryFn: listMobileDevices,
    staleTime: 15000,
  })
}
