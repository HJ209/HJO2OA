import { useEffect, useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Save, ShieldCheck } from 'lucide-react'
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
import { RoleResourceTree } from '@/features/org-perm/components/role-resource-tree'
import { useRoleResources } from '@/features/org-perm/hooks/use-role-resources'
import {
  decideApiPermission,
  listRoles,
  saveResourceDefinition,
  saveRoleResources,
} from '@/features/org-perm/services/role-resource-service'
import type {
  ApiPermissionDecision,
  ResourceAction,
  ResourceDefinitionPayload,
  ResourceNode,
  ResourceType,
} from '@/features/org-perm/types/org-perm'

const EMPTY_RESOURCES: ResourceNode[] = []
const RESOURCE_TYPES: ResourceType[] = [
  'MENU',
  'BUTTON',
  'API',
  'DATA_RESOURCE',
]
const API_ACTIONS: ResourceAction[] = ['READ', 'CREATE', 'UPDATE', 'DELETE']

function collectCheckedIds(nodes: ResourceNode[]): string[] {
  return nodes.flatMap(function collect(node: ResourceNode): string[] {
    return [
      ...(node.checked ? [node.id] : []),
      ...(node.children ?? []).flatMap(collect),
    ]
  })
}

function areSameIds(left: string[], right: string[]): boolean {
  return (
    left.length === right.length &&
    left.every((item, index) => item === right[index])
  )
}

export default function RoleAuthPage(): ReactElement {
  const queryClient = useQueryClient()
  const authState = useAuthStore()
  const identityState = useIdentityStore()
  const [selectedRoleId, setSelectedRoleId] = useState<string>()
  const [checkedIds, setCheckedIds] = useState<string[]>([])
  const [resourceForm, setResourceForm] = useState<ResourceDefinitionPayload>({
    resourceType: 'MENU',
    resourceCode: '',
    name: '',
    parentCode: '',
    sortOrder: 0,
  })
  const [verifyForm, setVerifyForm] = useState({
    tenantId: authState.user?.tenantId ?? '',
    personId: authState.user?.id ?? '',
    positionId: identityState.currentAssignment?.positionId ?? '',
    resourceCode: '/api/v1/org/roles',
    action: 'READ' as ResourceAction,
  })
  const [decision, setDecision] = useState<ApiPermissionDecision>()
  const { data: roles } = useQuery({
    queryKey: ['org-perm', 'roles', { page: 1, size: 100 }],
    queryFn: () => listRoles({ page: 1, size: 100 }),
  })
  const { data: resources = EMPTY_RESOURCES } = useRoleResources(selectedRoleId)

  const selectedRole = useMemo(
    () => roles?.items.find((role) => role.id === selectedRoleId),
    [roles?.items, selectedRoleId],
  )

  const saveResourcesMutation = useMutation({
    mutationFn: () =>
      selectedRoleId
        ? saveRoleResources(selectedRoleId, resources, checkedIds)
        : Promise.resolve(resources),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'role-resources', selectedRoleId],
      })
    },
  })

  const saveResourceMutation = useMutation({
    mutationFn: () => saveResourceDefinition(resourceForm),
    onSuccess: () => {
      setResourceForm((current) => ({
        ...current,
        resourceCode: '',
        name: '',
        parentCode: '',
      }))
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'role-resources', selectedRoleId],
      })
    },
  })

  const verifyMutation = useMutation({
    mutationFn: () => decideApiPermission(verifyForm),
    onSuccess: setDecision,
  })

  useEffect(() => {
    const firstRoleId = roles?.items[0]?.id

    if (!selectedRoleId && firstRoleId) {
      setSelectedRoleId(firstRoleId)
    }
  }, [roles, selectedRoleId])

  useEffect(() => {
    const nextCheckedIds = collectCheckedIds(resources)

    setCheckedIds((currentCheckedIds) =>
      areSameIds(currentCheckedIds, nextCheckedIds)
        ? currentCheckedIds
        : nextCheckedIds,
    )
  }, [resources])

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>org.perm.role.title</Badge>
          <CardTitle className="mt-3 text-2xl">角色资源授权</CardTitle>
          <CardDescription className="mt-2">
            {selectedRole
              ? `${selectedRole.name} · ${selectedRole.code}`
              : '选择角色'}
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        <Card>
          <CardHeader>
            <CardTitle>角色列表</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {roles?.items.map((role) => (
              <Button
                className="w-full justify-start"
                key={role.id}
                onClick={() => setSelectedRoleId(role.id)}
                variant={selectedRoleId === role.id ? 'default' : 'ghost'}
              >
                {role.name}
              </Button>
            ))}
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <CardTitle>权限资源树</CardTitle>
                <CardDescription>
                  当前已选择 {checkedIds.length} 个资源动作。
                </CardDescription>
              </div>
              <Button
                disabled={!selectedRoleId || saveResourcesMutation.isPending}
                onClick={() => saveResourcesMutation.mutate()}
              >
                <Save className="mr-2 h-4 w-4" />
                保存授权
              </Button>
            </CardHeader>
            <CardContent>
              <RoleResourceTree
                checkedIds={checkedIds}
                nodes={resources}
                onCheckedChange={setCheckedIds}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>资源目录</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 md:grid-cols-[160px_1fr_1fr_1fr_auto]">
              <select
                className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
                onChange={(event) =>
                  setResourceForm((current) => ({
                    ...current,
                    resourceType: event.target.value as ResourceType,
                  }))
                }
                value={resourceForm.resourceType}
              >
                {RESOURCE_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
              <Input
                onChange={(event) =>
                  setResourceForm((current) => ({
                    ...current,
                    resourceCode: event.target.value,
                  }))
                }
                placeholder="resource.code"
                value={resourceForm.resourceCode}
              />
              <Input
                onChange={(event) =>
                  setResourceForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                placeholder="资源名称"
                value={resourceForm.name}
              />
              <Input
                onChange={(event) =>
                  setResourceForm((current) => ({
                    ...current,
                    parentCode: event.target.value,
                  }))
                }
                placeholder="parent.code"
                value={resourceForm.parentCode ?? ''}
              />
              <Button
                disabled={
                  !resourceForm.resourceCode ||
                  !resourceForm.name ||
                  saveResourceMutation.isPending
                }
                onClick={() => saveResourceMutation.mutate()}
              >
                <Plus className="mr-2 h-4 w-4" />
                保存资源
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>权限验证</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 md:grid-cols-[1fr_1fr_1fr_1fr_140px_auto]">
              <Input
                onChange={(event) =>
                  setVerifyForm((current) => ({
                    ...current,
                    tenantId: event.target.value,
                  }))
                }
                placeholder="tenantId"
                value={verifyForm.tenantId}
              />
              <Input
                onChange={(event) =>
                  setVerifyForm((current) => ({
                    ...current,
                    personId: event.target.value,
                  }))
                }
                placeholder="personId"
                value={verifyForm.personId}
              />
              <Input
                onChange={(event) =>
                  setVerifyForm((current) => ({
                    ...current,
                    positionId: event.target.value,
                  }))
                }
                placeholder="positionId"
                value={verifyForm.positionId}
              />
              <Input
                onChange={(event) =>
                  setVerifyForm((current) => ({
                    ...current,
                    resourceCode: event.target.value,
                  }))
                }
                placeholder="/api/v1/..."
                value={verifyForm.resourceCode}
              />
              <select
                className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
                onChange={(event) =>
                  setVerifyForm((current) => ({
                    ...current,
                    action: event.target.value as ResourceAction,
                  }))
                }
                value={verifyForm.action}
              >
                {API_ACTIONS.map((action) => (
                  <option key={action} value={action}>
                    {action}
                  </option>
                ))}
              </select>
              <Button
                disabled={
                  !verifyForm.tenantId ||
                  !verifyForm.personId ||
                  !verifyForm.positionId ||
                  verifyMutation.isPending
                }
                onClick={() => verifyMutation.mutate()}
              >
                <ShieldCheck className="mr-2 h-4 w-4" />
                验证
              </Button>
              {decision ? (
                <div className="md:col-span-6">
                  <Badge variant={decision.allowed ? 'success' : 'secondary'}>
                    {decision.allowed ? '允许' : '拒绝'}
                  </Badge>
                  <span className="ml-3 text-sm text-slate-500">
                    roles {decision.snapshot.roleIds.length} · version{' '}
                    {decision.snapshot.version}
                  </span>
                </div>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
