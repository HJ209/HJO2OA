import type { ApiErrorDetail } from '@/types/api'

const ERROR_MESSAGE_MAP = {
  RESOURCE_NOT_FOUND: 'The requested resource was not found.',
  VALIDATION_ERROR: 'The submitted data is incomplete or invalid.',
  UNAUTHORIZED: 'Your session has expired. Please sign in again.',
  FORBIDDEN: 'You do not have permission to perform this action.',
  BUSINESS_RULE_VIOLATION: 'The operation does not satisfy the business rules.',
  INTERNAL_ERROR: 'The service encountered an internal error.',
  SERVICE_UNAVAILABLE: 'The service is temporarily unavailable.',
  NETWORK_ERROR: 'The network request failed.',
  UNKNOWN: 'The request could not be completed.',
} as const

type KnownErrorCode = keyof typeof ERROR_MESSAGE_MAP

const STATUS_TO_CODE_MAP: Record<number, KnownErrorCode> = {
  400: 'VALIDATION_ERROR',
  401: 'UNAUTHORIZED',
  403: 'FORBIDDEN',
  404: 'RESOURCE_NOT_FOUND',
  422: 'BUSINESS_RULE_VIOLATION',
  500: 'INTERNAL_ERROR',
  503: 'SERVICE_UNAVAILABLE',
}

export function resolveErrorCode(errorCode?: string, status?: number): string {
  if (errorCode) {
    return errorCode
  }

  if (status && STATUS_TO_CODE_MAP[status]) {
    return STATUS_TO_CODE_MAP[status]
  }

  return 'UNKNOWN'
}

export function mapErrorCodeToMessage(
  errorCode?: string,
  status?: number,
): string {
  const resolvedCode = resolveErrorCode(errorCode, status)

  if (resolvedCode in ERROR_MESSAGE_MAP) {
    return ERROR_MESSAGE_MAP[resolvedCode as KnownErrorCode]
  }

  return ERROR_MESSAGE_MAP.UNKNOWN
}

interface BizErrorOptions {
  code?: string
  requestId?: string
  backendMessage?: string
  errors?: ApiErrorDetail[]
  status?: number
}

export class BizError extends Error {
  readonly code: string
  readonly requestId?: string
  readonly backendMessage?: string
  readonly errors?: ApiErrorDetail[]
  readonly status?: number

  constructor(options: BizErrorOptions) {
    const resolvedCode = resolveErrorCode(options.code, options.status)
    super(
      options.backendMessage?.trim() ||
        mapErrorCodeToMessage(resolvedCode, options.status),
    )
    this.name = 'BizError'
    this.code = resolvedCode
    this.requestId = options.requestId
    this.backendMessage = options.backendMessage
    this.errors = options.errors
    this.status = options.status
  }
}

export function isBizError(error: unknown): error is BizError {
  return error instanceof BizError
}
