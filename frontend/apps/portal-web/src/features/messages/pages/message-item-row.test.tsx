import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MessageItemRow } from '@/features/messages/pages/message-item-row'
import type { MessageNotificationSummary } from '@/features/messages/types/message'

const baseMessage: MessageNotificationSummary = {
  id: 'msg-001',
  type: 'SYSTEM',
  category: 'SYSTEM_SECURITY',
  title: '系统通知',
  summary: '这是一条系统通知摘要',
  readStatus: 'UNREAD',
  createdAt: '2026-04-28T00:00:00.000Z',
}

describe('MessageItemRow', () => {
  it('renders unread messages with bold title and unread marker', () => {
    render(<MessageItemRow message={baseMessage} onSelect={vi.fn()} />)

    expect(screen.getByText('系统通知')).toHaveClass('font-semibold')
    expect(screen.getByText('未读')).toBeInTheDocument()
  })

  it('renders read messages without unread styling', () => {
    render(
      <MessageItemRow
        message={{ ...baseMessage, readStatus: 'READ' }}
        onSelect={vi.fn()}
      />,
    )

    expect(screen.getByText('系统通知')).toHaveClass('font-medium')
    expect(screen.getByText('已读')).toBeInTheDocument()
  })
})
