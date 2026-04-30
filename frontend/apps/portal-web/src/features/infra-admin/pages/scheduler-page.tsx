import { useMemo, useState, type ReactElement } from 'react'
import { Ban, CheckCircle2, Eye, Pause, RotateCcw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { SchedulerTriggerButton } from '@/features/infra-admin/components/scheduler-trigger-button'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import {
  useRetrySchedulerExecution,
  useSchedulerExecutions,
  useSchedulerJobStateAction,
  useSchedulerTasks,
  useTriggerSchedulerTask,
} from '@/features/infra-admin/hooks/use-scheduler'
import type { SchedulerExecution } from '@/features/infra-admin/types/infra'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function SchedulerPage(): ReactElement {
  const [selectedJobId, setSelectedJobId] = useState<string | undefined>()
  const [selectedExecution, setSelectedExecution] =
    useState<SchedulerExecution | null>(null)
  const tasksQuery = useSchedulerTasks({ page: 1, size: 20 })
  const executionsQuery = useSchedulerExecutions({
    page: 1,
    size: 20,
    jobId: selectedJobId,
  })
  const triggerMutation = useTriggerSchedulerTask()
  const retryMutation = useRetrySchedulerExecution()
  const enableMutation = useSchedulerJobStateAction('enable')
  const pauseMutation = useSchedulerJobStateAction('pause')
  const resumeMutation = useSchedulerJobStateAction('resume')
  const disableMutation = useSchedulerJobStateAction('disable')
  const jobsById = useMemo(
    () =>
      new Map((tasksQuery.data?.items ?? []).map((task) => [task.id, task])),
    [tasksQuery.data?.items],
  )

  function selectExecution(execution: SchedulerExecution): void {
    setSelectedExecution(execution)
    setSelectedJobId(execution.scheduledJobId)
  }

  return (
    <InfraPageSection
      description="Cron jobs, manual runs, retries, concurrency state, and execution history."
      title="Scheduler"
    >
      <div className="space-y-6">
        <InfraTable
          columns={[
            {
              key: 'job',
              title: 'Job',
              render: (item) => (
                <div>
                  <div className="font-medium text-slate-900">{item.name}</div>
                  <div className="text-xs text-slate-500">{item.jobCode}</div>
                </div>
              ),
            },
            {
              key: 'handler',
              title: 'Handler',
              render: (item) => item.handlerName,
            },
            { key: 'cron', title: 'Cron', render: (item) => item.cron },
            {
              key: 'policy',
              title: 'Policy',
              render: (item) => item.concurrencyPolicy,
            },
            {
              key: 'status',
              title: 'Status',
              render: (item) => (
                <StatusPill active={item.status === 'enabled'}>
                  {item.status}
                </StatusPill>
              ),
            },
            {
              key: 'updatedAt',
              title: 'Updated',
              render: (item) => formatUtcToUserTimezone(item.updatedAt),
            },
            {
              key: 'actions',
              title: 'Actions',
              render: (item) => (
                <div className="flex flex-wrap gap-2">
                  <Button
                    onClick={() => setSelectedJobId(item.id)}
                    size="sm"
                    variant="outline"
                  >
                    <Eye className="h-4 w-4" />
                    History
                  </Button>
                  <SchedulerTriggerButton
                    isLoading={triggerMutation.isPending}
                    onTrigger={(taskId) => triggerMutation.mutate(taskId)}
                    taskId={item.id}
                  />
                  {item.status === 'enabled' ? (
                    <Button
                      disabled={pauseMutation.isPending}
                      onClick={() => pauseMutation.mutate(item.id)}
                      size="sm"
                      variant="outline"
                    >
                      <Pause className="h-4 w-4" />
                      Pause
                    </Button>
                  ) : null}
                  {item.status === 'paused' ? (
                    <Button
                      disabled={resumeMutation.isPending}
                      onClick={() => resumeMutation.mutate(item.id)}
                      size="sm"
                      variant="outline"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      Resume
                    </Button>
                  ) : null}
                  {item.status === 'disabled' ? (
                    <Button
                      disabled={enableMutation.isPending}
                      onClick={() => enableMutation.mutate(item.id)}
                      size="sm"
                      variant="outline"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      Enable
                    </Button>
                  ) : (
                    <Button
                      disabled={disableMutation.isPending}
                      onClick={() => disableMutation.mutate(item.id)}
                      size="sm"
                      variant="outline"
                    >
                      <Ban className="h-4 w-4" />
                      Disable
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
          getRowKey={(item) => item.id}
          isLoading={tasksQuery.isLoading}
          items={tasksQuery.data?.items ?? []}
        />

        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">
              Execution Records
            </h2>
            {selectedJobId ? (
              <Button
                onClick={() => {
                  setSelectedJobId(undefined)
                  setSelectedExecution(null)
                }}
                size="sm"
                variant="outline"
              >
                All Jobs
              </Button>
            ) : null}
          </div>
          <InfraTable
            columns={[
              {
                key: 'job',
                title: 'Job',
                render: (item) =>
                  jobsById.get(item.scheduledJobId)?.jobCode ??
                  item.scheduledJobId,
              },
              {
                key: 'source',
                title: 'Source',
                render: (item) => item.triggerSource,
              },
              {
                key: 'status',
                title: 'Status',
                render: (item) => (
                  <StatusPill active={item.executionStatus === 'SUCCESS'}>
                    {item.executionStatus}
                  </StatusPill>
                ),
              },
              {
                key: 'attempt',
                title: 'Attempt',
                render: (item) => `${item.attemptNo}/${item.maxAttempts}`,
              },
              {
                key: 'duration',
                title: 'Duration',
                render: (item) =>
                  item.durationMs == null ? '-' : `${item.durationMs} ms`,
              },
              {
                key: 'startedAt',
                title: 'Started',
                render: (item) => formatUtcToUserTimezone(item.startedAt),
              },
              {
                key: 'executionActions',
                title: 'Actions',
                render: (item) => (
                  <div className="flex flex-wrap gap-2">
                    <Button
                      onClick={() => selectExecution(item)}
                      size="sm"
                      variant="outline"
                    >
                      <Eye className="h-4 w-4" />
                      Detail
                    </Button>
                    {['FAILED', 'TIMEOUT'].includes(item.executionStatus) &&
                    item.attemptNo < item.maxAttempts ? (
                      <Button
                        disabled={retryMutation.isPending}
                        onClick={() => retryMutation.mutate(item.id)}
                        size="sm"
                        variant="outline"
                      >
                        <RotateCcw className="h-4 w-4" />
                        Retry
                      </Button>
                    ) : null}
                  </div>
                ),
              },
            ]}
            getRowKey={(item) => item.id}
            isLoading={executionsQuery.isLoading}
            items={executionsQuery.data?.items ?? []}
          />
        </div>

        {selectedExecution ? (
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-semibold text-slate-900">Execution Detail</h2>
              <Button
                onClick={() => setSelectedExecution(null)}
                size="sm"
                variant="outline"
              >
                Close
              </Button>
            </div>
            <dl className="grid gap-3 md:grid-cols-3">
              <div>
                <dt className="text-xs uppercase text-slate-500">ID</dt>
                <dd className="break-all">{selectedExecution.id}</dd>
              </div>
              <div>
                <dt className="text-xs uppercase text-slate-500">Status</dt>
                <dd>{selectedExecution.executionStatus}</dd>
              </div>
              <div>
                <dt className="text-xs uppercase text-slate-500">Request</dt>
                <dd className="break-all">
                  {selectedExecution.idempotencyKey ?? '-'}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase text-slate-500">Error</dt>
                <dd>{selectedExecution.errorCode ?? '-'}</dd>
              </div>
              <div className="md:col-span-2">
                <dt className="text-xs uppercase text-slate-500">Message</dt>
                <dd>{selectedExecution.errorMessage ?? '-'}</dd>
              </div>
            </dl>
            <pre className="mt-4 max-h-64 overflow-auto rounded bg-white p-3 text-xs text-slate-700">
              {selectedExecution.errorStack ??
                selectedExecution.executionLog ??
                selectedExecution.triggerContext ??
                'No detail payload'}
            </pre>
          </div>
        ) : null}
      </div>
    </InfraPageSection>
  )
}
