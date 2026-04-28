import { useEffect, useState, type ReactElement } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { RoleResourceTree } from '@/features/org-perm/components/role-resource-tree'
import { useRoleResources } from '@/features/org-perm/hooks/use-role-resources'
import { listRoles } from '@/features/org-perm/services/role-resource-service'
import type { ResourceNode } from '@/features/org-perm/types/org-perm'

export default function RoleAuthPage(): ReactElement {
  const [selectedRoleId, setSelectedRoleId] = useState<string>()
  const [checkedIds, setCheckedIds] = useState<string[]>([])
  const { data: roles } = useQuery({
    queryKey: ['org-perm', 'roles', { page: 1, size: 20 }],
    queryFn: () => listRoles({ page: 1, size: 20 }),
  })
  const { data: resources = [] } = useRoleResources(selectedRoleId)

  useEffect(() => {
    const firstRoleId = roles?.items[0]?.id

    if (!selectedRoleId && firstRoleId) {
      setSelectedRoleId(firstRoleId)
    }
  }, [roles, selectedRoleId])

  useEffect(() => {
    setCheckedIds(
      resources.flatMap(function collect(node: ResourceNode): string[] {
        return [
          ...(node.checked ? [node.id] : []),
          ...(node.children ?? []).flatMap(collect),
        ]
      }),
    )
  }, [resources])

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>org.perm.role.title</Badge>
          <CardTitle className="mt-3 text-2xl">角色资源授权</CardTitle>
          <CardDescription className="mt-2">
            选择角色后勾选菜单、按钮和 API 资源权限。
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

        <Card>
          <CardHeader>
            <CardTitle>权限资源树</CardTitle>
            <CardDescription>
              当前已选择 {checkedIds.length} 个资源节点。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <RoleResourceTree
              checkedIds={checkedIds}
              nodes={resources}
              onCheckedChange={setCheckedIds}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
