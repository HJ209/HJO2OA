import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiClient from '@/services/api-client'
import {
  getCompletedTodos,
  getCopiedTodos,
  getOverdueTodos,
  getPendingTodos,
  getTodoCounts,
  markCopiedTodoRead,
} from '@/features/todo/services/todo-service'
import type {
  CopiedTodoSummary,
  TodoCounts,
  TodoItemSummary,
} from '@/features/todo/types/todo'

vi.mock('@/services/api-client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const todoItem: TodoItemSummary = {
  todoId: 'todo-001',
  taskId: 'task-001',
  instanceId: 'instance-001',
  assigneeId: 'assignment-001',
  type: 'APPROVAL',
  category: '流程审批',
  title: '审批采购申请',
  urgency: 'HIGH',
  status: 'PENDING',
  dueTime: '2026-04-28T02:00:00.000Z',
  createdAt: '2026-04-27T02:00:00.000Z',
}

const copiedTodo: CopiedTodoSummary = {
  todoId: 'copy-001',
  taskId: 'task-001',
  instanceId: 'instance-001',
  recipientAssignmentId: 'assignment-002',
  type: 'APPROVAL',
  category: '流程审批',
  title: '抄送采购申请',
  urgency: 'MEDIUM',
  readStatus: 'UNREAD',
  createdAt: '2026-04-27T02:00:00.000Z',
  updatedAt: '2026-04-27T02:00:00.000Z',
}

const counts: TodoCounts = {
  pendingCount: 3,
  completedCount: 4,
  overdueCount: 1,
  cancelledCount: 0,
  copiedUnreadCount: 2,
  copiedTotalCount: 5,
  total: 13,
}

beforeEach(() => {
  vi.mocked(apiClient.get).mockReset()
  vi.mocked(apiClient.post).mockReset()
})

describe('todo-service', () => {
  it('requests pending, completed and overdue todo lists', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [todoItem] })

    await expect(getPendingTodos()).resolves.toEqual([todoItem])
    await expect(getCompletedTodos()).resolves.toEqual([todoItem])
    await expect(getOverdueTodos()).resolves.toEqual([todoItem])

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/v1/todo/pending', {})
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/v1/todo/completed',
      {},
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(3, '/api/v1/todo/overdue', {})
  })

  it('serializes copied todo read status filter through pagination utility', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [copiedTodo] })

    await expect(getCopiedTodos('UNREAD')).resolves.toEqual([copiedTodo])

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/todo/copied', {
      params: new URLSearchParams('filter%5BreadStatus%5D=UNREAD'),
    })
  })

  it('marks copied todo as read with idempotency and dedupe config', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: copiedTodo })

    await expect(markCopiedTodoRead('copy-001')).resolves.toEqual(copiedTodo)

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v1/todo/copied/copy-001/read',
      {},
      expect.objectContaining({
        dedupeKey: 'todo:copied:read:copy-001',
        headers: expect.objectContaining({
          'X-Idempotency-Key': 'todo-copied-read-copy-001',
        }),
        idempotencyKey: 'todo-copied-read-copy-001',
      }),
    )
  })

  it('requests todo counts', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: counts })

    await expect(getTodoCounts()).resolves.toEqual(counts)

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/todo/counts', {})
  })
})
