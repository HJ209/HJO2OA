import type { ReactElement } from 'react'
import { Play } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { INFRA_COPY } from '@/features/infra-admin/infra-copy'

export interface SchedulerTriggerButtonProps {
  taskId: string
  isLoading?: boolean
  onTrigger: (taskId: string) => void
}

export function SchedulerTriggerButton({
  taskId,
  isLoading = false,
  onTrigger,
}: SchedulerTriggerButtonProps): ReactElement {
  return (
    <Button
      disabled={isLoading}
      onClick={() => onTrigger(taskId)}
      size="sm"
      variant="outline"
    >
      <Play className="h-4 w-4" />
      {isLoading ? INFRA_COPY.action.triggering : INFRA_COPY.action.trigger}
    </Button>
  )
}
