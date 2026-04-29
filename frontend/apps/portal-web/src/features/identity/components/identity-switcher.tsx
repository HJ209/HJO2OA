import { useEffect, useMemo, useRef, useState, type ReactElement } from 'react'
import { Check, ChevronDown, Loader2, LogOut } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useIdentityContext } from '@/features/identity/hooks/use-identity-context'
import { useAuth } from '@/hooks/use-auth'
import { cn } from '@/utils/cn'

const COPY = {
  triggerKey: 'identity.switcher.trigger',
  triggerText: '切换身份',
  emptyKey: 'identity.switcher.empty',
  emptyText: '身份未绑定',
  loadingKey: 'identity.switcher.loading',
  loadingText: '身份切换中',
  userFallbackKey: 'identity.switcher.userFallback',
  userFallbackText: '未登录用户',
  todoKey: 'identity.switcher.todo',
  todoText: '待办',
  messageKey: 'identity.switcher.message',
  messageText: '消息',
  roleKey: 'identity.switcher.role',
  roleText: '当前岗位',
  assignmentKey: 'identity.switcher.assignment',
  assignmentText: '身份切换',
  logoutKey: 'identity.switcher.logout',
  logoutText: '退出登录',
} as const

export interface IdentitySwitcherProps {
  onLogout?: () => void
}

function resolveAvatarText(seed: string): string {
  const normalizedSeed = seed.trim()

  if (!normalizedSeed) {
    return 'U'
  }

  return normalizedSeed.slice(0, 1).toUpperCase()
}

export function IdentitySwitcher({
  onLogout,
}: IdentitySwitcherProps = {}): ReactElement {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const { user } = useAuth()
  const { context, isSwitching, errorMessage, switchContext } =
    useIdentityContext()
  const currentAssignment = context.currentAssignment
  const triggerLabel = user?.displayName ?? COPY.userFallbackText
  const avatarText = useMemo(
    () =>
      resolveAvatarText(
        user?.displayName ??
          user?.accountName ??
          currentAssignment?.positionName ??
          COPY.userFallbackText,
      ),
    [currentAssignment?.positionName, user?.accountName, user?.displayName],
  )

  useEffect(() => {
    if (!isOpen) {
      return
    }

    function handlePointerDown(event: PointerEvent): void {
      if (containerRef.current?.contains(event.target as Node)) {
        return
      }

      setIsOpen(false)
    }

    function handleKeyDown(event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    window.addEventListener('pointerdown', handlePointerDown)
    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('pointerdown', handlePointerDown)
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen])

  async function handleSwitch(assignmentId: string): Promise<void> {
    const isSuccess = await switchContext(assignmentId)

    if (isSuccess) {
      setIsOpen(false)
    }
  }

  function handleLogout(): void {
    setIsOpen(false)
    onLogout?.()
  }

  return (
    <div className="relative" ref={containerRef}>
      <Button
        aria-label={triggerLabel}
        aria-expanded={isOpen}
        aria-haspopup="menu"
        className="h-10 gap-2 rounded-full px-2 text-slate-600 hover:bg-slate-100 hover:text-slate-950"
        onClick={() => setIsOpen((previousState) => !previousState)}
        variant="ghost"
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-sky-500 to-cyan-500 text-xs font-semibold text-white shadow-sm">
          {avatarText}
        </span>
        <span className="hidden max-w-28 truncate text-sm font-medium lg:block">
          {triggerLabel}
        </span>
        <span className="hidden h-1 w-1 rounded-full bg-slate-300 lg:block" />
        <span className="hidden max-w-24 truncate text-xs text-slate-400 xl:block">
          {currentAssignment?.orgName ?? COPY.emptyText}
        </span>
        <ChevronDown
          className={cn(
            'h-4 w-4 shrink-0 text-slate-400 transition',
            isOpen ? 'rotate-180' : undefined,
          )}
        />
      </Button>

      {isOpen ? (
        <div
          className="absolute right-0 z-30 mt-2 w-[320px] rounded-2xl border border-slate-200 bg-white p-3 shadow-xl"
          role="menu"
        >
          <div className="rounded-2xl bg-slate-50 px-4 py-3">
            <div className="flex items-center gap-3">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-sky-500 to-cyan-500 text-sm font-semibold text-white shadow-sm">
                {avatarText}
              </span>
              <div className="min-w-0">
                <div className="truncate text-sm font-semibold text-slate-900">
                  {triggerLabel}
                </div>
                <div className="truncate text-xs text-slate-500">
                  {user?.accountName ?? COPY.userFallbackText}
                </div>
              </div>
            </div>

            <div className="mt-3 flex flex-wrap gap-2">
              <Badge variant="secondary">
                {currentAssignment?.positionName ?? COPY.roleText}
              </Badge>
              <Badge variant="secondary">
                {currentAssignment?.orgName ?? COPY.emptyText}
              </Badge>
            </div>

            <div className="mt-3 flex items-center gap-3 text-xs text-slate-500">
              <span>
                {COPY.todoText} {context.pendingTodoCount}
              </span>
              <span>
                {COPY.messageText} {context.unreadMessageCount}
              </span>
            </div>
          </div>

          {context.assignments.length > 0 ? (
            <div className="mt-3">
              <div className="px-1 pb-2 text-xs font-medium text-slate-500">
                {COPY.assignmentText}
              </div>
              <div className="max-h-64 space-y-1 overflow-y-auto">
                {context.assignments.map((assignment) => {
                  const isActive =
                    currentAssignment?.assignmentId === assignment.assignmentId

                  return (
                    <button
                      className={cn(
                        'flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm transition hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70',
                        isActive
                          ? 'bg-sky-50 text-sky-700 ring-1 ring-sky-100'
                          : 'text-slate-700',
                      )}
                      disabled={isSwitching}
                      key={assignment.assignmentId}
                      onClick={() => void handleSwitch(assignment.assignmentId)}
                      role="menuitem"
                      type="button"
                    >
                      <span className="min-w-0">
                        <span className="block truncate font-medium">
                          {assignment.positionName}
                        </span>
                        <span className="block truncate text-xs text-slate-500">
                          {assignment.orgName}
                        </span>
                      </span>
                      {isActive ? <Check className="h-4 w-4" /> : null}
                    </button>
                  )
                })}
              </div>
            </div>
          ) : (
            <div className="px-3 py-4 text-sm text-slate-500">
              {COPY.emptyText}
            </div>
          )}

          {isSwitching ? (
            <div className="mt-2 flex items-center gap-2 px-3 py-2 text-xs text-slate-500">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              {COPY.loadingText}
            </div>
          ) : null}

          {errorMessage ? (
            <div className="mt-2 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              {errorMessage}
            </div>
          ) : null}

          {onLogout ? (
            <div className="mt-3 border-t border-slate-100 pt-2">
              <button
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-50"
                onClick={handleLogout}
                role="menuitem"
                type="button"
              >
                <LogOut className="h-4 w-4" />
                {COPY.logoutText}
              </button>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
