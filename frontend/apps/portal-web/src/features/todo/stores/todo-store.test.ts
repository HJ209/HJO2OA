import { afterEach, describe, expect, it } from 'vitest'
import {
  defaultTodoSort,
  useTodoStore,
} from '@/features/todo/stores/todo-store'

afterEach(() => {
  useTodoStore.getState().reset()
})

describe('todo-store', () => {
  it('updates active tab', () => {
    useTodoStore.getState().setActiveTab('copied')

    expect(useTodoStore.getState().activeTab).toBe('copied')
  })

  it('updates and resets sort option', () => {
    useTodoStore.getState().setSort({ field: 'dueTime', direction: 'asc' })

    expect(useTodoStore.getState().sort).toEqual({
      field: 'dueTime',
      direction: 'asc',
    })

    useTodoStore.getState().reset()

    expect(useTodoStore.getState().sort).toEqual(defaultTodoSort)
    expect(useTodoStore.getState().activeTab).toBe('pending')
  })
})
