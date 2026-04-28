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
import { listSyncAuditRecords } from '@/features/org-perm/services/sync-audit-service'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function SyncAuditPage(): ReactElement {
  const { data } = useQuery({
    queryKey: ['org-perm', 'sync-audit', { page: 1, size: 20 }],
    queryFn: () => listSyncAuditRecords({ page: 1, size: 20 }),
  })

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>org.perm.syncAudit.title</Badge>
          <CardTitle className="mt-3 text-2xl">同步审计日志</CardTitle>
          <CardDescription className="mt-2">
            查看组织、人员、岗位等同步批次的执行结果。
          </CardDescription>
        </CardHeader>
      </Card>

      <Card>
        <CardContent className="overflow-x-auto p-6">
          <table className="w-full min-w-[820px] text-left text-sm">
            <thead className="border-b border-slate-200 text-slate-500">
              <tr>
                <th className="py-3 font-medium">来源系统</th>
                <th className="py-3 font-medium">批次号</th>
                <th className="py-3 font-medium">状态</th>
                <th className="py-3 font-medium">总数</th>
                <th className="py-3 font-medium">成功</th>
                <th className="py-3 font-medium">失败</th>
                <th className="py-3 font-medium">开始时间</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.items.map((record) => (
                <tr key={record.id}>
                  <td className="py-3 font-medium text-slate-950">
                    {record.sourceSystem}
                  </td>
                  <td className="py-3">{record.batchNo}</td>
                  <td className="py-3">
                    <Badge
                      variant={
                        record.status === 'SUCCESS' ? 'success' : 'secondary'
                      }
                    >
                      {record.status}
                    </Badge>
                  </td>
                  <td className="py-3">{record.totalCount}</td>
                  <td className="py-3">{record.successCount}</td>
                  <td className="py-3">{record.failedCount}</td>
                  <td className="py-3">
                    {formatUtcToUserTimezone(record.startedAtUtc)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!data?.items.length ? (
            <div className="py-8 text-center text-sm text-slate-500">
              暂无同步审计记录
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
