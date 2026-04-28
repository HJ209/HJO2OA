import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore, type AuthSession } from '@/stores/auth-store'

const session: AuthSession = {
  token: 'access-token',
  refreshToken: 'refresh-token',
  expiresAtUtc: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
  user: {
    id: 'acct-001',
    accountName: 'zhangsan',
    displayName: '张三',
    tenantId: 'tenant-demo',
    locale: 'zh-CN',
  },
}

afterEach(() => {
  useAuthStore.getState().logout()
  localStorage.clear()
})

describe('useAuthStore', () => {
  it('persists session after login and clears it after logout', () => {
    useAuthStore.getState().login(session)

    expect(useAuthStore.getState().token).toBe('access-token')
    expect(useAuthStore.getState().user?.displayName).toBe('张三')
    expect(localStorage.getItem('hjo2oa.portal.auth')).toContain('access-token')

    useAuthStore.getState().logout()

    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
    expect(localStorage.getItem('hjo2oa.portal.auth')).toBeNull()
  })

  it('updates token on refreshSession', () => {
    useAuthStore.getState().login(session)
    useAuthStore.getState().refreshSession({
      ...session,
      token: 'access-token-next',
    })

    expect(useAuthStore.getState().token).toBe('access-token-next')
    expect(useAuthStore.getState().isRefreshing).toBe(false)
  })
})
