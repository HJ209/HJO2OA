import { useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
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
import { PersonFormDialog } from '@/features/org-perm/components/person-form-dialog'
import { usePersonList } from '@/features/org-perm/hooks/use-person-list'
import {
  createPersonAccount,
  deletePersonAccount,
  updatePersonAccount,
} from '@/features/org-perm/services/person-account-service'
import type {
  PersonAccount,
  PersonAccountPayload,
} from '@/features/org-perm/types/org-perm'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function PersonListPage(): ReactElement {
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [editingPerson, setEditingPerson] = useState<PersonAccount>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const query = { page: 1, size: 20, keyword }
  const { data, isLoading } = usePersonList(query)

  const saveMutation = useMutation({
    mutationFn: (payload: PersonAccountPayload) =>
      editingPerson
        ? updatePersonAccount(editingPerson.id, payload)
        : createPersonAccount(payload),
    onSuccess: () => {
      setDialogOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['org-perm', 'persons'] })
    },
  })
  const deleteMutation = useMutation({
    mutationFn: (personId: string) => deletePersonAccount(personId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['org-perm', 'persons'] })
    },
  })

  function openCreateDialog(): void {
    setEditingPerson(undefined)
    setDialogOpen(true)
  }

  function openEditDialog(person: PersonAccount): void {
    setEditingPerson(person)
    setDialogOpen(true)
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex-row items-start justify-between gap-4">
          <div>
            <Badge>org.perm.person.title</Badge>
            <CardTitle className="mt-3 text-2xl">人员账号</CardTitle>
            <CardDescription className="mt-2">
              查询人员账号，维护账号资料和启停状态。
            </CardDescription>
          </div>
          <Button onClick={openCreateDialog}>
            <Plus className="h-4 w-4" />
            新增人员
          </Button>
        </CardHeader>
      </Card>

      <Card>
        <CardHeader>
          <div className="relative max-w-sm">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              className="pl-9"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索账号、姓名、邮箱"
              value={keyword}
            />
          </div>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b border-slate-200 text-slate-500">
              <tr>
                <th className="py-3 font-medium">账号</th>
                <th className="py-3 font-medium">姓名</th>
                <th className="py-3 font-medium">组织</th>
                <th className="py-3 font-medium">状态</th>
                <th className="py-3 font-medium">更新时间</th>
                <th className="py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.items.map((person) => (
                <tr key={person.id}>
                  <td className="py-3 font-medium text-slate-950">
                    {person.accountName}
                  </td>
                  <td className="py-3">{person.displayName}</td>
                  <td className="py-3">{person.orgName ?? '-'}</td>
                  <td className="py-3">
                    <Badge
                      variant={
                        person.status === 'ACTIVE' ? 'success' : 'secondary'
                      }
                    >
                      {person.status}
                    </Badge>
                  </td>
                  <td className="py-3">
                    {formatUtcToUserTimezone(person.updatedAtUtc)}
                  </td>
                  <td className="flex gap-2 py-3">
                    <Button
                      onClick={() => openEditDialog(person)}
                      size="sm"
                      variant="outline"
                    >
                      编辑
                    </Button>
                    <Button
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(person.id)}
                      size="sm"
                      variant="outline"
                    >
                      删除
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!isLoading && !data?.items.length ? (
            <div className="py-8 text-center text-sm text-slate-500">
              暂无人员数据
            </div>
          ) : null}
        </CardContent>
      </Card>

      <PersonFormDialog
        onClose={() => setDialogOpen(false)}
        onSubmit={(payload) => saveMutation.mutate(payload)}
        open={dialogOpen}
        person={editingPerson}
        submitting={saveMutation.isPending}
      />
    </div>
  )
}
