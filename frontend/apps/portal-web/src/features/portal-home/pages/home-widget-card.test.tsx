import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import HomeWidgetCard from '@/features/portal-home/pages/home-widget-card'

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

describe('HomeWidgetCard', () => {
  it('renders widget chrome with optional action and children', () => {
    render(
      <MemoryRouter future={routerFuture}>
        <HomeWidgetCard
          action={{ label: '查看全部', href: '/todo' }}
          description="待办和审批摘要"
          title="待办摘要"
        >
          <span>部件内容</span>
        </HomeWidgetCard>
      </MemoryRouter>,
    )

    expect(
      screen.getByRole('heading', { name: '待办摘要' }),
    ).toBeInTheDocument()
    expect(screen.getByText('待办和审批摘要')).toBeInTheDocument()
    expect(screen.getByText('部件内容')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /查看全部/ })).toHaveAttribute(
      'href',
      '/todo',
    )
  })
})
