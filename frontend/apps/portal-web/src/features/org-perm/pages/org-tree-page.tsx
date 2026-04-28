import { useState, type FormEvent, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
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
import { OrgTree } from '@/features/org-perm/components/org-tree'
import {
  ORG_TREE_QUERY_KEY,
  useOrgTree,
} from '@/features/org-perm/hooks/use-org-tree'
import {
  createOrgStructure,
  updateOrgStructure,
} from '@/features/org-perm/services/org-structure-service'
import type {
  OrgStatus,
  OrgStructure,
  OrgStructurePayload,
} from '@/features/org-perm/types/org-perm'

const DEFAULT_ORG_FORM: OrgStructurePayload = {
  parentId: null,
  name: '',
  code: '',
  type: 'DEPARTMENT',
  status: 'ACTIVE',
  sortOrder: 0,
}

function toOrgForm(org?: OrgStructure): OrgStructurePayload {
  if (!org) {
    return DEFAULT_ORG_FORM
  }

  return {
    parentId: org.parentId ?? null,
    name: org.name,
    code: org.code,
    type: org.type,
    status: org.status,
    sortOrder: org.sortOrder,
  }
}

export default function OrgTreePage(): ReactElement {
  const queryClient = useQueryClient()
  const { data = [], isLoading } = useOrgTree()
  const [selectedOrg, setSelectedOrg] = useState<OrgStructure>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [formValue, setFormValue] =
    useState<OrgStructurePayload>(DEFAULT_ORG_FORM)

  const saveMutation = useMutation({
    mutationFn: (payload: OrgStructurePayload) =>
      selectedOrg
        ? updateOrgStructure(selectedOrg.id, payload)
        : createOrgStructure(payload),
    onSuccess: () => {
      setDialogOpen(false)
      void queryClient.invalidateQueries({ queryKey: ORG_TREE_QUERY_KEY })
    },
  })

  function openCreateDialog(): void {
    setSelectedOrg(undefined)
    setFormValue(DEFAULT_ORG_FORM)
    setDialogOpen(true)
  }

  function openEditDialog(): void {
    setFormValue(toOrgForm(selectedOrg))
    setDialogOpen(true)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    saveMutation.mutate(formValue)
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex-row items-start justify-between gap-4">
          <div>
            <Badge>org.perm.tree.title</Badge>
            <CardTitle className="mt-3 text-2xl">组织架构</CardTitle>
            <CardDescription className="mt-2">
              浏览组织树，维护组织节点、编码、状态和排序。
            </CardDescription>
          </div>
          <Button onClick={openCreateDialog}>
            <Plus className="h-4 w-4" />
            新增组织
          </Button>
        </CardHeader>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Card>
          <CardContent className="p-4">
            {isLoading ? (
              <div className="p-6 text-sm text-slate-500">加载组织树中</div>
            ) : (
              <OrgTree
                nodes={data}
                onSelect={setSelectedOrg}
                selectedId={selectedOrg?.id}
              />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>节点详情</CardTitle>
            <CardDescription>选中组织后可编辑基础信息。</CardDescription>
          </CardHeader>
          <CardContent>
            {selectedOrg ? (
              <div className="space-y-3 text-sm">
                <p className="font-semibold text-slate-950">
                  {selectedOrg.name}
                </p>
                <p className="text-slate-500">编码：{selectedOrg.code}</p>
                <p className="text-slate-500">类型：{selectedOrg.type}</p>
                <p className="text-slate-500">状态：{selectedOrg.status}</p>
                <Button className="mt-2 w-full" onClick={openEditDialog}>
                  编辑节点
                </Button>
              </div>
            ) : (
              <p className="text-sm text-slate-500">请选择组织节点</p>
            )}
          </CardContent>
        </Card>
      </div>

      {dialogOpen ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4"
          role="dialog"
        >
          <form
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onSubmit={handleSubmit}
          >
            <h2 className="text-lg font-semibold text-slate-950">
              {selectedOrg ? '编辑组织' : '新增组织'}
            </h2>
            <div className="mt-5 grid gap-4">
              <Input
                onChange={(event) =>
                  setFormValue((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                placeholder="组织名称"
                required
                value={formValue.name}
              />
              <Input
                onChange={(event) =>
                  setFormValue((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
                placeholder="组织编码"
                required
                value={formValue.code}
              />
              <Input
                onChange={(event) =>
                  setFormValue((current) => ({
                    ...current,
                    parentId: event.target.value || null,
                  }))
                }
                placeholder="父级组织 ID"
                value={formValue.parentId ?? ''}
              />
              <select
                className="h-10 rounded-xl border border-slate-200 px-3 text-sm"
                onChange={(event) =>
                  setFormValue((current) => ({
                    ...current,
                    status: event.target.value as OrgStatus,
                  }))
                }
                value={formValue.status}
              >
                <option value="ACTIVE">启用</option>
                <option value="DISABLED">停用</option>
              </select>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button onClick={() => setDialogOpen(false)} variant="outline">
                取消
              </Button>
              <Button disabled={saveMutation.isPending} type="submit">
                保存
              </Button>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  )
}
