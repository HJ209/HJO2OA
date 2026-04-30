import { useState, type ReactElement } from 'react'
import { Eye } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { AuditFilter } from '@/features/infra-admin/components/audit-filter'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import {
  useAuditRecordDetail,
  useAuditRecords,
} from '@/features/infra-admin/hooks/use-audit'
import {
  buildAuditQuery,
  type AuditQuery,
} from '@/features/infra-admin/services/audit-service'
import type { AuditFilterValues } from '@/features/infra-admin/types/infra'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function AuditPage(): ReactElement {
  const [query, setQuery] = useState<AuditQuery>({
    page: 1,
    size: 20,
  })
  const [selectedRecordId, setSelectedRecordId] = useState<string>()
  const recordsQuery = useAuditRecords(query)
  const detailQuery = useAuditRecordDetail(selectedRecordId)

  function handleSearch(values: AuditFilterValues): void {
    setQuery(buildAuditQuery(values))
    setSelectedRecordId(undefined)
  }

  return (
    <InfraPageSection
      description="Automatic write-operation audit records with request, operator, target, and field summaries."
      title="Audit Logs"
    >
      <div className="space-y-6">
        <AuditFilter onSearch={handleSearch} />
        <InfraTable
          columns={[
            {
              key: 'module',
              title: 'Module',
              render: (item) => item.moduleCode,
            },
            {
              key: 'action',
              title: 'Action',
              render: (item) => item.actionType,
            },
            {
              key: 'operator',
              title: 'Operator',
              render: (item) => item.operatorAccountId ?? '-',
            },
            {
              key: 'target',
              title: 'Target',
              render: (item) => `${item.objectType}:${item.objectId}`,
            },
            {
              key: 'request',
              title: 'Request',
              render: (item) => item.traceId ?? '-',
            },
            {
              key: 'occurredAt',
              title: 'Occurred',
              render: (item) => formatUtcToUserTimezone(item.occurredAt),
            },
            {
              key: 'actions',
              title: 'Actions',
              render: (item) => (
                <Button
                  onClick={() => setSelectedRecordId(item.id)}
                  size="sm"
                  variant="outline"
                >
                  <Eye className="h-4 w-4" />
                  Detail
                </Button>
              ),
            },
          ]}
          getRowKey={(item) => item.id}
          isLoading={recordsQuery.isLoading}
          items={recordsQuery.data?.items ?? []}
        />

        {selectedRecordId ? (
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-semibold text-slate-900">Audit Detail</h2>
              <Button
                onClick={() => setSelectedRecordId(undefined)}
                size="sm"
                variant="outline"
              >
                Close
              </Button>
            </div>
            {detailQuery.isLoading ? (
              <p className="text-slate-500">Loading detail...</p>
            ) : detailQuery.data ? (
              <div className="space-y-4">
                <dl className="grid gap-3 md:grid-cols-3">
                  <div>
                    <dt className="text-xs uppercase text-slate-500">
                      Summary
                    </dt>
                    <dd>{detailQuery.data.summary ?? '-'}</dd>
                  </div>
                  <div>
                    <dt className="text-xs uppercase text-slate-500">Tenant</dt>
                    <dd className="break-all">
                      {detailQuery.data.tenantId ?? '-'}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs uppercase text-slate-500">Person</dt>
                    <dd className="break-all">
                      {detailQuery.data.operatorPersonId ?? '-'}
                    </dd>
                  </div>
                </dl>
                <InfraTable
                  columns={[
                    {
                      key: 'field',
                      title: 'Field',
                      render: (item) => item.fieldName,
                    },
                    {
                      key: 'old',
                      title: 'Before',
                      render: (item) => item.oldValue ?? '-',
                    },
                    {
                      key: 'new',
                      title: 'After',
                      render: (item) => item.newValue ?? '-',
                    },
                    {
                      key: 'level',
                      title: 'Level',
                      render: (item) => item.sensitivityLevel ?? '-',
                    },
                  ]}
                  getRowKey={(item) => item.id}
                  items={detailQuery.data.fieldChanges}
                />
              </div>
            ) : (
              <p className="text-slate-500">No detail found.</p>
            )}
          </div>
        ) : null}
      </div>
    </InfraPageSection>
  )
}
