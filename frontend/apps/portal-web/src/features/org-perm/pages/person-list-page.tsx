import { useState, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  KeyRound,
  Lock,
  Plus,
  Search,
  ShieldCheck,
  UserRoundX,
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
import { PersonFormDialog } from '@/features/org-perm/components/person-form-dialog'
import { usePersonList } from '@/features/org-perm/hooks/use-person-list'
import {
  activateAccount,
  activatePerson,
  createAccount,
  createPersonAccount,
  disableAccount,
  disablePerson,
  getPersonAccountDetail,
  lockAccount,
  resignPerson,
  resetAccountCredential,
  setPrimaryAccount,
  unlockAccount,
  updatePersonAccount,
} from '@/features/org-perm/services/person-account-service'
import { listAssignmentsByPerson } from '@/features/org-perm/services/position-assignment-service'
import type {
  AccountPayload,
  PersonAccount,
  PersonAccountPayload,
} from '@/features/org-perm/types/org-perm'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const DEFAULT_ACCOUNT_FORM: AccountPayload = {
  username: '',
  credential: '',
  accountType: 'PASSWORD',
  primaryAccount: false,
  mustChangePassword: true,
}

export default function PersonListPage(): ReactElement {
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [selectedPerson, setSelectedPerson] = useState<PersonAccount>()
  const [editingPerson, setEditingPerson] = useState<PersonAccount>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [accountForm, setAccountForm] =
    useState<AccountPayload>(DEFAULT_ACCOUNT_FORM)
  const [resetCredential, setResetCredential] = useState('ChangeMe@123')
  const query = { page: 1, size: 20, keyword }
  const { data, isLoading } = usePersonList(query)

  const detailQuery = useQuery({
    enabled: Boolean(selectedPerson?.id),
    queryKey: ['org-perm', 'person-detail', selectedPerson?.id],
    queryFn: () => getPersonAccountDetail(selectedPerson?.id ?? ''),
  })

  const assignmentQuery = useQuery({
    enabled: Boolean(selectedPerson?.id),
    queryKey: ['org-perm', 'person-assignments', selectedPerson?.id],
    queryFn: () => listAssignmentsByPerson(selectedPerson?.id ?? ''),
  })

  const invalidatePersons = (): void => {
    void queryClient.invalidateQueries({ queryKey: ['org-perm', 'persons'] })
    if (selectedPerson?.id) {
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'person-detail', selectedPerson.id],
      })
      void queryClient.invalidateQueries({
        queryKey: ['org-perm', 'person-assignments', selectedPerson.id],
      })
    }
  }

  const saveMutation = useMutation({
    mutationFn: (payload: PersonAccountPayload) =>
      editingPerson
        ? updatePersonAccount(editingPerson.id, payload)
        : createPersonAccount(payload),
    onSuccess: (person) => {
      setDialogOpen(false)
      setSelectedPerson(person)
      invalidatePersons()
    },
  })

  const personStatusMutation = useMutation({
    mutationFn: ({
      id,
      action,
    }: {
      id: string
      action: 'activate' | 'disable' | 'resign'
    }) => {
      if (action === 'activate') {
        return activatePerson(id)
      }
      if (action === 'disable') {
        return disablePerson(id)
      }
      return resignPerson(id)
    },
    onSuccess: invalidatePersons,
  })

  const accountCreateMutation = useMutation({
    mutationFn: (payload: AccountPayload) =>
      createAccount(selectedPerson?.id ?? '', payload),
    onSuccess: () => {
      setAccountForm(DEFAULT_ACCOUNT_FORM)
      invalidatePersons()
    },
  })

  const accountActionMutation = useMutation({
    mutationFn: async (payload: {
      accountId: string
      action: 'activate' | 'disable' | 'lock' | 'unlock' | 'reset' | 'primary'
    }): Promise<void> => {
      if (payload.action === 'activate') {
        await activateAccount(payload.accountId)
        return
      }
      if (payload.action === 'disable') {
        await disableAccount(payload.accountId)
        return
      }
      if (payload.action === 'lock') {
        await lockAccount(payload.accountId)
        return
      }
      if (payload.action === 'unlock') {
        await unlockAccount(payload.accountId)
        return
      }
      if (payload.action === 'reset') {
        await resetAccountCredential(payload.accountId, resetCredential)
        return
      }
      await setPrimaryAccount(selectedPerson?.id ?? '', payload.accountId)
    },
    onSuccess: invalidatePersons,
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
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex-row items-start justify-between gap-4">
          <div>
            <Badge>Person master data</Badge>
            <CardTitle className="mt-2 text-xl">Persons and accounts</CardTitle>
            <CardDescription className="mt-1">
              Maintain person profile, status, login accounts, locks and
              password resets.
            </CardDescription>
          </div>
          <Button onClick={openCreateDialog}>
            <Plus className="h-4 w-4" />
            Person
          </Button>
        </CardHeader>
      </Card>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_440px]">
        <Card>
          <CardHeader>
            <div className="relative max-w-sm">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <Input
                className="pl-9"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Search employee, account, mobile"
                value={keyword}
              />
            </div>
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-slate-200 text-slate-500">
                <tr>
                  <th className="py-3 font-medium">Account</th>
                  <th className="py-3 font-medium">Name</th>
                  <th className="py-3 font-medium">Organization</th>
                  <th className="py-3 font-medium">Status</th>
                  <th className="py-3 font-medium">Updated</th>
                  <th className="py-3 font-medium">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data?.items.map((person) => (
                  <tr
                    className="cursor-pointer hover:bg-slate-50"
                    key={person.id}
                    onClick={() => setSelectedPerson(person)}
                  >
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
                        {person.personStatus ?? person.status}
                      </Badge>
                    </td>
                    <td className="py-3">
                      {formatUtcToUserTimezone(person.updatedAtUtc)}
                    </td>
                    <td className="py-3">
                      <Button
                        onClick={(event) => {
                          event.stopPropagation()
                          openEditDialog(person)
                        }}
                        size="sm"
                        variant="outline"
                      >
                        Edit
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!isLoading && !data?.items.length ? (
              <div className="py-8 text-center text-sm text-slate-500">
                No person data
              </div>
            ) : null}
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Person detail</CardTitle>
            </CardHeader>
            <CardContent>
              {detailQuery.data ? (
                <div className="space-y-3 text-sm">
                  <div>
                    <p className="font-semibold text-slate-950">
                      {detailQuery.data.person.name}
                    </p>
                    <p className="text-slate-500">
                      {detailQuery.data.person.employeeNo}
                    </p>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-slate-600">
                    <span>Status: {detailQuery.data.person.status}</span>
                    <span>Org: {detailQuery.data.person.organizationId}</span>
                    <span>Mobile: {detailQuery.data.person.mobile ?? '-'}</span>
                    <span>Email: {detailQuery.data.person.email ?? '-'}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button
                      onClick={() =>
                        personStatusMutation.mutate({
                          id: detailQuery.data.person.id,
                          action: 'activate',
                        })
                      }
                      size="sm"
                      variant="outline"
                    >
                      <ShieldCheck className="h-4 w-4" />
                      Activate
                    </Button>
                    <Button
                      onClick={() =>
                        personStatusMutation.mutate({
                          id: detailQuery.data.person.id,
                          action: 'disable',
                        })
                      }
                      size="sm"
                      variant="outline"
                    >
                      <UserRoundX className="h-4 w-4" />
                      Disable
                    </Button>
                    <Button
                      onClick={() =>
                        personStatusMutation.mutate({
                          id: detailQuery.data.person.id,
                          action: 'resign',
                        })
                      }
                      size="sm"
                      variant="outline"
                    >
                      Resign
                    </Button>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-slate-500">Select a person.</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Accounts</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {selectedPerson ? (
                <>
                  <div className="grid gap-2">
                    <Input
                      onChange={(event) =>
                        setAccountForm((current) => ({
                          ...current,
                          username: event.target.value,
                        }))
                      }
                      placeholder="Username"
                      value={accountForm.username}
                    />
                    <Input
                      onChange={(event) =>
                        setAccountForm((current) => ({
                          ...current,
                          credential: event.target.value,
                        }))
                      }
                      placeholder="Initial credential"
                      type="password"
                      value={accountForm.credential}
                    />
                    <Button
                      disabled={accountCreateMutation.isPending}
                      onClick={() => accountCreateMutation.mutate(accountForm)}
                    >
                      <KeyRound className="h-4 w-4" />
                      Create account
                    </Button>
                  </div>
                  <Input
                    onChange={(event) => setResetCredential(event.target.value)}
                    placeholder="Reset credential"
                    type="password"
                    value={resetCredential}
                  />
                  <div className="divide-y divide-slate-100 rounded-lg border border-slate-200">
                    {(detailQuery.data?.accounts ?? []).map((account) => (
                      <div className="space-y-2 p-3" key={account.id}>
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="font-medium text-slate-950">
                              {account.username}
                            </p>
                            <p className="text-xs text-slate-500">
                              {account.accountType} | {account.status}
                              {account.locked ? ' | locked' : ''}
                              {account.primaryAccount ? ' | primary' : ''}
                            </p>
                          </div>
                          <Badge
                            variant={
                              account.status === 'ACTIVE'
                                ? 'success'
                                : 'secondary'
                            }
                          >
                            {account.status}
                          </Badge>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            onClick={() =>
                              accountActionMutation.mutate({
                                accountId: account.id,
                                action: account.locked ? 'unlock' : 'lock',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            <Lock className="h-4 w-4" />
                            {account.locked ? 'Unlock' : 'Lock'}
                          </Button>
                          <Button
                            onClick={() =>
                              accountActionMutation.mutate({
                                accountId: account.id,
                                action:
                                  account.status === 'ACTIVE'
                                    ? 'disable'
                                    : 'activate',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            {account.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                          </Button>
                          <Button
                            onClick={() =>
                              accountActionMutation.mutate({
                                accountId: account.id,
                                action: 'reset',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            Reset
                          </Button>
                          <Button
                            onClick={() =>
                              accountActionMutation.mutate({
                                accountId: account.id,
                                action: 'primary',
                              })
                            }
                            size="sm"
                            variant="outline"
                          >
                            Primary
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <p className="text-sm text-slate-500">Select a person.</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Assignments</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2 text-sm">
                {(assignmentQuery.data ?? []).map((assignment) => (
                  <div
                    className="rounded-lg border border-slate-200 p-3"
                    key={assignment.id}
                  >
                    <p className="font-medium text-slate-950">
                      {assignment.positionId}
                    </p>
                    <p className="text-slate-500">
                      {assignment.type} | {assignment.status} |{' '}
                      {assignment.startDate ?? '-'} -{' '}
                      {assignment.endDate ?? '-'}
                    </p>
                  </div>
                ))}
                {selectedPerson && !assignmentQuery.data?.length ? (
                  <p className="text-sm text-slate-500">No assignments.</p>
                ) : null}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

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
