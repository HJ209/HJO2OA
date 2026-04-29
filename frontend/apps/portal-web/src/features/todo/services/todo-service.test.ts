import { beforeEach, describe, expect, it, vi } from 'vitest'
import { get, post } from '@/services/request'
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

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
}))

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

const copiedTodo: CopiedTodoSummary = {
  todoId: 'copy-001',
  taskId: 'task-001',
  instanceId: 'instance-001',
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
  initiatedCount: 0,
  copiedUnreadCount: 2,
  draftCount: 0,
  archivedCount: 0,
}

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)

beforeEach(() => {
  mockedGet.mockReset()
  mockedPost.mockReset()
})

describe('todo-service', () => {
  it('requests pending, completed and overdue todo lists', async () => {
    mockedGet.mockResolvedValue([todoItem])

    await expect(getPendingTodos()).resolves.toEqual([todoItem])
    await expect(getCompletedTodos()).resolves.toEqual([todoItem])
    await expect(getOverdueTodos()).resolves.toEqual([todoItem])

    expect(mockedGet).toHaveBeenNthCalledWith(1, '/v1/todo/pending')
    expect(mockedGet).toHaveBeenNthCalledWith(2, '/v1/todo/completed')
    expect(mockedGet).toHaveBeenNthCalledWith(3, '/v1/todo/overdue')
  })

  it('serializes copied todo read status filter through pagination utility', async () => {
    mockedGet.mockResolvedValue([copiedTodo])

    await expect(getCopiedTodos('UNREAD')).resolves.toEqual([copiedTodo])

    expect(mockedGet).toHaveBeenCalledWith('/v1/todo/copied', {
      params: new URLSearchParams('filter%5BreadStatus%5D=UNREAD'),
    })
  })

  it('marks copied todo as read with idempotency and dedupe config', async () => {
    mockedPost.mockResolvedValue(copiedTodo)

    await expect(markCopiedTodoRead('copy-001')).resolves.toEqual(copiedTodo)

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/todo/copied/copy-001/read',
      {},
      expect.objectContaining({
        dedupeKey: 'todo:copied:read:copy-001',
        idempotencyKey: 'todo-copied-read-copy-001',
      }),
    )
  })

  it('requests todo counts', async () => {
    mockedGet.mockResolvedValue(counts)

    await expect(getTodoCounts()).resolves.toEqual(counts)

    expect(mockedGet).toHaveBeenCalledWith('/v1/todo/counts')
  })
})
