import type { ReactElement } from 'react'
import { useQuery } from '@tanstack/react-query'
import { CheckSquare, ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useTodoCounts } from '@/features/todo/hooks/use-todo-counts'
import { CopiedTodoList } from '@/features/todo/pages/copied-todo-list'
import { TodoCountsBar } from '@/features/todo/pages/todo-counts-bar'
import { TodoList } from '@/features/todo/pages/todo-list'
import {
  getArchivedProcesses,
  getDraftProcesses,
  getInitiatedProcesses,
} from '@/features/todo/services/todo-service'
import { useTodoStore } from '@/features/todo/stores/todo-store'
import type {
  ArchiveProcessSummary,
  DraftProcessSummary,
  InitiatedProcessSummary,
  TodoItemTab,
  TodoSortOption,
  TodoTab,
} from '@/features/todo/types/todo'
import { cn } from '@/utils/cn'

type ProcessListItem =
  | InitiatedProcessSummary
  | DraftProcessSummary
  | ArchiveProcessSummary

const tabs: Array<{ value: TodoTab; label: string }> = [
  { value: 'pending', label: 'Pending' },
  { value: 'completed', label: 'Completed' },
  { value: 'overdue', label: 'Overdue' },
  { value: 'copied', label: 'Copied' },
  { value: 'initiated', label: 'Initiated' },
  { value: 'drafts', label: 'Drafts' },
  { value: 'archives', label: 'Archives' },
]

const sortOptions: Array<{
  value: string
  label: string
  sort: TodoSortOption
}> = [
  {
    value: 'createdAt-desc',
    label: 'Created time desc',
    sort: { field: 'createdAt', direction: 'desc' },
  },
  {
    value: 'dueTime-asc',
    label: 'Due time asc',
    sort: { field: 'dueTime', direction: 'asc' },
  },
  {
    value: 'urgency-desc',
    label: 'Urgency desc',
    sort: { field: 'urgency', direction: 'desc' },
  },
]

function isTodoItemTab(tab: TodoTab): tab is TodoItemTab {
  return tab === 'pending' || tab === 'completed' || tab === 'overdue'
}

function processKey(item: ProcessListItem): string {
  return 'submissionId' in item ? item.submissionId : item.instanceId
}

function ProcessListRow({ item }: { item: ProcessListItem }): ReactElement {
  if ('submissionId' in item) {
    return (
      <article className="border-b border-slate-100 px-5 py-4 last:border-b-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-semibold text-slate-950">{item.metadataCode}</h3>
          <span className="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600">
            v{item.metadataVersion}
          </span>
        </div>
        <p className="mt-1 text-sm text-slate-500">
          {item.submissionId} / {item.nodeId ?? '-'}
        </p>
      </article>
    )
  }

  return (
    <article className="border-b border-slate-100 px-5 py-4 last:border-b-0">
      <div className="flex flex-wrap items-center gap-2">
        <h3 className="font-semibold text-slate-950">{item.title}</h3>
        <span className="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600">
          {item.status}
        </span>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        {item.definitionCode} / {item.category ?? '-'} / {item.instanceId}
      </p>
    </article>
  )
}

function ProcessList({
  type,
}: {
  type: 'initiated' | 'drafts' | 'archives'
}): ReactElement {
  const query = useQuery({
    queryKey: ['todo-processes', type],
    queryFn: async (): Promise<ProcessListItem[]> => {
      if (type === 'initiated') {
        return getInitiatedProcesses()
      }

      if (type === 'drafts') {
        return getDraftProcesses()
      }

      return getArchivedProcesses()
    },
  })

  if (query.isLoading) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white p-5 text-sm text-slate-500">
        Loading...
      </div>
    )
  }

  if (query.isError) {
    return (
      <div className="rounded-lg border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
        Failed to load process list.
      </div>
    )
  }

  const items = query.data ?? []

  if (items.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">
        No records.
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      {items.map((item) => (
        <ProcessListRow item={item} key={processKey(item)} />
      ))}
    </div>
  )
}

function renderTabPanel(activeTab: TodoTab): ReactElement {
  if (activeTab === 'copied') {
    return <CopiedTodoList />
  }

  if (isTodoItemTab(activeTab)) {
    return <TodoList tab={activeTab} />
  }

  return <ProcessList type={activeTab} />
}

export default function TodoCenterPage(): ReactElement {
  const { data: counts, isLoading: countsLoading } = useTodoCounts()
  const activeTab = useTodoStore((state) => state.activeTab)
  const setActiveTab = useTodoStore((state) => state.setActiveTab)
  const sort = useTodoStore((state) => state.sort)
  const setSort = useTodoStore((state) => state.setSort)
  const sortValue = `${sort.field}-${sort.direction}`

  return (
    <div className="space-y-6">
      <Card className="rounded-lg">
        <CardHeader className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <CardTitle className="flex items-center gap-2 text-2xl">
              <CheckSquare className="h-6 w-6 text-sky-600" />
              Todo center
            </CardTitle>
            <CardDescription className="mt-2">
              Pending, completed, copied, reminders, drafts and process
              archives.
            </CardDescription>
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <span>Sort</span>
            <span className="relative">
              <select
                className="h-10 appearance-none rounded-lg border border-slate-200 bg-white px-3 pr-9 text-sm text-slate-900 shadow-sm outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-100"
                onChange={(event) => {
                  const selected = sortOptions.find(
                    (item) => item.value === event.target.value,
                  )

                  if (selected) {
                    setSort(selected.sort)
                  }
                }}
                value={sortValue}
              >
                {sortOptions.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-2.5 h-5 w-5 text-slate-400" />
            </span>
          </label>
        </CardHeader>
        <CardContent>
          <TodoCountsBar counts={counts} isLoading={countsLoading} />
        </CardContent>
      </Card>

      <div className="grid gap-4 lg:grid-cols-[13rem_1fr]">
        <div
          aria-label="Todo center"
          className="flex gap-2 overflow-x-auto rounded-lg border border-slate-200 bg-white p-2 shadow-sm lg:flex-col lg:overflow-visible"
          role="tablist"
        >
          {tabs.map((tab) => (
            <Button
              aria-controls={`todo-panel-${tab.value}`}
              aria-selected={activeTab === tab.value}
              className={cn(
                'shrink-0 justify-start rounded-lg',
                activeTab === tab.value
                  ? 'bg-sky-600 text-white hover:bg-sky-700'
                  : 'text-slate-600',
              )}
              id={`todo-tab-${tab.value}`}
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              role="tab"
              variant={activeTab === tab.value ? 'default' : 'ghost'}
            >
              {tab.label}
            </Button>
          ))}
        </div>

        <section
          aria-labelledby={`todo-tab-${activeTab}`}
          id={`todo-panel-${activeTab}`}
          role="tabpanel"
        >
          {renderTabPanel(activeTab)}
        </section>
      </div>
    </div>
  )
}
