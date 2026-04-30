import { AxiosHeaders, type AxiosAdapter } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import apiClient from '@/services/api-client'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'

const TENANT_ID = '11111111-1111-4111-8111-111111111111'
const PERSON_ID = '22222222-2222-4222-8222-222222222222'
const ASSIGNMENT_ID = '33333333-3333-4333-8333-333333333333'
const POSITION_ID = '44444444-4444-4444-8444-444444444444'

describe('apiClient unified contract headers', () => {
  const originalAdapter = apiClient.defaults.adapter
  let capturedHeaders: AxiosHeaders

  beforeEach(() => {
    capturedHeaders = new AxiosHeaders()
    useAuthStore.setState({
      token: 'access-token',
      refreshTokenValue: null,
      expiresAtUtc: null,
      user: {
        id: PERSON_ID,
        accountName: 'portal.admin',
        displayName: 'Portal Admin',
        tenantId: TENANT_ID,
        locale: 'en-US',
      },
      isRefreshing: false,
    })
    useIdentityStore.setState({
      currentAssignment: {
        assignmentId: ASSIGNMENT_ID,
        positionId: POSITION_ID,
        orgId: 'org-1',
        positionName: 'Admin',
        orgName: 'Headquarters',
      },
      orgId: 'org-1',
      roleIds: ['ROLE_ADMIN'],
      assignments: [],
      pendingTodoCount: 0,
      unreadMessageCount: 0,
      menuVersion: 0,
      todoVersion: 0,
      messageVersion: 0,
      isLoading: false,
      isSwitching: false,
      isInvalidated: false,
    })

    apiClient.defaults.adapter = vi.fn(async (config) => {
      capturedHeaders = AxiosHeaders.from(config.headers)

      return {
        config,
        data: {
          code: 'OK',
          data: { ok: true },
          meta: {
            requestId: 'server-request-id',
            tenantId: TENANT_ID,
            language: 'en-US',
            timezone: 'Asia/Shanghai',
            idempotencyKey: 'server-idempotency-key',
          },
        },
        headers: new AxiosHeaders({ 'X-Request-Id': 'server-request-id' }),
        status: 200,
        statusText: 'OK',
      }
    }) as AxiosAdapter
  })

  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
    useAuthStore.setState({
      token: null,
      refreshTokenValue: null,
      expiresAtUtc: null,
      user: null,
      isRefreshing: false,
    })
    useIdentityStore.getState().clear()
  })

  it('injects tenant, request, locale, timezone and idempotency headers', async () => {
    const response = await apiClient.post('/v1/org/roles', { code: 'admin' })

    expect(capturedHeaders.get('X-Tenant-Id')).toBe(TENANT_ID)
    expect(capturedHeaders.get('X-Request-Id')).toEqual(expect.any(String))
    expect(capturedHeaders.get('Accept-Language')).toBe('en-US')
    expect(capturedHeaders.get('X-Timezone')).toEqual(expect.any(String))
    expect(capturedHeaders.get('X-Idempotency-Key')).toEqual(expect.any(String))
    expect(capturedHeaders.get('Authorization')).toBe('Bearer access-token')
    expect(capturedHeaders.get('X-Identity-Assignment-Id')).toBe(ASSIGNMENT_ID)
    expect(capturedHeaders.get('X-Identity-Position-Id')).toBe(POSITION_ID)
    expect(capturedHeaders.get('X-Person-Id')).toBe(PERSON_ID)
    expect(response.data.meta.tenantId).toBe(TENANT_ID)
    expect(response.data.meta.language).toBe('en-US')
    expect(response.data.meta.timezone).toBe('Asia/Shanghai')
    expect(response.data.meta.idempotencyKey).toBe('server-idempotency-key')
  })

  it('does not send identity headers to auth endpoints', async () => {
    await apiClient.post('/v1/auth/login', {
      username: 'admin',
      password: 'admin',
    })

    expect(capturedHeaders.has('Authorization')).toBe(false)
    expect(capturedHeaders.has('X-Identity-Assignment-Id')).toBe(false)
    expect(capturedHeaders.has('X-Identity-Position-Id')).toBe(false)
  })

  it('skips invalid identity header values', async () => {
    useIdentityStore.setState({
      currentAssignment: {
        assignmentId: 'assign-demo-001',
        positionId: 'position-demo-001',
        orgId: 'org-demo-001',
        positionName: 'Demo',
        orgName: 'Demo Org',
      },
    })

    await apiClient.get('/v1/org/identity-context/current')

    expect(capturedHeaders.has('X-Identity-Assignment-Id')).toBe(false)
    expect(capturedHeaders.has('X-Identity-Position-Id')).toBe(false)
  })
})
