import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  schedulerService,
  type SchedulerExecutionQuery,
} from '@/features/infra-admin/services/scheduler-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useSchedulerTasks(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'scheduler', query],
    queryFn: () => schedulerService.list(query),
  })
}

export function useTriggerSchedulerTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (taskId: string) => schedulerService.trigger(taskId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['infra', 'scheduler'] })
    },
  })
}

export function useSchedulerExecutions(query?: SchedulerExecutionQuery) {
  return useQuery({
    queryKey: ['infra', 'scheduler', 'executions', query],
    queryFn: () => schedulerService.listExecutions(query),
  })
}

export function useRetrySchedulerExecution() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (executionId: string) =>
      schedulerService.retryExecution(executionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['infra', 'scheduler'] })
    },
  })
}

export function useSchedulerJobStateAction(
  action: 'enable' | 'pause' | 'resume' | 'disable',
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (jobId: string) => schedulerService[action](jobId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['infra', 'scheduler'] })
    },
  })
}
