import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Search, ShieldCheck } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'
import {
  createDataPermissionPolicy,
  createFieldPermissionPolicy,
  listDataPermissionPolicies,
  listFieldPermissionPolicies,
  previewFieldMask,
  previewRowPermission,
} from '@/features/org-perm/services/data-permission-service'
import type {
  DataPermissionPolicyPayload,
  FieldMaskResult,
  FieldPermissionPayload,
  PermissionDecision,
  PermissionIdentityPayload,
  PermissionSubjectType,
} from '@/features/org-perm/types/org-perm'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const SUBJECT_TYPES: PermissionSubjectType[] = [
  'ROLE',
  'PERSON',
  'POSITION',
  'DEPARTMENT',
  'ORGANIZATION',
]
const ROW_SCOPE_TYPES: DataPermissionPolicyPayload['scopeType'][] = [
  'ALL',
  'SELF',
  'ORG_AND_CHILDREN',
  'DEPT_AND_CHILDREN',
  'CONDITION',
]
const FIELD_ACTIONS: FieldPermissionPayload['action'][] = [
  'VISIBLE',
  'EDITABLE',
  'EXPORTABLE',
  'DESENSITIZED',
  'HIDDEN',
]

function splitRoleIds(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

export default function DataPermissionPage(): ReactElement {
  const queryClient = useQueryClient()
  const authState = useAuthStore()
  const identityState = useIdentityStore()
  const currentAssignment = identityState.currentAssignment
  const [rowForm, setRowForm] = useState<DataPermissionPolicyPayload>({
    subjectType: 'ROLE',
    subjectId: identityState.roleIds[0] ?? '',
    businessObject: 'process_instance',
    scopeType: 'SELF',
    conditionExpr: '',
    effect: 'ALLOW',
    priority: 10,
    enabled: true,
  })
  const [fieldForm, setFieldForm] = useState<FieldPermissionPayload>({
    subjectType: 'ROLE',
    subjectId: identityState.roleIds[0] ?? '',
    businessObject: 'person_profile',
    usageScenario: 'view',
    fieldCode: 'mobile',
    action: 'DESENSITIZED',
    effect: 'ALLOW',
  })
  const [previewForm, setPreviewForm] = useState({
    tenantId: authState.user?.tenantId ?? '',
    personId: authState.user?.id ?? '',
    organizationId: identityState.orgId ?? '',
    departmentId: currentAssignment?.departmentId ?? '',
    positionId: currentAssignment?.positionId ?? '',
    roleIds: identityState.roleIds.join(','),
    businessObject: 'process_instance',
    usageScenario: 'view',
    sampleRow: '{"mobile":"13812345678","salary":"20000"}',
  })
  const [rowDecision, setRowDecision] = useState<PermissionDecision>()
  const [fieldMask, setFieldMask] = useState<FieldMaskResult>()

  const { data: rowPolicies } = useQuery({
    queryKey: ['org-perm', 'data-permissions', { page: 1, size: 50 }],
    queryFn: () => listDataPermissionPolicies({ page: 1, size: 50 }),
  })
  const { data: fieldPolicies } = useQuery({
    queryKey: ['org-perm', 'field-permissions', { page: 1, size: 50 }],
    queryFn: () => listFieldPermissionPolicies({ page: 1, size: 50 }),
  })

  const previewIdentity: PermissionIdentityPayload = useMemo(
    () => ({
      tenantId: previewForm.tenantId,
      personId: previewForm.personId,
      organizationId: previewForm.organizationId || undefined,
      departmentId: previewForm.departmentId || undefined,
      positionId: previewForm.positionId,
      roleIds: splitRoleIds(previewForm.roleIds),
    }),
    [previewForm],
  )

  const createRowMutation = useMutation({
    mutationFn: () => createDataPermissionPolicy(rowForm),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'data-permissions'],
      })
    },
  })

  const createFieldMutation = useMutation({
    mutationFn: () => createFieldPermissionPolicy(fieldForm),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'field-permissions'],
      })
    },
  })

  const previewRowMutation = useMutation({
    mutationFn: () =>
      previewRowPermission({
        businessObject: previewForm.businessObject,
        identityContext: previewIdentity,
      }),
    onSuccess: setRowDecision,
  })

  const previewFieldMutation = useMutation({
    mutationFn: () =>
      previewFieldMask({
        businessObject: previewForm.businessObject,
        usageScenario: previewForm.usageScenario,
        identityContext: previewIdentity,
        row: JSON.parse(previewForm.sampleRow) as Record<string, unknown>,
      }),
    onSuccess: setFieldMask,
  })

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>org.perm.dataPermission.title</Badge>
          <CardTitle className="mt-3 text-2xl">数据权限配置</CardTitle>
          <CardDescription className="mt-2">
            行范围、字段动作和最终裁剪结果。
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-6 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>行级策略</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-3 md:grid-cols-2">
              <select
                className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
                onChange={(event) =>
                  setRowForm((current) => ({
                    ...current,
                    subjectType: event.target.value as PermissionSubjectType,
                  }))
                }
                value={rowForm.subjectType}
              >
                {SUBJECT_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
              <Input
                onChange={(event) =>
                  setRowForm((current) => ({
                    ...current,
                    subjectId: event.target.value,
                  }))
                }
                placeholder="subjectId"
                value={rowForm.subjectId}
              />
              <Input
                onChange={(event) =>
                  setRowForm((current) => ({
                    ...current,
                    businessObject: event.target.value,
                  }))
                }
                placeholder="business_object"
                value={rowForm.businessObject}
              />
              <select
                className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
                onChange={(event) =>
                  setRowForm((current) => ({
                    ...current,
                    scopeType: event.target
                      .value as DataPermissionPolicyPayload['scopeType'],
                  }))
                }
                value={rowForm.scopeType}
              >
                {ROW_SCOPE_TYPES.map((scope) => (
                  <option key={scope} value={scope}>
                    {scope}
                  </option>
                ))}
              </select>
              <Input
                onChange={(event) =>
                  setRowForm((current) => ({
                    ...current,
                    conditionExpr: event.target.value,
                  }))
                }
                placeholder="department_id = '{departmentId}'"
                value={rowForm.conditionExpr ?? ''}
              />
              <Input
                onChange={(event) =>
                  setRowForm((current) => ({
                    ...current,
                    priority: Number(event.target.value),
                  }))
                }
                type="number"
                value={rowForm.priority}
              />
            </div>
            <Button
              disabled={
                !rowForm.subjectId ||
                !rowForm.businessObject ||
                createRowMutation.isPending
              }
              onClick={() => createRowMutation.mutate()}
            >
              <Plus className="mr-2 h-4 w-4" />
              保存行策略
            </Button>
            <div className="space-y-2">
              {rowPolicies?.items.map((policy) => (
                <div
                  className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm"
                  key={policy.id}
                >
                  <span>
                    {policy.code} · {policy.subjectType} · {policy.scopeType}
                  </span>
                  <Badge variant={policy.enabled ? 'success' : 'secondary'}>
                    {formatUtcToUserTimezone(policy.updatedAtUtc)}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>字段策略</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-3 md:grid-cols-2">
              <select
                className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
                onChange={(event) =>
                  setFieldForm((current) => ({
                    ...current,
                    subjectType: event.target.value as PermissionSubjectType,
                  }))
                }
                value={fieldForm.subjectType}
              >
                {SUBJECT_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
              <Input
                onChange={(event) =>
                  setFieldForm((current) => ({
                    ...current,
                    subjectId: event.target.value,
                  }))
                }
                placeholder="subjectId"
                value={fieldForm.subjectId}
              />
              <Input
                onChange={(event) =>
                  setFieldForm((current) => ({
                    ...current,
                    businessObject: event.target.value,
                  }))
                }
                placeholder="business_object"
                value={fieldForm.businessObject}
              />
              <Input
                onChange={(event) =>
                  setFieldForm((current) => ({
                    ...current,
                    usageScenario: event.target.value,
                  }))
                }
                placeholder="usageScenario"
                value={fieldForm.usageScenario}
              />
              <Input
                onChange={(event) =>
                  setFieldForm((current) => ({
                    ...current,
                    fieldCode: event.target.value,
                  }))
                }
                placeholder="fieldCode"
                value={fieldForm.fieldCode}
              />
              <select
                className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
                onChange={(event) =>
                  setFieldForm((current) => ({
                    ...current,
                    action: event.target
                      .value as FieldPermissionPayload['action'],
                  }))
                }
                value={fieldForm.action}
              >
                {FIELD_ACTIONS.map((action) => (
                  <option key={action} value={action}>
                    {action}
                  </option>
                ))}
              </select>
            </div>
            <Button
              disabled={
                !fieldForm.subjectId ||
                !fieldForm.businessObject ||
                !fieldForm.fieldCode ||
                createFieldMutation.isPending
              }
              onClick={() => createFieldMutation.mutate()}
            >
              <Plus className="mr-2 h-4 w-4" />
              保存字段策略
            </Button>
            <div className="space-y-2">
              {fieldPolicies?.items.map((policy) => (
                <div
                  className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm"
                  key={policy.id}
                >
                  <span>
                    {policy.businessObject}.{policy.fieldCode} · {policy.action}
                  </span>
                  <Badge
                    variant={
                      policy.effect === 'ALLOW' ? 'success' : 'secondary'
                    }
                  >
                    {policy.subjectType}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>权限预览</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-3">
            {(
              [
                'tenantId',
                'personId',
                'positionId',
                'organizationId',
                'departmentId',
                'roleIds',
              ] as const
            ).map((field) => (
              <Input
                key={field}
                onChange={(event) =>
                  setPreviewForm((current) => ({
                    ...current,
                    [field]: event.target.value,
                  }))
                }
                placeholder={field}
                value={previewForm[field]}
              />
            ))}
            <Input
              onChange={(event) =>
                setPreviewForm((current) => ({
                  ...current,
                  businessObject: event.target.value,
                }))
              }
              placeholder="businessObject"
              value={previewForm.businessObject}
            />
            <Input
              onChange={(event) =>
                setPreviewForm((current) => ({
                  ...current,
                  usageScenario: event.target.value,
                }))
              }
              placeholder="usageScenario"
              value={previewForm.usageScenario}
            />
          </div>
          <textarea
            className="min-h-24 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            onChange={(event) =>
              setPreviewForm((current) => ({
                ...current,
                sampleRow: event.target.value,
              }))
            }
            value={previewForm.sampleRow}
          />
          <div className="flex flex-wrap gap-3">
            <Button
              disabled={
                !previewForm.tenantId ||
                !previewForm.personId ||
                !previewForm.positionId ||
                previewRowMutation.isPending
              }
              onClick={() => previewRowMutation.mutate()}
            >
              <Search className="mr-2 h-4 w-4" />
              预览行权限
            </Button>
            <Button
              disabled={
                !previewForm.tenantId ||
                !previewForm.personId ||
                !previewForm.positionId ||
                previewFieldMutation.isPending
              }
              onClick={() => previewFieldMutation.mutate()}
            >
              <ShieldCheck className="mr-2 h-4 w-4" />
              验证字段权限
            </Button>
          </div>
          {rowDecision ? (
            <div className="rounded-lg border border-slate-200 p-4 text-sm">
              <Badge variant={rowDecision.allowed ? 'success' : 'secondary'}>
                {rowDecision.allowed ? '允许' : '拒绝'}
              </Badge>
              <code className="ml-3 text-slate-700">
                {rowDecision.sqlCondition}
              </code>
            </div>
          ) : null}
          {fieldMask ? (
            <div className="grid gap-3 md:grid-cols-2">
              <pre className="overflow-auto rounded-lg bg-slate-950 p-4 text-xs text-slate-50">
                {JSON.stringify(fieldMask.row, null, 2)}
              </pre>
              <div className="rounded-lg border border-slate-200 p-4 text-sm text-slate-700">
                hidden {fieldMask.decision.hiddenFields.join(', ') || '-'} ·
                masked {fieldMask.decision.desensitizedFields.join(', ') || '-'}
              </div>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
