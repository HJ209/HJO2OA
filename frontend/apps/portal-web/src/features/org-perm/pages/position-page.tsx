import { useState, type FormEvent, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  GitPullRequest,
  Link,
  Plus,
  Power,
  ShieldPlus,
  Star,
  XCircle,
} from 'lucide-react'
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
import {
  addPositionRole,
  changePrimaryAssignment,
  createAssignment,
  createPosition,
  deactivateAssignment,
  listAssignmentsByPosition,
  listPositionRoles,
  listPositions,
  removePositionRole,
  setPositionEnabled,
  updatePosition,
} from '@/features/org-perm/services/position-assignment-service'
import type {
  AssignmentPayload,
  OrgPosition,
  PositionPayload,
} from '@/features/org-perm/types/org-perm'

const DEFAULT_POSITION_FORM: PositionPayload = {
  code: '',
  name: '',
  organizationId: '',
  departmentId: null,
  category: 'OTHER',
  level: null,
  sortOrder: 0,
}

const DEFAULT_ASSIGNMENT_FORM: AssignmentPayload = {
  personId: '',
  positionId: '',
  type: 'SECONDARY',
  startDate: '',
  endDate: '',
}

function toPositionForm(position?: OrgPosition): PositionPayload {
  if (!position) {
    return DEFAULT_POSITION_FORM
  }

  return {
    code: position.code,
    name: position.name,
    organizationId: position.organizationId,
    departmentId: position.departmentId ?? null,
    category: position.category,
    level: position.level ?? null,
    sortOrder: position.sortOrder,
  }
}

export default function PositionPage(): ReactElement {
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [selectedPosition, setSelectedPosition] = useState<OrgPosition>()
  const [editingPosition, setEditingPosition] = useState<OrgPosition>()
  const [positionForm, setPositionForm] = useState<PositionPayload>(
    DEFAULT_POSITION_FORM,
  )
  const [assignmentForm, setAssignmentForm] = useState<AssignmentPayload>(
    DEFAULT_ASSIGNMENT_FORM,
  )
  const [roleId, setRoleId] = useState('')

  const positionsQuery = useQuery({
    queryKey: ['org-perm', 'positions', { page: 1, size: 50, keyword }],
    queryFn: () => listPositions({ page: 1, size: 50, keyword }),
  })

  const assignmentsQuery = useQuery({
    enabled: Boolean(selectedPosition?.id),
    queryKey: ['org-perm', 'position-assignments', selectedPosition?.id],
    queryFn: () => listAssignmentsByPosition(selectedPosition?.id ?? ''),
  })

  const rolesQuery = useQuery({
    enabled: Boolean(selectedPosition?.id),
    queryKey: ['org-perm', 'position-roles', selectedPosition?.id],
    queryFn: () => listPositionRoles(selectedPosition?.id ?? ''),
  })

  const invalidatePositions = (): void => {
    void queryClient.invalidateQueries({ queryKey: ['org-perm', 'positions'] })
    if (selectedPosition?.id) {
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'position-assignments', selectedPosition.id],
      })
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'position-roles', selectedPosition.id],
      })
    }
  }

  const savePositionMutation = useMutation({
    mutationFn: (payload: PositionPayload) =>
      editingPosition
        ? updatePosition(editingPosition.id, payload)
        : createPosition(payload),
    onSuccess: (position) => {
      setEditingPosition(undefined)
      setSelectedPosition(position)
      setPositionForm(toPositionForm(position))
      invalidatePositions()
    },
  })

  const positionStatusMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      setPositionEnabled(id, enabled),
    onSuccess: invalidatePositions,
  })

  const assignmentCreateMutation = useMutation({
    mutationFn: (payload: AssignmentPayload) => createAssignment(payload),
    onSuccess: () => {
      setAssignmentForm({
        ...DEFAULT_ASSIGNMENT_FORM,
        positionId: selectedPosition?.id ?? '',
      })
      invalidatePositions()
    },
  })

  const assignmentActionMutation = useMutation({
    mutationFn: (payload: {
      assignmentId: string
      personId: string
      action: 'primary' | 'deactivate'
    }) =>
      payload.action === 'primary'
        ? changePrimaryAssignment(payload.personId, payload.assignmentId)
        : deactivateAssignment(
            payload.assignmentId,
            new Date().toISOString().slice(0, 10),
          ),
    onSuccess: invalidatePositions,
  })

  const roleMutation = useMutation({
    mutationFn: async (payload: {
      action: 'add' | 'remove'
      roleId: string
    }): Promise<void> => {
      if (payload.action === 'add') {
        await addPositionRole(selectedPosition?.id ?? '', payload.roleId)
        return
      }

      await removePositionRole(selectedPosition?.id ?? '', payload.roleId)
    },
    onSuccess: () => {
      setRoleId('')
      invalidatePositions()
    },
  })

  function selectPosition(position: OrgPosition): void {
    setSelectedPosition(position)
    setEditingPosition(position)
    setPositionForm(toPositionForm(position))
    setAssignmentForm({
      ...DEFAULT_ASSIGNMENT_FORM,
      positionId: position.id,
    })
  }

  function newPosition(): void {
    setEditingPosition(undefined)
    setSelectedPosition(undefined)
    setPositionForm(DEFAULT_POSITION_FORM)
    setAssignmentForm(DEFAULT_ASSIGNMENT_FORM)
  }

  function handlePositionSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    savePositionMutation.mutate(positionForm)
  }

  function handleAssignmentSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!selectedPosition) {
      return
    }
    assignmentCreateMutation.mutate({
      ...assignmentForm,
      positionId: selectedPosition.id,
      startDate: assignmentForm.startDate || null,
      endDate: assignmentForm.endDate || null,
    })
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex-row items-start justify-between gap-4">
          <div>
            <Badge>Position master data</Badge>
            <CardTitle className="mt-2 text-xl">
              Positions and assignments
            </CardTitle>
            <CardDescription className="mt-1">
              Maintain position definitions, role pre-bindings and
              multi-position assignments.
            </CardDescription>
          </div>
          <Button onClick={newPosition}>
            <Plus className="h-4 w-4" />
            Position
          </Button>
        </CardHeader>
      </Card>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_440px]">
        <Card>
          <CardHeader>
            <Input
              className="max-w-sm"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Search code, name, organization"
              value={keyword}
            />
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-slate-200 text-slate-500">
                <tr>
                  <th className="py-3 font-medium">Code</th>
                  <th className="py-3 font-medium">Name</th>
                  <th className="py-3 font-medium">Organization</th>
                  <th className="py-3 font-medium">Category</th>
                  <th className="py-3 font-medium">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {positionsQuery.data?.items.map((position) => (
                  <tr
                    className="cursor-pointer hover:bg-slate-50"
                    key={position.id}
                    onClick={() => selectPosition(position)}
                  >
                    <td className="py-3 font-medium text-slate-950">
                      {position.code}
                    </td>
                    <td className="py-3">{position.name}</td>
                    <td className="py-3">{position.organizationId}</td>
                    <td className="py-3">{position.category}</td>
                    <td className="py-3">
                      <Badge
                        variant={
                          position.status === 'ACTIVE' ? 'success' : 'secondary'
                        }
                      >
                        {position.status}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Position</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="grid gap-2" onSubmit={handlePositionSubmit}>
                <Input
                  onChange={(event) =>
                    setPositionForm((current) => ({
                      ...current,
                      code: event.target.value,
                    }))
                  }
                  placeholder="Code"
                  required
                  value={positionForm.code}
                />
                <Input
                  onChange={(event) =>
                    setPositionForm((current) => ({
                      ...current,
                      name: event.target.value,
                    }))
                  }
                  placeholder="Name"
                  required
                  value={positionForm.name}
                />
                <Input
                  onChange={(event) =>
                    setPositionForm((current) => ({
                      ...current,
                      organizationId: event.target.value,
                    }))
                  }
                  placeholder="Organization ID"
                  required
                  value={positionForm.organizationId}
                />
                <Input
                  onChange={(event) =>
                    setPositionForm((current) => ({
                      ...current,
                      departmentId: event.target.value || null,
                    }))
                  }
                  placeholder="Department ID"
                  value={positionForm.departmentId ?? ''}
                />
                <div className="grid grid-cols-2 gap-2">
                  <select
                    className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                    onChange={(event) =>
                      setPositionForm((current) => ({
                        ...current,
                        category: event.target
                          .value as PositionPayload['category'],
                      }))
                    }
                    value={positionForm.category}
                  >
                    <option value="MANAGEMENT">Management</option>
                    <option value="PROFESSIONAL">Professional</option>
                    <option value="TECHNICAL">Technical</option>
                    <option value="OPERATIONAL">Operational</option>
                    <option value="OTHER">Other</option>
                  </select>
                  <Input
                    onChange={(event) =>
                      setPositionForm((current) => ({
                        ...current,
                        sortOrder: Number(event.target.value),
                      }))
                    }
                    type="number"
                    value={positionForm.sortOrder}
                  />
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button
                    disabled={savePositionMutation.isPending}
                    type="submit"
                  >
                    Save
                  </Button>
                  {selectedPosition ? (
                    <Button
                      onClick={() =>
                        positionStatusMutation.mutate({
                          id: selectedPosition.id,
                          enabled: selectedPosition.status !== 'ACTIVE',
                        })
                      }
                      variant="outline"
                    >
                      <Power className="h-4 w-4" />
                      {selectedPosition.status === 'ACTIVE'
                        ? 'Disable'
                        : 'Enable'}
                    </Button>
                  ) : null}
                </div>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Assignments</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {selectedPosition ? (
                <>
                  <form
                    className="grid gap-2"
                    onSubmit={handleAssignmentSubmit}
                  >
                    <Input
                      onChange={(event) =>
                        setAssignmentForm((current) => ({
                          ...current,
                          personId: event.target.value,
                        }))
                      }
                      placeholder="Person ID"
                      required
                      value={assignmentForm.personId}
                    />
                    <select
                      className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                      onChange={(event) =>
                        setAssignmentForm((current) => ({
                          ...current,
                          type: event.target.value as AssignmentPayload['type'],
                        }))
                      }
                      value={assignmentForm.type}
                    >
                      <option value="PRIMARY">Primary</option>
                      <option value="SECONDARY">Secondary</option>
                      <option value="PART_TIME">Part time</option>
                    </select>
                    <div className="grid grid-cols-2 gap-2">
                      <Input
                        onChange={(event) =>
                          setAssignmentForm((current) => ({
                            ...current,
                            startDate: event.target.value,
                          }))
                        }
                        type="date"
                        value={assignmentForm.startDate ?? ''}
                      />
                      <Input
                        onChange={(event) =>
                          setAssignmentForm((current) => ({
                            ...current,
                            endDate: event.target.value,
                          }))
                        }
                        type="date"
                        value={assignmentForm.endDate ?? ''}
                      />
                    </div>
                    <Button
                      disabled={assignmentCreateMutation.isPending}
                      type="submit"
                    >
                      <GitPullRequest className="h-4 w-4" />
                      Assign
                    </Button>
                  </form>

                  <div className="divide-y divide-slate-100 rounded-lg border border-slate-200">
                    {(assignmentsQuery.data ?? []).map((assignment) => (
                      <div className="space-y-2 p-3" key={assignment.id}>
                        <div>
                          <p className="font-medium text-slate-950">
                            {assignment.personId}
                          </p>
                          <p className="text-xs text-slate-500">
                            {assignment.type} | {assignment.status} |{' '}
                            {assignment.startDate ?? '-'} -{' '}
                            {assignment.endDate ?? '-'}
                          </p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            onClick={() =>
                              assignmentActionMutation.mutate({
                                assignmentId: assignment.id,
                                personId: assignment.personId,
                                action: 'primary',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            <Star className="h-4 w-4" />
                            Primary
                          </Button>
                          <Button
                            onClick={() =>
                              assignmentActionMutation.mutate({
                                assignmentId: assignment.id,
                                personId: assignment.personId,
                                action: 'deactivate',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            <XCircle className="h-4 w-4" />
                            End
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <p className="text-sm text-slate-500">Select a position.</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Role pre-binding</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {selectedPosition ? (
                <>
                  <div className="flex gap-2">
                    <Input
                      onChange={(event) => setRoleId(event.target.value)}
                      placeholder="Role ID"
                      value={roleId}
                    />
                    <Button
                      onClick={() =>
                        roleMutation.mutate({ action: 'add', roleId })
                      }
                    >
                      <ShieldPlus className="h-4 w-4" />
                      Bind
                    </Button>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {(rolesQuery.data ?? []).map((role) => (
                      <Button
                        key={role.id}
                        onClick={() =>
                          roleMutation.mutate({
                            action: 'remove',
                            roleId: role.roleId,
                          })
                        }
                        size="sm"
                        variant="outline"
                      >
                        <Link className="h-4 w-4" />
                        {role.roleId}
                      </Button>
                    ))}
                  </div>
                </>
              ) : (
                <p className="text-sm text-slate-500">Select a position.</p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
