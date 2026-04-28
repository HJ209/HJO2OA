import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TodoCountsBar } from '@/features/todo/pages/todo-counts-bar'
import type { TodoCounts } from '@/features/todo/types/todo'

const counts: TodoCounts = {
  pendingCount: 3,
  completedCount: 4,
  overdueCount: 1,
  initiatedCount: 0,
  copiedUnreadCount: 2,
  draftCount: 0,
  archivedCount: 0,
}

describe('TodoCountsBar', () => {
  it('renders todo count metrics', () => {
    render(<TodoCountsBar counts={counts} />)

    expect(screen.getByText('待办数')).toBeInTheDocument()
    expect(screen.getByText('已办数')).toBeInTheDocument()
    expect(screen.getByText('逾期数')).toBeInTheDocument()
    expect(screen.getByText('抄送未读数')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })
})
