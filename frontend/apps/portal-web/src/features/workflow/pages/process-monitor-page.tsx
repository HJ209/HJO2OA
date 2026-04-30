import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AlertTriangle,
  GitCommitVertical,
  RotateCw,
  ShieldAlert,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { processMonitorService } from '@/features/workflow/services/process-monitor-service'
import type {
  ExceptionInstance,
  MonitoredInstance,
  NodeTrail,
} from '@/features/workflow/types/process-monitor'

function InstanceRow({
  instance,
  selected,
  onSelect,
}: {
  instance: MonitoredInstance
  selected: boolean
  onSelect: () => void
}): ReactElement {
  return (
    <button
      className={`w-full border-b border-slate-100 px-4 py-3 text-left last:border-b-0 ${
        selected ? 'bg-sky-50' : 'bg-white hover:bg-slate-50'
      }`}
      onClick={onSelect}
      type="button"
    >
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-semibold text-slate-950">{instance.title}</span>
        <Badge>{instance.status}</Badge>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        {instance.definitionCode} / {instance.category ?? '-'} /{' '}
        {instance.instanceId}
      </p>
    </button>
  )
}

function ExceptionRow({ item }: { item: ExceptionInstance }): ReactElement {
  return (
    <article className="border-b border-slate-100 px-4 py-3 last:border-b-0">
      <div className="flex flex-wrap items-center gap-2">
        <AlertTriangle className="h-4 w-4 text-amber-500" />
        <span className="font-medium text-slate-950">{item.title}</span>
        <Badge variant="secondary">{item.exceptionType}</Badge>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        {item.definitionCode} / {item.exceptionMinutes} minutes
      </p>
    </article>
  )
}

function TrailList({ items }: { items: NodeTrail[] }): ReactElement {
  if (items.length === 0) {
    return <p className="p-4 text-sm text-slate-500">No trail records.</p>
  }

  return (
    <div className="divide-y divide-slate-100">
      {items.map((item) => (
        <article className="px-4 py-3 text-sm" key={item.taskId}>
          <div className="flex flex-wrap items-center gap-2">
            <GitCommitVertical className="h-4 w-4 text-sky-500" />
            <span className="font-medium text-slate-950">{item.nodeName}</span>
            <Badge>{item.taskStatus}</Badge>
          </div>
          <p className="mt-1 text-slate-500">
            {item.nodeId} / {item.assigneeId ?? '-'} /{' '}
            {item.lastActionName ?? '-'}
          </p>
        </article>
      ))}
    </div>
  )
}

export default function ProcessMonitorPage(): ReactElement {
  const queryClient = useQueryClient()
  const [tenantId, setTenantId] = useState(
    '11111111-1111-1111-1111-111111111111',
  )
  const [definitionCode, setDefinitionCode] = useState('')
  const [status, setStatus] = useState('RUNNING')
  const [selectedInstanceId, setSelectedInstanceId] = useState('')
  const [operatorId, setOperatorId] = useState(
    '22222222-2222-2222-2222-222222222222',
  )
  const [taskId, setTaskId] = useState('')
  const [targetAssigneeId, setTargetAssigneeId] = useState('')

  const params = useMemo(
    () => ({
      tenantId,
      definitionCode: definitionCode || undefined,
      status: status || undefined,
      limit: 50,
    }),
    [tenantId, definitionCode, status],
  )

  const instancesQuery = useQuery({
    queryKey: ['process-monitor', 'instances', params],
    queryFn: () => processMonitorService.getInstances(params),
  })

  const exceptionsQuery = useQuery({
    queryKey: ['process-monitor', 'exceptions', tenantId, definitionCode],
    queryFn: () =>
      processMonitorService.getExceptions({
        tenantId,
        definitionCode: definitionCode || undefined,
        stalledThresholdMinutes: 1440,
      }),
  })

  const trailQuery = useQuery({
    enabled: Boolean(tenantId && selectedInstanceId),
    queryKey: ['process-monitor', 'trail', tenantId, selectedInstanceId],
    queryFn: () =>
      processMonitorService.getNodeTrail(tenantId, selectedInstanceId),
  })

  const interventionMutation = useMutation({
    mutationFn: (
      actionType: 'SUSPEND' | 'RESUME' | 'TERMINATE' | 'REASSIGN_TASK',
    ) =>
      processMonitorService.intervene(selectedInstanceId, {
        tenantId,
        actionType,
        operatorId,
        taskId: actionType === 'REASSIGN_TASK' ? taskId : undefined,
        targetAssigneeId:
          actionType === 'REASSIGN_TASK' ? targetAssigneeId : undefined,
        reason: 'Manual monitor intervention',
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['process-monitor'] })
    },
  })

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-slate-100 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-base font-semibold text-slate-950">
              Process monitor
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Query instances, exceptions, node trail and apply administrator
              intervention.
            </p>
          </div>
          <Button
            onClick={() =>
              void queryClient.invalidateQueries({
                queryKey: ['process-monitor'],
              })
            }
            variant="outline"
          >
            <RotateCw className="h-4 w-4" />
            Refresh
          </Button>
        </div>
        <div className="grid gap-3 p-4 lg:grid-cols-4">
          <Input
            onChange={(event) => setTenantId(event.target.value)}
            value={tenantId}
          />
          <Input
            onChange={(event) => setDefinitionCode(event.target.value)}
            placeholder="definitionCode"
            value={definitionCode}
          />
          <Input
            onChange={(event) => setStatus(event.target.value)}
            value={status}
          />
          <Input
            onChange={(event) => setOperatorId(event.target.value)}
            value={operatorId}
          />
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_24rem]">
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-4 py-3 font-semibold text-slate-950">
            Instances
          </div>
          {(instancesQuery.data ?? []).map((instance) => (
            <InstanceRow
              instance={instance}
              key={instance.instanceId}
              onSelect={() => setSelectedInstanceId(instance.instanceId)}
              selected={selectedInstanceId === instance.instanceId}
            />
          ))}
          {instancesQuery.data?.length === 0 ? (
            <p className="p-4 text-sm text-slate-500">No instances.</p>
          ) : null}
        </div>

        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-3 font-semibold text-slate-950">
            <ShieldAlert className="h-4 w-4 text-amber-500" />
            Exceptions
          </div>
          {(exceptionsQuery.data ?? []).map((item) => (
            <ExceptionRow
              item={item}
              key={`${item.instanceId}-${item.exceptionType}`}
            />
          ))}
          {exceptionsQuery.data?.length === 0 ? (
            <p className="p-4 text-sm text-slate-500">No exceptions.</p>
          ) : null}
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_24rem]">
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-4 py-3 font-semibold text-slate-950">
            Node trail
          </div>
          <TrailList items={trailQuery.data ?? []} />
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="font-semibold text-slate-950">Intervention</h2>
          <div className="mt-3 grid gap-2">
            <Input
              onChange={(event) => setSelectedInstanceId(event.target.value)}
              placeholder="instanceId"
              value={selectedInstanceId}
            />
            <Input
              onChange={(event) => setTaskId(event.target.value)}
              value={taskId}
            />
            <Input
              onChange={(event) => setTargetAssigneeId(event.target.value)}
              value={targetAssigneeId}
            />
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {(['SUSPEND', 'RESUME', 'TERMINATE', 'REASSIGN_TASK'] as const).map(
              (actionType) => (
                <Button
                  disabled={
                    !selectedInstanceId || interventionMutation.isPending
                  }
                  key={actionType}
                  onClick={() => interventionMutation.mutate(actionType)}
                  size="sm"
                  variant="outline"
                  className={
                    actionType === 'TERMINATE'
                      ? 'border-rose-200 text-rose-700 hover:bg-rose-50'
                      : undefined
                  }
                >
                  {actionType}
                </Button>
              ),
            )}
          </div>
        </div>
      </section>

      {instancesQuery.error ||
      exceptionsQuery.error ||
      trailQuery.error ||
      interventionMutation.error ? (
        <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {String(
            instancesQuery.error ??
              exceptionsQuery.error ??
              trailQuery.error ??
              interventionMutation.error,
          )}
        </p>
      ) : null}
    </div>
  )
}
