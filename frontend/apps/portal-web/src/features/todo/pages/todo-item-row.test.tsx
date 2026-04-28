import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TodoItemRow } from '@/features/todo/pages/todo-item-row'
import type { TodoItemSummary } from '@/features/todo/types/todo'

const todoItem: TodoItemSummary = {
  todoId: 'todo-001',
  taskId: 'task-001',
  instanceId: 'instance-001',
  title: '审批采购申请',
  category: '流程审批',
  urgency: 'HIGH',
  status: 'PENDING',
  assigneeId: 'assignment-001',
  dueTime: '2026-04-28T02:00:00.000Z',
  createdAt: '2026-04-27T02:00:00.000Z',
}

describe('TodoItemRow', () => {
  it('renders todo summary fields', () => {
    render(<TodoItemRow item={todoItem} />)

    expect(screen.getByText('审批采购申请')).toBeInTheDocument()
    expect(screen.getByText(/流程审批/)).toBeInTheDocument()
    expect(screen.getByText('高')).toBeInTheDocument()
    expect(screen.getByText('待办')).toBeInTheDocument()
  })
})
