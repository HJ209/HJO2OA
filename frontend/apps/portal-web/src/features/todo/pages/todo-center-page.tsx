import type { ReactElement } from 'react'
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
import { useTodoStore } from '@/features/todo/stores/todo-store'
import type { TodoSortOption, TodoTab } from '@/features/todo/types/todo'
import { cn } from '@/utils/cn'

const COPY = {
  titleKey: 'todo.center.title',
  titleText: '待办中心',
  descriptionKey: 'todo.center.description',
  descriptionText: '统一查看流程待办、已办、逾期任务和抄送通知。',
  sortText: '排序',
  tabs: {
    pending: '待办',
    completed: '已办',
    overdue: '逾期',
    copied: '抄送',
  },
} as const

const tabs: Array<{ value: TodoTab; label: string }> = [
  { value: 'pending', label: COPY.tabs.pending },
  { value: 'completed', label: COPY.tabs.completed },
  { value: 'overdue', label: COPY.tabs.overdue },
  { value: 'copied', label: COPY.tabs.copied },
]

const sortOptions: Array<{
  value: string
  label: string
  sort: TodoSortOption
}> = [
  {
    value: 'createdAt-desc',
    label: '创建时间倒序',
    sort: { field: 'createdAt', direction: 'desc' },
  },
  {
    value: 'dueTime-asc',
    label: '截止时间正序',
    sort: { field: 'dueTime', direction: 'asc' },
  },
  {
    value: 'urgency-desc',
    label: '紧急程度倒序',
    sort: { field: 'urgency', direction: 'desc' },
  },
]

function renderTabPanel(activeTab: TodoTab): ReactElement {
  if (activeTab === 'copied') {
    return <CopiedTodoList />
  }

  return <TodoList tab={activeTab} />
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
              {COPY.titleText}
            </CardTitle>
            <CardDescription className="mt-2">
              {COPY.descriptionText}
            </CardDescription>
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <span>{COPY.sortText}</span>
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
          aria-label={COPY.titleText}
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
