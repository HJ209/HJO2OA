import { post } from '@/services/request'
import type { AuthSession } from '@/stores/auth-store'

export interface LoginRequest {
  username: string
  password: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export async function login(
  username: string,
  password: string,
): Promise<AuthSession> {
  return post<AuthSession, LoginRequest>(
    '/v1/auth/login',
    { username, password },
    {
      dedupeKey: `auth-login:${username}`,
    },
  )
}

export async function logout(): Promise<void> {
  await post<void, Record<string, never>>(
    '/v1/auth/logout',
    {},
    {
      dedupeKey: 'auth-logout',
    },
  )
}

export async function refreshToken(
  refreshTokenValue: string,
): Promise<AuthSession> {
  return post<AuthSession, RefreshTokenRequest>(
    '/v1/auth/refresh',
    { refreshToken: refreshTokenValue },
    {
      dedupeKey: 'auth-refresh-token',
    },
  )
}
