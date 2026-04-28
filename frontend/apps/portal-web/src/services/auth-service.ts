import { post } from '@/services/request'
import type { AuthSession } from '@/stores/auth-store'

export interface LoginRequest {
  username: string
  password: string
}

export interface RefreshRequest {
  token: string
}

interface LoginResponse {
  token: string
  tokenType: string
  expiresAt: string
}

function toAuthSession(
  response: LoginResponse,
  user: AuthSession['user'],
): AuthSession {
  return {
    token: response.token,
    expiresAtUtc: response.expiresAt,
    user,
  }
}

export async function login(
  username: string,
  password: string,
): Promise<AuthSession> {
  const loginResponse = await post<LoginResponse, LoginRequest>(
    '/auth/login',
    { username, password },
    {
      dedupeKey: `auth-login:${username}`,
    },
  )

  return toAuthSession(loginResponse, {
    id: '',
    accountName: username,
    displayName: username,
    tenantId: import.meta.env.VITE_TENANT_ID ?? 'tenant-demo',
    locale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'zh-CN',
  })
}

export async function logout(): Promise<void> {
  await post<void, Record<string, never>>(
    '/auth/logout',
    {},
    {
      dedupeKey: 'auth-logout',
    },
  )
}

export async function refreshToken(tokenValue: string): Promise<AuthSession> {
  const loginResponse = await post<LoginResponse, RefreshRequest>(
    '/auth/refresh',
    { token: tokenValue },
    {
      dedupeKey: 'auth-refresh-token',
    },
  )

  return toAuthSession(loginResponse, {
    id: '',
    accountName: '',
    displayName: '',
    tenantId: import.meta.env.VITE_TENANT_ID ?? 'tenant-demo',
    locale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'zh-CN',
  })
}
