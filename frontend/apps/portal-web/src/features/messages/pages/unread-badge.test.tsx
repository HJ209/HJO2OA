import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { UnreadBadge } from '@/features/messages/pages/unread-badge'

describe('UnreadBadge', () => {
  it('renders nothing when count is zero', () => {
    const { container } = render(<UnreadBadge count={0} />)

    expect(container).toBeEmptyDOMElement()
  })

  it('renders capped count when count exceeds 99', () => {
    render(<UnreadBadge count={104} />)

    expect(screen.getByRole('status')).toHaveTextContent('99+')
    expect(screen.getByLabelText('未读消息 104 条')).toBeInTheDocument()
  })
})
