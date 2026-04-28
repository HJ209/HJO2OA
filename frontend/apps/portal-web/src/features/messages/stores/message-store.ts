import { create } from 'zustand'
import type {
  MessageReadStatus,
  MessageType,
} from '@/features/messages/types/message'

export type MessageTypeFilter = MessageType | 'ALL'
export type MessageReadStatusFilter = MessageReadStatus | 'ALL'

interface MessageStoreState {
  type: MessageTypeFilter
  readStatus: MessageReadStatusFilter
  selectedMessageId: string | null
  setType: (type: MessageTypeFilter) => void
  setReadStatus: (readStatus: MessageReadStatusFilter) => void
  setSelectedMessageId: (messageId: string | null) => void
  resetFilters: () => void
}

export const DEFAULT_MESSAGE_FILTERS = {
  type: 'ALL',
  readStatus: 'ALL',
} as const

export const useMessageStore = create<MessageStoreState>((set) => ({
  ...DEFAULT_MESSAGE_FILTERS,
  selectedMessageId: null,
  setType: (type) => set({ type, selectedMessageId: null }),
  setReadStatus: (readStatus) => set({ readStatus, selectedMessageId: null }),
  setSelectedMessageId: (selectedMessageId) => set({ selectedMessageId }),
  resetFilters: () =>
    set({
      ...DEFAULT_MESSAGE_FILTERS,
      selectedMessageId: null,
    }),
}))
