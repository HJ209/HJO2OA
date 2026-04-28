import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import HomeSection from '@/features/portal-home/pages/home-section'

describe('HomeSection', () => {
  it('renders title, description and widget grid content', () => {
    render(
      <HomeSection
        description="聚合业务部件"
        layout="three-column"
        title="工作聚合"
      >
        <div>待办部件</div>
        <div>消息部件</div>
      </HomeSection>,
    )

    expect(
      screen.getByRole('heading', { name: '工作聚合' }),
    ).toBeInTheDocument()
    expect(screen.getByText('聚合业务部件')).toBeInTheDocument()
    expect(screen.getByText('待办部件')).toBeInTheDocument()
    expect(screen.getByText('消息部件')).toBeInTheDocument()
  })
})
