import { useMemo, useState, type FormEvent, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, GitBranch, Plus, Power, Save, Trash2 } from 'lucide-react'
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
  createDepartment,
  createOrgStructure,
  deleteDepartment,
  deleteOrgStructure,
  exportOrgMasterData,
  listDepartments,
  moveDepartment,
  moveOrgStructure,
  setDepartmentEnabled,
  setOrgStructureEnabled,
  updateDepartment,
  updateOrgStructure,
} from '@/features/org-perm/services/org-structure-service'
import type {
  Department,
  DepartmentPayload,
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

const DEFAULT_DEPARTMENT_FORM: DepartmentPayload = {
  organizationId: '',
  parentId: null,
  name: '',
  code: '',
  managerId: null,
  sortOrder: 0,
}

function flattenOrganizations(nodes: OrgStructure[]): OrgStructure[] {
  return nodes.flatMap((node) => [
    node,
    ...flattenOrganizations(node.children ?? []),
  ])
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

function toDepartmentForm(
  organizationId: string,
  department?: Department,
): DepartmentPayload {
  if (!department) {
    return {
      ...DEFAULT_DEPARTMENT_FORM,
      organizationId,
    }
  }

  return {
    organizationId: department.organizationId,
    parentId: department.parentId ?? null,
    name: department.name,
    code: department.code,
    managerId: department.managerId ?? null,
    sortOrder: department.sortOrder,
  }
}

export default function OrgTreePage(): ReactElement {
  const queryClient = useQueryClient()
  const { data = [], isLoading } = useOrgTree()
  const flatOrganizations = useMemo(() => flattenOrganizations(data), [data])
  const [selectedOrg, setSelectedOrg] = useState<OrgStructure>()
  const [editingOrg, setEditingOrg] = useState<OrgStructure>()
  const [orgDialogOpen, setOrgDialogOpen] = useState(false)
  const [orgForm, setOrgForm] = useState<OrgStructurePayload>(DEFAULT_ORG_FORM)
  const [selectedDepartment, setSelectedDepartment] = useState<Department>()
  const [departmentForm, setDepartmentForm] = useState<DepartmentPayload>(
    DEFAULT_DEPARTMENT_FORM,
  )
  const [exportPreview, setExportPreview] = useState('')

  const departmentsQuery = useQuery({
    enabled: Boolean(selectedOrg?.id),
    queryKey: ['org-perm', 'departments', selectedOrg?.id],
    queryFn: () => listDepartments(selectedOrg?.id ?? ''),
  })

  const invalidateStructure = (): void => {
    void queryClient.invalidateQueries({ queryKey: ORG_TREE_QUERY_KEY })
    if (selectedOrg?.id) {
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'departments', selectedOrg.id],
      })
    }
  }

  const saveOrgMutation = useMutation({
    mutationFn: (payload: OrgStructurePayload) =>
      editingOrg
        ? updateOrgStructure(editingOrg.id, payload)
        : createOrgStructure(payload),
    onSuccess: () => {
      setOrgDialogOpen(false)
      invalidateStructure()
    },
  })

  const orgStatusMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      setOrgStructureEnabled(id, enabled),
    onSuccess: invalidateStructure,
  })

  const orgMoveMutation = useMutation({
    mutationFn: (payload: {
      id: string
      parentId?: string | null
      sortOrder?: number
    }) =>
      moveOrgStructure(payload.id, {
        parentId: payload.parentId,
        sortOrder: payload.sortOrder,
      }),
    onSuccess: invalidateStructure,
  })

  const orgDeleteMutation = useMutation({
    mutationFn: (id: string) => deleteOrgStructure(id),
    onSuccess: () => {
      setSelectedOrg(undefined)
      invalidateStructure()
    },
  })

  const saveDepartmentMutation = useMutation({
    mutationFn: (payload: DepartmentPayload) =>
      selectedDepartment
        ? updateDepartment(selectedDepartment.id, payload)
        : createDepartment(payload),
    onSuccess: () => {
      setSelectedDepartment(undefined)
      setDepartmentForm(
        selectedOrg
          ? toDepartmentForm(selectedOrg.id)
          : DEFAULT_DEPARTMENT_FORM,
      )
      invalidateStructure()
    },
  })

  const departmentStatusMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      setDepartmentEnabled(id, enabled),
    onSuccess: invalidateStructure,
  })

  const departmentMoveMutation = useMutation({
    mutationFn: (payload: {
      id: string
      parentId?: string | null
      sortOrder?: number
    }) =>
      moveDepartment(payload.id, {
        parentId: payload.parentId,
        sortOrder: payload.sortOrder,
      }),
    onSuccess: invalidateStructure,
  })

  const departmentDeleteMutation = useMutation({
    mutationFn: (id: string) => deleteDepartment(id),
    onSuccess: () => {
      setSelectedDepartment(undefined)
      invalidateStructure()
    },
  })

  const exportMutation = useMutation({
    mutationFn: exportOrgMasterData,
    onSuccess: (data) => {
      setExportPreview(JSON.stringify(data, null, 2))
    },
  })

  function openCreateOrg(parentId?: string | null): void {
    setEditingOrg(undefined)
    setOrgForm({
      ...DEFAULT_ORG_FORM,
      parentId: parentId ?? null,
    })
    setOrgDialogOpen(true)
  }

  function openEditOrg(org: OrgStructure): void {
    setEditingOrg(org)
    setOrgForm(toOrgForm(org))
    setOrgDialogOpen(true)
  }

  function selectOrg(org: OrgStructure): void {
    setSelectedOrg(org)
    setSelectedDepartment(undefined)
    setDepartmentForm(toDepartmentForm(org.id))
  }

  function handleOrgSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    saveOrgMutation.mutate(orgForm)
  }

  function handleDepartmentSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!selectedOrg) {
      return
    }
    saveDepartmentMutation.mutate({
      ...departmentForm,
      organizationId: selectedOrg.id,
    })
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex-row items-start justify-between gap-3">
          <div>
            <Badge>Org master data</Badge>
            <CardTitle className="mt-2 text-xl">Organization tree</CardTitle>
            <CardDescription className="mt-1">
              Maintain organizations, departments, status, sort order, moves and
              protected deletes.
            </CardDescription>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => exportMutation.mutate()} variant="outline">
              <Download className="h-4 w-4" />
              Export
            </Button>
            <Button onClick={() => openCreateOrg(selectedOrg?.id)}>
              <Plus className="h-4 w-4" />
              Organization
            </Button>
          </div>
        </CardHeader>
      </Card>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <Card>
          <CardContent className="p-3">
            {isLoading ? (
              <div className="p-4 text-sm text-slate-500">Loading tree</div>
            ) : (
              <OrgTree
                nodes={data}
                onSelect={selectOrg}
                selectedId={selectedOrg?.id}
              />
            )}
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Organization detail</CardTitle>
            </CardHeader>
            <CardContent>
              {selectedOrg ? (
                <div className="space-y-3 text-sm">
                  <div>
                    <p className="font-semibold text-slate-950">
                      {selectedOrg.name}
                    </p>
                    <p className="text-slate-500">{selectedOrg.code}</p>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-slate-600">
                    <span>Type: {selectedOrg.type}</span>
                    <span>Status: {selectedOrg.status}</span>
                    <span>Sort: {selectedOrg.sortOrder}</span>
                    <span>Level: {selectedOrg.level ?? '-'}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button onClick={() => openEditOrg(selectedOrg)} size="sm">
                      Edit
                    </Button>
                    <Button
                      onClick={() =>
                        orgStatusMutation.mutate({
                          id: selectedOrg.id,
                          enabled: selectedOrg.status !== 'ACTIVE',
                        })
                      }
                      size="sm"
                      variant="outline"
                    >
                      <Power className="h-4 w-4" />
                      {selectedOrg.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                    </Button>
                    <Button
                      onClick={() =>
                        orgMoveMutation.mutate({
                          id: selectedOrg.id,
                          parentId: orgForm.parentId,
                          sortOrder: orgForm.sortOrder,
                        })
                      }
                      size="sm"
                      variant="outline"
                    >
                      <GitBranch className="h-4 w-4" />
                      Move
                    </Button>
                    <Button
                      onClick={() => orgDeleteMutation.mutate(selectedOrg.id)}
                      size="sm"
                      variant="outline"
                    >
                      <Trash2 className="h-4 w-4" />
                      Delete
                    </Button>
                  </div>
                  <div className="grid gap-2">
                    <select
                      className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                      onChange={(event) =>
                        setOrgForm((current) => ({
                          ...current,
                          parentId: event.target.value || null,
                        }))
                      }
                      value={orgForm.parentId ?? ''}
                    >
                      <option value="">No parent</option>
                      {flatOrganizations
                        .filter((org) => org.id !== selectedOrg.id)
                        .map((org) => (
                          <option key={org.id} value={org.id}>
                            {org.name}
                          </option>
                        ))}
                    </select>
                    <Input
                      onChange={(event) =>
                        setOrgForm((current) => ({
                          ...current,
                          sortOrder: Number(event.target.value),
                        }))
                      }
                      type="number"
                      value={orgForm.sortOrder}
                    />
                  </div>
                </div>
              ) : (
                <p className="text-sm text-slate-500">
                  Select an organization.
                </p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Departments</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {selectedOrg ? (
                <>
                  <form
                    className="grid gap-2"
                    onSubmit={handleDepartmentSubmit}
                  >
                    <Input
                      onChange={(event) =>
                        setDepartmentForm((current) => ({
                          ...current,
                          code: event.target.value,
                        }))
                      }
                      placeholder="Department code"
                      required
                      value={departmentForm.code}
                    />
                    <Input
                      onChange={(event) =>
                        setDepartmentForm((current) => ({
                          ...current,
                          name: event.target.value,
                        }))
                      }
                      placeholder="Department name"
                      required
                      value={departmentForm.name}
                    />
                    <div className="grid grid-cols-2 gap-2">
                      <Input
                        onChange={(event) =>
                          setDepartmentForm((current) => ({
                            ...current,
                            sortOrder: Number(event.target.value),
                          }))
                        }
                        type="number"
                        value={departmentForm.sortOrder}
                      />
                      <Button
                        disabled={saveDepartmentMutation.isPending}
                        type="submit"
                      >
                        <Save className="h-4 w-4" />
                        {selectedDepartment ? 'Update' : 'Create'}
                      </Button>
                    </div>
                  </form>
                  <div className="divide-y divide-slate-100 rounded-lg border border-slate-200">
                    {(departmentsQuery.data ?? []).map((department) => (
                      <div className="space-y-2 p-3" key={department.id}>
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="font-medium text-slate-950">
                              {department.name}
                            </p>
                            <p className="text-xs text-slate-500">
                              {department.code} | {department.status}
                            </p>
                          </div>
                          <Button
                            onClick={() => {
                              setSelectedDepartment(department)
                              setDepartmentForm(
                                toDepartmentForm(selectedOrg.id, department),
                              )
                            }}
                            size="sm"
                            variant="outline"
                          >
                            Edit
                          </Button>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            onClick={() =>
                              departmentStatusMutation.mutate({
                                id: department.id,
                                enabled: department.status !== 'ACTIVE',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            {department.status === 'ACTIVE'
                              ? 'Disable'
                              : 'Enable'}
                          </Button>
                          <Button
                            onClick={() =>
                              departmentMoveMutation.mutate({
                                id: department.id,
                                parentId: departmentForm.parentId,
                                sortOrder: departmentForm.sortOrder,
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            Move
                          </Button>
                          <Button
                            onClick={() =>
                              departmentDeleteMutation.mutate(department.id)
                            }
                            size="sm"
                            variant="outline"
                          >
                            Delete
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <p className="text-sm text-slate-500">
                  Select an organization.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {exportPreview ? (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Export preview</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="max-h-72 overflow-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-50">
              {exportPreview}
            </pre>
          </CardContent>
        </Card>
      ) : null}

      {orgDialogOpen ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4"
          role="dialog"
        >
          <form
            className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl"
            onSubmit={handleOrgSubmit}
          >
            <h2 className="text-lg font-semibold text-slate-950">
              {editingOrg ? 'Edit organization' : 'Create organization'}
            </h2>
            <div className="mt-5 grid gap-4">
              <Input
                onChange={(event) =>
                  setOrgForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                placeholder="Organization name"
                required
                value={orgForm.name}
              />
              <Input
                onChange={(event) =>
                  setOrgForm((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
                placeholder="Organization code"
                required
                value={orgForm.code}
              />
              <select
                className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                onChange={(event) =>
                  setOrgForm((current) => ({
                    ...current,
                    type: event.target.value as OrgStructure['type'],
                  }))
                }
                value={orgForm.type}
              >
                <option value="COMPANY">Company</option>
                <option value="DEPARTMENT">Department</option>
                <option value="TEAM">Team</option>
              </select>
              <select
                className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                onChange={(event) =>
                  setOrgForm((current) => ({
                    ...current,
                    status: event.target.value as OrgStatus,
                  }))
                }
                value={orgForm.status}
              >
                <option value="ACTIVE">Active</option>
                <option value="DISABLED">Disabled</option>
              </select>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button onClick={() => setOrgDialogOpen(false)} variant="outline">
                Cancel
              </Button>
              <Button disabled={saveOrgMutation.isPending} type="submit">
                Save
              </Button>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  )
}
