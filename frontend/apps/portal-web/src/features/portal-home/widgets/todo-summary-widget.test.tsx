import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import TodoSummaryWidget from '@/features/portal-home/widgets/todo-summary-widget'

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

describe('TodoSummaryWidget', () => {
  it('renders pending, overdue, today due counts and entry action', () => {
    render(
      <MemoryRouter future={routerFuture}>
        <TodoSummaryWidget
          summary={{
            pendingCount: 12,
            overdueCount: 3,
            todayDueCount: 5,
            entryHref: '/todo',
          }}
        />
      </MemoryRouter>,
    )

    expect(screen.getByText('待办摘要')).toBeInTheDocument()
    expect(screen.getByText('待处理')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('已逾期')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('今日到期')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /进入待办中心/ })).toHaveAttribute(
      'href',
      '/todo',
    )
  })
})
