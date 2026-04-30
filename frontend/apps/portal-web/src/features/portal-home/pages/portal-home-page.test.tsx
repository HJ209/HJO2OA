import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { PortalRuntimeCard } from '@/features/portal-home/pages/portal-home-page'

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

describe('PortalRuntimeCard', () => {
  it('renders a real failed backend card as data source failure', () => {
    render(
      <MemoryRouter future={routerFuture}>
        <PortalRuntimeCard
          card={{
            cardCode: 'todo-card',
            cardType: 'TODO',
            title: '待办',
            description: '待办聚合',
            actionLink: '/api/v1/todo/pending',
            state: 'FAILED',
            message: 'upstream timeout',
            data: null,
          }}
        />
      </MemoryRouter>,
    )

    expect(screen.getByText('数据源失败')).toBeInTheDocument()
    expect(screen.getByText('upstream timeout')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '打开' })).toHaveAttribute(
      'href',
      '/todo',
    )
  })
})
