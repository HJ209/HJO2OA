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

function isUuid(value: string | null | undefined): value is string {
  return Boolean(
    value &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
      value,
    ),
  )
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const segments = token.split('.')

  if (segments.length < 2) {
    return null
  }

  try {
    const normalizedPayload = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      '=',
    )
    const decodedPayload = atob(paddedPayload)

    return JSON.parse(decodedPayload) as Record<string, unknown>
  } catch {
    return null
  }
}

function normalizeSessionTenantId<TSession extends AuthSession>(
  session: TSession,
): TSession {
  const payload = decodeJwtPayload(session.token)
  const jwtTenantId =
    typeof payload?.tenantId === 'string' ? payload.tenantId : null

  if (!isUuid(jwtTenantId) || session.user.tenantId === jwtTenantId) {
    return session
  }

  return {
    ...session,
    user: {
      ...session.user,
      tenantId: jwtTenantId,
    },
  }
}

function resolveEnvTenantId(): string {
  const envTenantId = import.meta.env.VITE_TENANT_ID

  return isUuid(envTenantId) ? envTenantId : ''
}

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

    return normalizeSessionTenantId(parsedValue as StoredAuthSession)
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
    ...normalizeSessionTenantId(session),
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
      tenantId: resolveEnvTenantId(),
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
    const normalizedSession = normalizeSessionTenantId(session)

    persistSession(normalizedSession)
    scheduleRefresh(normalizedSession.expiresAtUtc)
    set({
      token: normalizedSession.token,
      refreshTokenValue: normalizedSession.refreshToken ?? null,
      expiresAtUtc: normalizedSession.expiresAtUtc ?? null,
      user: normalizedSession.user,
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
    const normalizedSession = normalizeSessionTenantId(session)

    persistSession(normalizedSession)
    scheduleRefresh(normalizedSession.expiresAtUtc)
    set({
      token: normalizedSession.token,
      refreshTokenValue: normalizedSession.refreshToken ?? null,
      expiresAtUtc: normalizedSession.expiresAtUtc ?? null,
      user: normalizedSession.user,
      isRefreshing: false,
    })
  },
  setRefreshing: (isRefreshing) => {
    set({ isRefreshing })
  },
}))
