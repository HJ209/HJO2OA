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

function isUuid(value: string | null | undefined): value is string {
  return Boolean(
    value &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    ),
  )
}

function decodeJwtTenantId(token: string): string | null {
  return decodeJwtStringClaim(token, 'tenantId')
}

function decodeJwtStringClaim(token: string, claim: string): string | null {
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
    const payload = JSON.parse(atob(paddedPayload)) as Record<string, unknown>
    const value = typeof payload[claim] === 'string' ? payload[claim] : null

    return isUuid(value) ? value : null
  } catch {
    return null
  }
}

function resolveSessionTenantId(token: string): string {
  const envTenantId = import.meta.env.VITE_TENANT_ID

  return decodeJwtTenantId(token) ?? (isUuid(envTenantId) ? envTenantId : '')
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
    '/v1/auth/login',
    { username, password },
    {
      dedupeKey: `auth-login:${username}`,
    },
  )

  return toAuthSession(loginResponse, {
    id: decodeJwtStringClaim(loginResponse.token, 'sub') ?? '',
    accountName: username,
    displayName: username,
    tenantId: resolveSessionTenantId(loginResponse.token),
    locale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'zh-CN',
  })
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

export async function refreshToken(tokenValue: string): Promise<AuthSession> {
  const loginResponse = await post<LoginResponse, RefreshRequest>(
    '/v1/auth/refresh',
    { token: tokenValue },
    {
      dedupeKey: 'auth-refresh-token',
    },
  )

  return toAuthSession(loginResponse, {
    id: decodeJwtStringClaim(loginResponse.token, 'sub') ?? '',
    accountName: '',
    displayName: '',
    tenantId: resolveSessionTenantId(loginResponse.token),
    locale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'zh-CN',
  })
}
