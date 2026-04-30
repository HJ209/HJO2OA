import { get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  CreateSecurityPolicyPayload,
  CryptoResult,
  InfraListQuery,
  InfraPageData,
  PasswordValidationResult,
  SecurityPolicy,
} from '@/features/infra-admin/types/infra'

const POLICY_URL = '/v1/infra/security/policies'
const RUNTIME_URL = '/v1/infra/security'

interface SecurityPolicySnapshot {
  minPasswordLength?: number
  mfaRequired?: boolean
  sessionTimeoutMinutes?: number
}

function parseSecuritySnapshot(configSnapshot: string): SecurityPolicySnapshot {
  try {
    const value = JSON.parse(configSnapshot) as SecurityPolicySnapshot

    return value && typeof value === 'object' ? value : {}
  } catch {
    return {}
  }
}

function mapSecurityPolicy(item: SecurityPolicy): SecurityPolicy {
  const snapshot = parseSecuritySnapshot(item.configSnapshot)

  return {
    ...item,
    minPasswordLength:
      item.minPasswordLength ?? snapshot.minPasswordLength ?? 0,
    mfaRequired: item.mfaRequired ?? snapshot.mfaRequired ?? false,
    sessionTimeoutMinutes:
      item.sessionTimeoutMinutes ?? snapshot.sessionTimeoutMinutes ?? 0,
  }
}

export const securityService = {
  async list(query?: InfraListQuery): Promise<InfraPageData<SecurityPolicy>> {
    const items = await get<SecurityPolicy[]>(POLICY_URL, {
      params: buildListParams(query),
    })

    return toPageData(items.map(mapSecurityPolicy), query)
  },
  create(payload: CreateSecurityPolicyPayload): Promise<SecurityPolicy> {
    return post<SecurityPolicy, CreateSecurityPolicyPayload>(
      POLICY_URL,
      payload,
      {
        dedupeKey: `security:create:${payload.policyCode}`,
      },
    ).then(mapSecurityPolicy)
  },
  updateConfig(id: string, configSnapshot: string): Promise<SecurityPolicy> {
    return put<SecurityPolicy, { configSnapshot: string }>(
      `${POLICY_URL}/${id}/config`,
      { configSnapshot },
      {
        dedupeKey: `security:update-config:${id}`,
      },
    ).then(mapSecurityPolicy)
  },
  addSecretKey(
    id: string,
    payload: { keyRef: string; algorithm: string },
  ): Promise<SecurityPolicy> {
    return post<SecurityPolicy, typeof payload>(
      `${POLICY_URL}/${id}/secret-keys`,
      payload,
      {
        dedupeKey: `security:key:${id}:${payload.keyRef}`,
      },
    ).then(mapSecurityPolicy)
  },
  addMaskingRule(
    id: string,
    payload: { dataType: string; ruleExpr: string },
  ): Promise<SecurityPolicy> {
    return post<SecurityPolicy, typeof payload>(
      `${POLICY_URL}/${id}/masking-rules`,
      payload,
      {
        dedupeKey: `security:masking:${id}:${payload.dataType}`,
      },
    ).then(mapSecurityPolicy)
  },
  addRateLimitRule(
    id: string,
    payload: {
      subjectType: 'IP' | 'USER' | 'TENANT' | 'API_CLIENT'
      windowSeconds: number
      maxRequests: number
    },
  ): Promise<SecurityPolicy> {
    return post<SecurityPolicy, typeof payload>(
      `${POLICY_URL}/${id}/rate-limit-rules`,
      payload,
      {
        dedupeKey: `security:rate:${id}:${payload.subjectType}`,
      },
    ).then(mapSecurityPolicy)
  },
  previewMasking(payload: {
    policyCode?: string
    dataType: string
    value: string
  }): Promise<{ maskedValue: string }> {
    return post(`${RUNTIME_URL}/masking/preview`, payload, {
      dedupeKey: `security:mask:${payload.dataType}:${payload.value}`,
    })
  },
  encrypt(payload: {
    keyRef: string
    algorithm: 'AES' | 'RSA'
    value: string
  }): Promise<CryptoResult> {
    return post(`${RUNTIME_URL}/crypto/encrypt`, payload, {
      dedupeKey: `security:encrypt:${payload.keyRef}:${payload.value}`,
    })
  },
  decrypt(payload: {
    keyRef: string
    algorithm: 'AES' | 'RSA'
    value: string
  }): Promise<CryptoResult> {
    return post(`${RUNTIME_URL}/crypto/decrypt`, payload, {
      dedupeKey: `security:decrypt:${payload.keyRef}:${payload.value}`,
    })
  },
  validatePassword(payload: {
    policyCode?: string
    username?: string
    password: string
    passwordHistory?: string[]
  }): Promise<PasswordValidationResult> {
    return post(`${RUNTIME_URL}/password/validate`, payload, {
      dedupeKey: `security:password:${payload.policyCode}:${payload.password}`,
    })
  },
}
