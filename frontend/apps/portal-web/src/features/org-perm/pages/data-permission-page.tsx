import type { ReactElement } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { listDataPermissionPolicies } from '@/features/org-perm/services/data-permission-service'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function DataPermissionPage(): ReactElement {
  const { data } = useQuery({
    queryKey: ['org-perm', 'data-permissions', { page: 1, size: 20 }],
    queryFn: () => listDataPermissionPolicies({ page: 1, size: 20 }),
  })

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>org.perm.dataPermission.title</Badge>
          <CardTitle className="mt-3 text-2xl">数据权限配置</CardTitle>
          <CardDescription className="mt-2">
            管理角色关联的数据权限策略和可见范围。
          </CardDescription>
        </CardHeader>
      </Card>

      <Card>
        <CardContent className="grid gap-3 p-6">
          {data?.items.map((policy) => (
            <div
              className="flex flex-col gap-3 rounded-xl border border-slate-200 p-4 md:flex-row md:items-center md:justify-between"
              key={policy.id}
            >
              <div>
                <p className="font-semibold text-slate-950">{policy.name}</p>
                <p className="mt-1 text-sm text-slate-500">
                  {policy.code} · {policy.scopeType}
                </p>
              </div>
              <div className="flex items-center gap-3 text-sm text-slate-500">
                <Badge variant={policy.enabled ? 'success' : 'secondary'}>
                  {policy.enabled ? '启用' : '停用'}
                </Badge>
                <span>{formatUtcToUserTimezone(policy.updatedAtUtc)}</span>
              </div>
            </div>
          ))}
          {!data?.items.length ? (
            <div className="py-8 text-center text-sm text-slate-500">
              暂无数据权限策略
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
