import { useCallback, useEffect, useState } from 'react'
import {
  getCurrentContext,
  switchAssignment,
  type IdentityContext,
} from '@/services/identity-service'
import { isBizError } from '@/services/error-mapper'
import {
  IDENTITY_REFRESH_REQUESTED_EVENT,
  registerIdentityInvalidationListener,
  useIdentityStore,
} from '@/stores/identity-store'

const COPY = {
  unknownErrorText: '身份上下文刷新失败，请稍后重试',
} as const

export interface UseIdentityContextResult {
  context: IdentityContext
  isLoading: boolean
  isSwitching: boolean
  errorMessage: string | null
  loadContext: () => Promise<IdentityContext | null>
  switchContext: (assignmentId: string) => Promise<boolean>
}

function resolveErrorMessage(error: unknown): string {
  if (isBizError(error)) {
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return COPY.unknownErrorText
}

export function useIdentityContext(): UseIdentityContextResult {
  const identityStore = useIdentityStore()
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const loadContext = useCallback(async (): Promise<IdentityContext | null> => {
    identityStore.setLoading(true)
    setErrorMessage(null)

    try {
      const nextContext = await getCurrentContext()
      identityStore.applyContext(nextContext)

      return nextContext
    } catch (error) {
      setErrorMessage(resolveErrorMessage(error))

      return null
    } finally {
      identityStore.setLoading(false)
    }
  }, [identityStore])

  const switchContext = useCallback(
    async (assignmentId: string): Promise<boolean> => {
      if (
        identityStore.currentAssignment?.assignmentId === assignmentId ||
        identityStore.isSwitching
      ) {
        return true
      }

      identityStore.setSwitching(true)
      setErrorMessage(null)

      try {
        const nextContext = await switchAssignment(assignmentId)
        identityStore.applyContext(nextContext)

        return true
      } catch (error) {
        setErrorMessage(resolveErrorMessage(error))

        return false
      } finally {
        identityStore.setSwitching(false)
      }
    },
    [identityStore],
  )

  useEffect(() => {
    const unregister = registerIdentityInvalidationListener()

    function handleRefreshRequested(): void {
      void loadContext()
    }

    window.addEventListener(
      IDENTITY_REFRESH_REQUESTED_EVENT,
      handleRefreshRequested,
    )

    return () => {
      unregister()
      window.removeEventListener(
        IDENTITY_REFRESH_REQUESTED_EVENT,
        handleRefreshRequested,
      )
    }
  }, [loadContext])

  return {
    context: {
      currentAssignment: identityStore.currentAssignment,
      orgId: identityStore.orgId,
      roleIds: identityStore.roleIds,
      assignments: identityStore.assignments,
      pendingTodoCount: identityStore.pendingTodoCount,
      unreadMessageCount: identityStore.unreadMessageCount,
    },
    isLoading: identityStore.isLoading,
    isSwitching: identityStore.isSwitching,
    errorMessage,
    loadContext,
    switchContext,
  }
}
