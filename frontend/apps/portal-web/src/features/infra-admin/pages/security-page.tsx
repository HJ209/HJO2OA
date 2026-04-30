import type { FormEvent, ReactElement } from 'react'
import { useMemo, useState } from 'react'
import { Eye, KeyRound, LockKeyhole, Plus, ShieldCheck } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useSecurityPolicies } from '@/features/infra-admin/hooks/use-security'
import { securityService } from '@/features/infra-admin/services/security-service'
import type {
  CreateSecurityPolicyPayload,
  SecurityPolicy,
} from '@/features/infra-admin/types/infra'

const DEFAULT_CONFIG: Record<SecurityPolicy['policyType'], string> = {
  KEY_MANAGEMENT: '{}',
  MASKING: '{}',
  SIGNATURE: '{}',
  PASSWORD:
    '{"minLength":10,"requireUppercase":true,"requireLowercase":true,"requireDigit":true,"requireSpecial":true,"historyCount":5}',
  ACCESS_CONTROL: '{"paths":["/api/v1/infra/"],"ipWhitelist":["127.0.0.1"]}',
}

export default function SecurityPage(): ReactElement {
  const queryClient = useQueryClient()
  const query = useSecurityPolicies({ page: 1, size: 20 })
  const policies = useMemo(() => query.data?.items ?? [], [query.data?.items])
  const [draft, setDraft] = useState<CreateSecurityPolicyPayload>({
    policyCode: 'password-default',
    policyType: 'PASSWORD',
    name: 'Default password policy',
    configSnapshot: DEFAULT_CONFIG.PASSWORD,
  })
  const [selectedPolicyId, setSelectedPolicyId] = useState('')
  const selectedPolicy = useMemo(
    () =>
      policies.find((policy) => policy.id === selectedPolicyId) ?? policies[0],
    [policies, selectedPolicyId],
  )
  const [maskValue, setMaskValue] = useState('13812345678')
  const [maskResult, setMaskResult] = useState('')
  const [password, setPassword] = useState('Example#1234')
  const [passwordResult, setPasswordResult] = useState('')
  const [cryptoValue, setCryptoValue] = useState('secret-value')
  const [cryptoResult, setCryptoResult] = useState('')

  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['infra', 'security'] })

  const createPolicy = useMutation({
    mutationFn: securityService.create,
    onSuccess: refresh,
  })
  const addSecretKey = useMutation({
    mutationFn: () =>
      securityService.addSecretKey(requireSelectedPolicy(selectedPolicy), {
        keyRef: 'customer-data',
        algorithm: 'AES',
      }),
    onSuccess: refresh,
  })
  const addMaskingRule = useMutation({
    mutationFn: () =>
      securityService.addMaskingRule(requireSelectedPolicy(selectedPolicy), {
        dataType: 'phone',
        ruleExpr: 'KEEP_SUFFIX(4)',
      }),
    onSuccess: refresh,
  })
  const addRateLimitRule = useMutation({
    mutationFn: () =>
      securityService.addRateLimitRule(requireSelectedPolicy(selectedPolicy), {
        subjectType: 'IP',
        windowSeconds: 60,
        maxRequests: 20,
      }),
    onSuccess: refresh,
  })

  function submitPolicy(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    createPolicy.mutate(draft)
  }

  async function previewMasking(): Promise<void> {
    const result = await securityService.previewMasking({
      policyCode: selectedPolicy?.policyCode,
      dataType: 'phone',
      value: maskValue,
    })
    setMaskResult(result.maskedValue)
  }

  async function validatePassword(): Promise<void> {
    const result = await securityService.validatePassword({
      policyCode: selectedPolicy?.policyCode,
      username: 'alice',
      password,
      passwordHistory: ['OldPassword#123'],
    })
    setPasswordResult(
      result.accepted ? 'ACCEPTED' : result.violations.join(', '),
    )
  }

  async function encryptValue(): Promise<void> {
    const result = await securityService.encrypt({
      keyRef: 'customer-data',
      algorithm: 'AES',
      value: cryptoValue,
    })
    setCryptoResult(result.value)
  }

  return (
    <InfraPageSection
      description="运行时安全策略、密钥引用、脱敏和限流。"
      title="安全策略"
    >
      <form
        className="mb-5 grid gap-3 rounded-lg border border-slate-200 bg-white p-4 md:grid-cols-5"
        onSubmit={submitPolicy}
      >
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          onChange={(event) =>
            setDraft((current) => ({
              ...current,
              policyCode: event.target.value,
            }))
          }
          placeholder="policyCode"
          value={draft.policyCode}
        />
        <select
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          onChange={(event) => {
            const policyType = event.target
              .value as SecurityPolicy['policyType']
            setDraft((current) => ({
              ...current,
              policyType,
              configSnapshot: DEFAULT_CONFIG[policyType],
            }))
          }}
          value={draft.policyType}
        >
          {Object.keys(DEFAULT_CONFIG).map((policyType) => (
            <option key={policyType} value={policyType}>
              {policyType}
            </option>
          ))}
        </select>
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          onChange={(event) =>
            setDraft((current) => ({ ...current, name: event.target.value }))
          }
          placeholder="name"
          value={draft.name}
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-1"
          onChange={(event) =>
            setDraft((current) => ({
              ...current,
              configSnapshot: event.target.value,
            }))
          }
          placeholder="config JSON"
          value={draft.configSnapshot}
        />
        <button
          className="inline-flex items-center justify-center gap-2 rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white"
          type="submit"
        >
          <Plus size={16} />
          创建
        </button>
      </form>

      <InfraTable
        columns={[
          {
            key: 'policyCode',
            title: '编码',
            render: (item) => item.policyCode,
          },
          {
            key: 'policyType',
            title: '类型',
            render: (item) => item.policyType,
          },
          {
            key: 'status',
            title: '状态',
            render: (item) => (
              <StatusPill active={item.status === 'ACTIVE'}>
                {item.status}
              </StatusPill>
            ),
          },
          {
            key: 'rules',
            title: '规则',
            render: (item) =>
              `${item.secretKeys.length} keys / ${item.maskingRules.length} masks / ${item.rateLimitRules.length} limits`,
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={policies}
      />

      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <select
            className="mb-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            onChange={(event) => setSelectedPolicyId(event.target.value)}
            value={selectedPolicy?.id ?? ''}
          >
            {policies.map((policy) => (
              <option key={policy.id} value={policy.id}>
                {policy.policyCode}
              </option>
            ))}
          </select>
          <div className="flex flex-wrap gap-2">
            <button
              className="inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onClick={() => addSecretKey.mutate()}
              type="button"
            >
              <KeyRound size={16} />
              AES 密钥
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onClick={() => addMaskingRule.mutate()}
              type="button"
            >
              <Eye size={16} />
              手机脱敏
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onClick={() => addRateLimitRule.mutate()}
              type="button"
            >
              <ShieldCheck size={16} />
              IP 限流
            </button>
          </div>
        </section>

        <section className="grid gap-3 rounded-lg border border-slate-200 bg-white p-4">
          <div className="flex gap-2">
            <input
              className="min-w-0 flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onChange={(event) => setMaskValue(event.target.value)}
              value={maskValue}
            />
            <button
              className="rounded-md bg-slate-900 px-3 py-2 text-sm text-white"
              onClick={() => void previewMasking()}
              type="button"
            >
              脱敏
            </button>
          </div>
          <div className="text-sm text-slate-600">{maskResult || '-'}</div>
          <div className="flex gap-2">
            <input
              className="min-w-0 flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onChange={(event) => setPassword(event.target.value)}
              value={password}
            />
            <button
              className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-3 py-2 text-sm text-white"
              onClick={() => void validatePassword()}
              type="button"
            >
              <LockKeyhole size={16} />
              校验
            </button>
          </div>
          <div className="text-sm text-slate-600">{passwordResult || '-'}</div>
          <div className="flex gap-2">
            <input
              className="min-w-0 flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onChange={(event) => setCryptoValue(event.target.value)}
              value={cryptoValue}
            />
            <button
              className="rounded-md bg-slate-900 px-3 py-2 text-sm text-white"
              onClick={() => void encryptValue()}
              type="button"
            >
              加密
            </button>
          </div>
          <div className="break-all text-xs text-slate-600">
            {cryptoResult || '-'}
          </div>
        </section>
      </div>
    </InfraPageSection>
  )
}

function requireSelectedPolicy(policy: SecurityPolicy | undefined): string {
  if (!policy) {
    throw new Error('No security policy selected')
  }

  return policy.id
}
