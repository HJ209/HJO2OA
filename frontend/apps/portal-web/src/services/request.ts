import type { AxiosRequestConfig } from 'axios'
import apiClient from '@/services/api-client'
import type { ApiResponse } from '@/types/api'

export interface RequestConfig<TData = unknown> extends Omit<
  AxiosRequestConfig<TData>,
  'method' | 'url'
> {
  dedupeKey?: string
  idempotencyKey?: string
}

const pendingMutationRequests = new Map<string, Promise<unknown>>()

function safeSerialize(value: unknown): string {
  try {
    return JSON.stringify(value) ?? ''
  } catch {
    return '[unserializable]'
  }
}

function withHeaders<TData>(
  config: RequestConfig<TData>,
): AxiosRequestConfig<TData> {
  const headers = {
    ...(config.headers ?? {}),
  }

  if (config.idempotencyKey) {
    return {
      ...config,
      headers: {
        ...headers,
        'X-Idempotency-Key': config.idempotencyKey,
      },
    }
  }

  return config
}

function buildMutationKey<TBody>(
  method: 'post' | 'put' | 'delete',
  url: string,
  body: TBody | undefined,
  config: RequestConfig<TBody>,
): string {
  if (config.dedupeKey) {
    return config.dedupeKey
  }

  return [method, url, safeSerialize(config.params), safeSerialize(body)].join(
    '::',
  )
}

function trackMutation<T>(key: string, execute: () => Promise<T>): Promise<T> {
  const existingRequest = pendingMutationRequests.get(key)

  if (existingRequest) {
    return existingRequest as Promise<T>
  }

  const requestPromise = execute().finally(() => {
    pendingMutationRequests.delete(key)
  })

  pendingMutationRequests.set(key, requestPromise)

  return requestPromise
}

export async function get<TResponse>(
  url: string,
  config: RequestConfig = {},
): Promise<TResponse> {
  const response = await apiClient.get<ApiResponse<TResponse>>(
    url,
    withHeaders(config),
  )

  return response.data.data
}

export async function post<TResponse, TBody = unknown>(
  url: string,
  body: TBody,
  config: RequestConfig<TBody> = {},
): Promise<TResponse> {
  const response = await trackMutation(
    buildMutationKey('post', url, body, config),
    () =>
      apiClient.post<ApiResponse<TResponse>>(url, body, withHeaders(config)),
  )

  return response.data.data
}

export async function put<TResponse, TBody = unknown>(
  url: string,
  body: TBody,
  config: RequestConfig<TBody> = {},
): Promise<TResponse> {
  const response = await trackMutation(
    buildMutationKey('put', url, body, config),
    () => apiClient.put<ApiResponse<TResponse>>(url, body, withHeaders(config)),
  )

  return response.data.data
}

export async function del<TResponse, TBody = unknown>(
  url: string,
  config: RequestConfig<TBody> = {},
): Promise<TResponse> {
  const response = await trackMutation(
    buildMutationKey('delete', url, config.data, config),
    () => apiClient.delete<ApiResponse<TResponse>>(url, withHeaders(config)),
  )

  return response.data.data
}
