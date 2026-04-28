import { useCallback, useEffect, useState } from 'react'
import {
  login as requestLogin,
  logout as requestLogout,
  refreshToken,
} from '@/services/auth-service'
import { isBizError } from '@/services/error-mapper'
import { useAuthStore } from '@/stores/auth-store'

const COPY = {
  unknownErrorText: '登录失败，请稍后重试',
  missingRefreshTokenText: '登录状态已失效，请重新登录',
} as const

export interface UseLoginResult {
  isSubmitting: boolean
  errorMessage: string | null
  submitLogin: (username: string, password: string) => Promise<boolean>
  submitLogout: () => Promise<void>
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

export function useLogin(): UseLoginResult {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const authStore = useAuthStore()

  const submitLogin = useCallback(
    async (username: string, password: string): Promise<boolean> => {
      setIsSubmitting(true)
      setErrorMessage(null)

      try {
        const session = await requestLogin(username, password)
        authStore.login(session)

        return true
      } catch (error) {
        setErrorMessage(resolveErrorMessage(error))

        return false
      } finally {
        setIsSubmitting(false)
      }
    },
    [authStore],
  )

  const submitLogout = useCallback(async (): Promise<void> => {
    try {
      await requestLogout()
    } finally {
      authStore.logout()
    }
  }, [authStore])

  useEffect(() => {
    function handleRefreshRequired(): void {
      const { refreshTokenValue, setRefreshing, refreshSession, logout } =
        useAuthStore.getState()

      if (!refreshTokenValue) {
        setErrorMessage(COPY.missingRefreshTokenText)
        logout()

        return
      }

      setRefreshing(true)
      void refreshToken(refreshTokenValue)
        .then((session) => refreshSession(session))
        .catch((error: unknown) => {
          setErrorMessage(resolveErrorMessage(error))
          logout()
        })
        .finally(() => setRefreshing(false))
    }

    window.addEventListener(
      'auth.session.refresh-required',
      handleRefreshRequired,
    )

    return () => {
      window.removeEventListener(
        'auth.session.refresh-required',
        handleRefreshRequired,
      )
    }
  }, [])

  return {
    isSubmitting,
    errorMessage,
    submitLogin,
    submitLogout,
  }
}
