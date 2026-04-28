import { afterEach, describe, expect, it } from 'vitest'
import { useMessageStore } from '@/features/messages/stores/message-store'

afterEach(() => {
  useMessageStore.getState().resetFilters()
})

describe('message-store', () => {
  it('updates type and read status filters', () => {
    useMessageStore.getState().setType('SYSTEM')
    useMessageStore.getState().setReadStatus('UNREAD')

    expect(useMessageStore.getState().type).toBe('SYSTEM')
    expect(useMessageStore.getState().readStatus).toBe('UNREAD')
  })

  it('resets filters and selected message', () => {
    useMessageStore.getState().setType('APPROVAL')
    useMessageStore.getState().setReadStatus('READ')
    useMessageStore.getState().setSelectedMessageId('msg-001')

    useMessageStore.getState().resetFilters()

    expect(useMessageStore.getState().type).toBe('ALL')
    expect(useMessageStore.getState().readStatus).toBe('ALL')
    expect(useMessageStore.getState().selectedMessageId).toBeNull()
  })
})
