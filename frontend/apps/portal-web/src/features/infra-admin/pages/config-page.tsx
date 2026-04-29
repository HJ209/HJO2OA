import { useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { ConfigEntryDialog } from '@/features/infra-admin/components/config-entry-dialog'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useConfigEntries } from '@/features/infra-admin/hooks/use-config'
import { configService } from '@/features/infra-admin/services/config-service'
import type { ConfigEntry } from '@/features/infra-admin/types/infra'

export default function ConfigPage(): ReactElement {
  const [editingEntry, setEditingEntry] = useState<ConfigEntry | undefined>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const queryClient = useQueryClient()
  const entriesQuery = useConfigEntries({ page: 1, size: 20 })
  const mutation = useMutation({
    mutationFn: (entry: ConfigEntry) =>
      editingEntry
        ? configService.update(editingEntry.id ?? editingEntry.key, entry)
        : configService.create(entry),
    onSuccess: async () => {
      setDialogOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['infra', 'config'] })
    },
  })

  return (
    <InfraPageSection
      actions={
        <Button
          onClick={() => {
            setEditingEntry(undefined)
            setDialogOpen(true)
          }}
        >
          新建配置
        </Button>
      }
      description="维护运行时配置项，写操作自动携带幂等键并防重复提交。"
      title="配置项管理"
    >
      <InfraTable
        columns={[
          { key: 'key', title: '键', render: (item) => item.key },
          { key: 'group', title: '分组', render: (item) => item.group },
          { key: 'value', title: '值', render: (item) => item.value },
          {
            key: 'encrypted',
            title: '敏感',
            render: (item) => (
              <StatusPill active={item.encrypted}>
                {item.encrypted ? '是' : '否'}
              </StatusPill>
            ),
          },
          {
            key: 'actions',
            title: '操作',
            render: (item) => (
              <Button
                onClick={() => {
                  setEditingEntry(item)
                  setDialogOpen(true)
                }}
                size="sm"
                variant="outline"
              >
                编辑
              </Button>
            ),
          },
        ]}
        getRowKey={(item) => item.key}
        isLoading={entriesQuery.isLoading}
        items={entriesQuery.data?.items ?? []}
      />
      <ConfigEntryDialog
        entry={editingEntry}
        isSubmitting={mutation.isPending}
        onClose={() => setDialogOpen(false)}
        onSubmit={(entry) => mutation.mutate(entry)}
        open={dialogOpen}
      />
    </InfraPageSection>
  )
}
