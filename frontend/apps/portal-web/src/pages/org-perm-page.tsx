import type { ReactElement } from 'react'
import { Shield, Users } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useIdentity } from '@/hooks/use-identity'

const COPY = {
  titleKey: 'org.perm.title',
  titleText: '组织与权限',
  descriptionKey: 'org.perm.description',
  descriptionText: '查看当前身份、组织归属和角色授权覆盖情况。',
} as const

export default function OrgPermPage(): ReactElement {
  const { currentAssignment, orgId, roleIds } = useIdentity()

  return (
    <div className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
      <Card>
        <CardHeader>
          <Badge>{COPY.titleText}</Badge>
          <CardTitle className="mt-3 flex items-center gap-2 text-2xl">
            <Users className="h-6 w-6 text-sky-600" />
            {COPY.titleText}
          </CardTitle>
          <CardDescription className="mt-2 text-base">
            {COPY.descriptionText}
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="rounded-2xl bg-slate-50 p-4">
            <p className="text-sm text-slate-500">当前任职关系</p>
            <strong className="mt-3 block text-lg text-slate-950">
              {currentAssignment?.positionName ?? '未选择身份'}
            </strong>
            <p className="mt-2 text-sm text-slate-500">
              assignmentId：{currentAssignment?.assignmentId ?? 'N/A'}
            </p>
          </div>
          <div className="rounded-2xl bg-slate-50 p-4">
            <p className="text-sm text-slate-500">组织归属</p>
            <strong className="mt-3 block text-lg text-slate-950">
              {currentAssignment?.orgName ?? '未绑定组织'}
            </strong>
            <p className="mt-2 text-sm text-slate-500">
              orgId：{orgId ?? 'N/A'}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-xl">
            <Shield className="h-5 w-5 text-sky-600" />
            角色授权摘要
          </CardTitle>
          <CardDescription>
            角色信息来自 identity-store，可在后续接入真实身份上下文接口。
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          {roleIds.map((roleId) => (
            <Badge key={roleId} variant="secondary">
              {roleId}
            </Badge>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
