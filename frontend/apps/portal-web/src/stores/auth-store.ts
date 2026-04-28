import { create } from 'zustand'
import type { AuthenticatedUser } from '@/types/domain'

const AUTH_STORAGE_KEY = 'hjo2oa.portal.auth'
const REFRESH_EARLY_MS = 60000

export interface AuthSession {
  token: string
  refreshToken?: string
  expiresAtUtc?: string
  user: AuthenticatedUser
}

interface StoredAuthSession extends AuthSession {
  savedAtUtc: string
}

interface AuthStoreState {
  token: string | null
  refreshTokenValue: string | null
  expiresAtUtc: string | null
  user: AuthenticatedUser | null
  isRefreshing: boolean
  login: (session: AuthSession) => void
  logout: () => void
  hydrateFromStorage: () => void
  refreshSession: (session: AuthSession) => void
  setRefreshing: (isRefreshing: boolean) => void
}

let refreshTimerId: ReturnType<typeof setTimeout> | null = null

function readStoredSession(): StoredAuthSession | null {
  if (typeof localStorage === 'undefined') {
    return null
  }

  const rawValue = localStorage.getItem(AUTH_STORAGE_KEY)

  if (!rawValue) {
    return null
  }

  try {
    const parsedValue = JSON.parse(rawValue) as Partial<StoredAuthSession>

    if (!parsedValue.token || !parsedValue.user) {
      return null
    }

    return parsedValue as StoredAuthSession
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)

    return null
  }
}

function persistSession(session: AuthSession): void {
  if (typeof localStorage === 'undefined') {
    return
  }

  const storedSession: StoredAuthSession = {
    ...session,
    savedAtUtc: new Date().toISOString(),
  }

  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(storedSession))
}

function clearStoredSession(): void {
  if (typeof localStorage === 'undefined') {
    return
  }

  localStorage.removeItem(AUTH_STORAGE_KEY)
}

function clearRefreshTimer(): void {
  if (!refreshTimerId) {
    return
  }

  clearTimeout(refreshTimerId)
  refreshTimerId = null
}

function scheduleRefresh(expiresAtUtc?: string): void {
  clearRefreshTimer()

  if (!expiresAtUtc) {
    return
  }

  const expiresAt = new Date(expiresAtUtc).getTime()
  const delay = expiresAt - Date.now() - REFRESH_EARLY_MS

  if (!Number.isFinite(delay)) {
    return
  }

  refreshTimerId = setTimeout(
    () => {
      window.dispatchEvent(new CustomEvent('auth.session.refresh-required'))
    },
    Math.max(delay, 0),
  )
}

export function createDemoAuthSession(): AuthSession {
  return {
    token: 'demo-access-token',
    refreshToken: 'demo-refresh-token',
    expiresAtUtc: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
    user: {
      id: 'acct-demo-001',
      accountName: 'portal.admin',
      displayName: '门户管理员',
      tenantId: import.meta.env.VITE_TENANT_ID ?? 'tenant-demo',
      locale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'zh-CN',
    },
  }
}

const initialSession = readStoredSession()

if (
  initialSession?.expiresAtUtc &&
  new Date(initialSession.expiresAtUtc).getTime() <= Date.now()
) {
  clearStoredSession()
}

const activeInitialSession =
  initialSession?.expiresAtUtc &&
  new Date(initialSession.expiresAtUtc).getTime() <= Date.now()
    ? null
    : initialSession

if (activeInitialSession) {
  scheduleRefresh(activeInitialSession.expiresAtUtc)
}

export const useAuthStore = create<AuthStoreState>((set) => ({
  token: activeInitialSession?.token ?? null,
  refreshTokenValue: activeInitialSession?.refreshToken ?? null,
  expiresAtUtc: activeInitialSession?.expiresAtUtc ?? null,
  user: activeInitialSession?.user ?? null,
  isRefreshing: false,
  login: (session) => {
    persistSession(session)
    scheduleRefresh(session.expiresAtUtc)
    set({
      token: session.token,
      refreshTokenValue: session.refreshToken ?? null,
      expiresAtUtc: session.expiresAtUtc ?? null,
      user: session.user,
    })
  },
  logout: () => {
    clearRefreshTimer()
    clearStoredSession()
    set({
      token: null,
      refreshTokenValue: null,
      expiresAtUtc: null,
      user: null,
      isRefreshing: false,
    })
  },
  hydrateFromStorage: () => {
    const storedSession = readStoredSession()

    if (!storedSession) {
      return
    }

    if (
      storedSession.expiresAtUtc &&
      new Date(storedSession.expiresAtUtc).getTime() <= Date.now()
    ) {
      clearStoredSession()

      return
    }

    scheduleRefresh(storedSession.expiresAtUtc)
    set({
      token: storedSession.token,
      refreshTokenValue: storedSession.refreshToken ?? null,
      expiresAtUtc: storedSession.expiresAtUtc ?? null,
      user: storedSession.user,
    })
  },
  refreshSession: (session) => {
    persistSession(session)
    scheduleRefresh(session.expiresAtUtc)
    set({
      token: session.token,
      refreshTokenValue: session.refreshToken ?? null,
      expiresAtUtc: session.expiresAtUtc ?? null,
      user: session.user,
      isRefreshing: false,
    })
  },
  setRefreshing: (isRefreshing) => {
    set({ isRefreshing })
  },
}))
