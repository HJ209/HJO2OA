import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('renders the home page capability map', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByText('React 门户骨架已经就位')).toBeInTheDocument()
    expect(screen.getByText('平台能力地图')).toBeInTheDocument()
    expect(screen.getByText('消息中心')).toBeInTheDocument()
  })

  it('renders the roadmap page', () => {
    render(
      <MemoryRouter initialEntries={['/roadmap']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByText('建议的 React 端实施顺序')).toBeInTheDocument()
    expect(screen.getByText('先沉淀应用壳、路由、鉴权、主题与国际化基础设施。')).toBeInTheDocument()
  })
})
