import { useMutation, useQuery } from '@tanstack/react-query'
import { schedulerService } from '@/features/infra-admin/services/scheduler-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useSchedulerTasks(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'scheduler', query],
    queryFn: () => schedulerService.list(query),
  })
}

export function useTriggerSchedulerTask() {
  return useMutation({
    mutationFn: (taskId: string) => schedulerService.trigger(taskId),
  })
}
