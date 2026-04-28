import axios, {
  AxiosHeaders,
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'
import { BizError, resolveErrorCode } from '@/services/error-mapper'
import type { ApiResponse, BackendApiResponse } from '@/types/api'
import { getUserTimezone } from '@/utils/format-time'

const MUTATION_METHODS = new Set(['post', 'put', 'patch', 'delete'])

function generateClientId(): string {
  if (
    typeof crypto !== 'undefined' &&
    typeof crypto.randomUUID === 'function'
  ) {
    return crypto.randomUUID()
  }

  return `req-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function readHeader(headers: AxiosHeaders, key: string): string | undefined {
  const value = headers.get(key)

  return typeof value === 'string' ? value : undefined
}

function resolveRequestId(headers: AxiosHeaders): string {
  return (
    readHeader(headers, 'X-Request-Id') ??
    readHeader(headers, 'x-request-id') ??
    generateClientId()
  )
}

function toAxiosHeaders(headers: unknown): AxiosHeaders {
  if (headers instanceof AxiosHeaders) {
    return headers
  }

  const normalizedHeaders = new AxiosHeaders()

  if (isObjectRecord(headers)) {
    for (const [key, value] of Object.entries(headers)) {
      if (
        typeof value === 'string' ||
        typeof value === 'number' ||
        typeof value === 'boolean'
      ) {
        normalizedHeaders.set(key, String(value))
      }
    }
  }

  return normalizedHeaders
}

function isObjectRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isBackendApiResponse<T>(
  value: unknown,
): value is BackendApiResponse<T> {
  if (!isObjectRecord(value)) {
    return false
  }

  return 'data' in value || 'code' in value || 'meta' in value
}

function normalizeApiResponse<T>(
  payload: BackendApiResponse<T>,
  fallbackRequestId: string,
): ApiResponse<T> {
  const success =
    typeof payload.meta?.success === 'boolean'
      ? payload.meta.success
      : payload.code === undefined || payload.code === 'OK'
  const errorCode =
    payload.meta?.errorCode ?? (success ? undefined : payload.code)

  return {
    data: payload.data,
    message: payload.message,
    errors: payload.errors,
    meta: {
      requestId: payload.meta?.requestId ?? fallbackRequestId,
      success,
      errorCode,
      timestamp: payload.meta?.timestamp,
      serverTimezone: payload.meta?.serverTimezone,
    },
  }
}

function toBizError<T>(payload: ApiResponse<T>, status?: number): BizError {
  return new BizError({
    code: payload.meta.errorCode,
    requestId: payload.meta.requestId,
    backendMessage: payload.message,
    errors: payload.errors,
    status,
  })
}

function applyRequestHeaders(
  config: InternalAxiosRequestConfig<unknown>,
): InternalAxiosRequestConfig<unknown> {
  const headers = AxiosHeaders.from(config.headers)
  const method = config.method?.toLowerCase()
  const authState = useAuthStore.getState()
  const identityState = useIdentityStore.getState()

  if (!readHeader(headers, 'X-Request-Id')) {
    headers.set('X-Request-Id', generateClientId())
  }

  if (!readHeader(headers, 'X-Tenant-Id')) {
    headers.set(
      'X-Tenant-Id',
      authState.user?.tenantId ??
        import.meta.env.VITE_TENANT_ID ??
        'tenant-demo',
    )
  }

  if (!readHeader(headers, 'Accept-Language')) {
    headers.set(
      'Accept-Language',
      authState.user?.locale ??
        navigator.language ??
        import.meta.env.VITE_DEFAULT_LOCALE ??
        'zh-CN',
    )
  }

  if (!readHeader(headers, 'X-Timezone')) {
    headers.set('X-Timezone', getUserTimezone())
  }

  if (authState.token && !readHeader(headers, 'Authorization')) {
    headers.set('Authorization', `Bearer ${authState.token}`)
  }

  if (
    identityState.currentAssignment?.assignmentId &&
    !readHeader(headers, 'X-Identity-Assignment-Id')
  ) {
    headers.set(
      'X-Identity-Assignment-Id',
      identityState.currentAssignment.assignmentId,
    )
  }

  if (
    method &&
    MUTATION_METHODS.has(method) &&
    !readHeader(headers, 'X-Idempotency-Key')
  ) {
    headers.set('X-Idempotency-Key', generateClientId())
  }

  config.headers = headers

  return config
}

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15000,
})

apiClient.interceptors.request.use((config) => applyRequestHeaders(config))

apiClient.interceptors.response.use(
  (response: AxiosResponse<unknown>) => {
    const fallbackRequestId = resolveRequestId(toAxiosHeaders(response.headers))

    if (isBackendApiResponse(response.data)) {
      const normalizedResponse = normalizeApiResponse(
        response.data,
        fallbackRequestId,
      )

      if (!normalizedResponse.meta.success) {
        throw toBizError(normalizedResponse, response.status)
      }

      return {
        ...response,
        data: normalizedResponse,
      } as AxiosResponse<ApiResponse<unknown>>
    }

    return {
      ...response,
      data: {
        data: response.data,
        meta: {
          requestId: fallbackRequestId,
          success: true,
        },
      },
    } as AxiosResponse<ApiResponse<unknown>>
  },
  (error: AxiosError<BackendApiResponse<unknown>>) => {
    if (error.response) {
      const fallbackRequestId = resolveRequestId(
        toAxiosHeaders(error.response.headers),
      )

      if (isBackendApiResponse(error.response.data)) {
        return Promise.reject(
          toBizError(
            normalizeApiResponse(error.response.data, fallbackRequestId),
            error.response.status,
          ),
        )
      }

      return Promise.reject(
        new BizError({
          code: resolveErrorCode(undefined, error.response.status),
          requestId: fallbackRequestId,
          status: error.response.status,
        }),
      )
    }

    return Promise.reject(
      new BizError({
        code:
          error.code === 'ERR_NETWORK'
            ? 'NETWORK_ERROR'
            : 'SERVICE_UNAVAILABLE',
      }),
    )
  },
)

export default apiClient
