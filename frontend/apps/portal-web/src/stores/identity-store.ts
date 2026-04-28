import { create } from 'zustand'
import type { IdentityAssignment, IdentitySnapshot } from '@/types/domain'

export const IDENTITY_INVALIDATED_EVENT = 'org.identity-context.invalidated'
export const IDENTITY_REFRESH_REQUESTED_EVENT =
  'org.identity-context.refresh-requested'

export interface IdentityContextState extends IdentitySnapshot {
  assignments: IdentityAssignment[]
  pendingTodoCount: number
  unreadMessageCount: number
  menuVersion: number
  todoVersion: number
  messageVersion: number
  isLoading: boolean
  isSwitching: boolean
  isInvalidated: boolean
  refresh: (
    nextContext?: Partial<IdentityContextPayload>,
  ) => Promise<IdentitySnapshot>
  applyContext: (nextContext: IdentityContextPayload) => void
  setLoading: (isLoading: boolean) => void
  setSwitching: (isSwitching: boolean) => void
  markInvalidated: () => void
  clearInvalidated: () => void
  clear: () => void
}

export interface IdentityContextPayload extends IdentitySnapshot {
  assignments: IdentityAssignment[]
  pendingTodoCount: number
  unreadMessageCount: number
}

const defaultAssignment: IdentityAssignment = {
  assignmentId: 'assign-demo-001',
  positionId: 'position-demo-001',
  orgId: 'org-demo-001',
  positionName: '门户平台主管',
  orgName: '数字办公部',
}

const defaultSnapshot: IdentityContextPayload = {
  currentAssignment: defaultAssignment,
  orgId: defaultAssignment.orgId,
  roleIds: ['ROLE_PORTAL_ADMIN', 'ROLE_MESSAGE_OPERATOR'],
  assignments: [defaultAssignment],
  pendingTodoCount: 0,
  unreadMessageCount: 0,
}

function buildSnapshot(
  nextContext?: Partial<IdentityContextPayload>,
): IdentityContextPayload {
  const currentAssignment =
    nextContext?.currentAssignment ?? defaultSnapshot.currentAssignment

  return {
    currentAssignment,
    orgId: nextContext?.orgId ?? currentAssignment?.orgId ?? null,
    roleIds: nextContext?.roleIds ?? defaultSnapshot.roleIds,
    assignments: nextContext?.assignments ?? defaultSnapshot.assignments,
    pendingTodoCount:
      nextContext?.pendingTodoCount ?? defaultSnapshot.pendingTodoCount,
    unreadMessageCount:
      nextContext?.unreadMessageCount ?? defaultSnapshot.unreadMessageCount,
  }
}

function applyRefreshCounters(
  snapshot: IdentityContextPayload,
): Partial<IdentityContextState> {
  return {
    ...snapshot,
    menuVersion: Date.now(),
    todoVersion: Date.now(),
    messageVersion: Date.now(),
    isInvalidated: false,
  }
}

export const useIdentityStore = create<IdentityContextState>((set) => ({
  ...defaultSnapshot,
  menuVersion: 0,
  todoVersion: 0,
  messageVersion: 0,
  isLoading: false,
  isSwitching: false,
  isInvalidated: false,
  refresh: async (nextContext) => {
    const snapshot = buildSnapshot(nextContext)
    set(applyRefreshCounters(snapshot))

    return {
      currentAssignment: snapshot.currentAssignment,
      orgId: snapshot.orgId,
      roleIds: snapshot.roleIds,
    }
  },
  applyContext: (nextContext) => {
    set(applyRefreshCounters(nextContext))
  },
  setLoading: (isLoading) => {
    set({ isLoading })
  },
  setSwitching: (isSwitching) => {
    set({ isSwitching })
  },
  markInvalidated: () => {
    set({ isInvalidated: true })
  },
  clearInvalidated: () => {
    set({ isInvalidated: false })
  },
  clear: () => {
    set({
      currentAssignment: null,
      orgId: null,
      roleIds: [],
      assignments: [],
      pendingTodoCount: 0,
      unreadMessageCount: 0,
      isLoading: false,
      isSwitching: false,
      isInvalidated: false,
    })
  },
}))

export function registerIdentityInvalidationListener(): () => void {
  if (typeof window === 'undefined') {
    return () => undefined
  }

  const handleInvalidated = (): void => {
    useIdentityStore.getState().markInvalidated()
    window.dispatchEvent(new CustomEvent(IDENTITY_REFRESH_REQUESTED_EVENT))
  }

  window.addEventListener(IDENTITY_INVALIDATED_EVENT, handleInvalidated)

  return () => {
    window.removeEventListener(IDENTITY_INVALIDATED_EVENT, handleInvalidated)
  }
}
