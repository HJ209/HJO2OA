import { create } from 'zustand'
import type { TodoSortOption, TodoTab } from '@/features/todo/types/todo'

interface TodoStoreState {
  activeTab: TodoTab
  sort: TodoSortOption
  setActiveTab: (tab: TodoTab) => void
  setSort: (sort: TodoSortOption) => void
  reset: () => void
}

export const defaultTodoSort: TodoSortOption = {
  field: 'createdAt',
  direction: 'desc',
}

export const useTodoStore = create<TodoStoreState>((set) => ({
  activeTab: 'pending',
  sort: defaultTodoSort,
  setActiveTab: (activeTab) => set({ activeTab }),
  setSort: (sort) => set({ sort }),
  reset: () => set({ activeTab: 'pending', sort: defaultTodoSort }),
}))
