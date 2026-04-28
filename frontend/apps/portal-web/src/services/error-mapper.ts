import type { ApiErrorDetail } from '@/types/api'

const ERROR_MESSAGE_MAP = {
  RESOURCE_NOT_FOUND: '请求的资源不存在，请刷新后重试',
  VALIDATION_ERROR: '提交信息不完整或格式不正确，请检查后重试',
  UNAUTHORIZED: '登录状态已失效，请重新登录',
  FORBIDDEN: '当前身份暂无访问权限，请切换身份或联系管理员',
  BUSINESS_RULE_VIOLATION: '当前操作不满足业务规则，请确认后再提交',
  INTERNAL_ERROR: '系统暂时不可用，请稍后重试',
  SERVICE_UNAVAILABLE: '服务暂不可用，请稍后重试',
  NETWORK_ERROR: '网络连接异常，请检查网络后重试',
  UNKNOWN: '系统处理请求时发生异常，请稍后重试',
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
    super(mapErrorCodeToMessage(resolvedCode, options.status))
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
