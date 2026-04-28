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
import { listPositionAssignments } from '@/features/org-perm/services/position-assignment-service'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function PositionPage(): ReactElement {
  const { data } = useQuery({
    queryKey: ['org-perm', 'positions', { page: 1, size: 20 }],
    queryFn: () => listPositionAssignments({ page: 1, size: 20 }),
  })

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>org.perm.position.title</Badge>
          <CardTitle className="mt-3 text-2xl">岗位任职</CardTitle>
          <CardDescription className="mt-2">
            查看人员岗位任职关系，支持主岗标记和生效周期展示。
          </CardDescription>
        </CardHeader>
      </Card>

      <Card>
        <CardContent className="overflow-x-auto p-6">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b border-slate-200 text-slate-500">
              <tr>
                <th className="py-3 font-medium">人员</th>
                <th className="py-3 font-medium">组织</th>
                <th className="py-3 font-medium">岗位</th>
                <th className="py-3 font-medium">主岗</th>
                <th className="py-3 font-medium">生效时间</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.items.map((assignment) => (
                <tr key={assignment.id}>
                  <td className="py-3 font-medium text-slate-950">
                    {assignment.personName}
                  </td>
                  <td className="py-3">{assignment.orgName}</td>
                  <td className="py-3">{assignment.positionName}</td>
                  <td className="py-3">
                    <Badge
                      variant={assignment.primary ? 'success' : 'secondary'}
                    >
                      {assignment.primary ? '主岗' : '兼岗'}
                    </Badge>
                  </td>
                  <td className="py-3">
                    {formatUtcToUserTimezone(assignment.effectiveFromUtc)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!data?.items.length ? (
            <div className="py-8 text-center text-sm text-slate-500">
              暂无任职数据
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
