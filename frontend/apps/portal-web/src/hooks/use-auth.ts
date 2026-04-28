import { useAuthStore, type AuthSession } from '@/stores/auth-store'
import type { AuthenticatedUser } from '@/types/domain'

export interface UseAuthResult {
  token: string | null
  user: AuthenticatedUser | null
  isAuthenticated: boolean
  login: (session: AuthSession) => void
  logout: () => void
}

export function useAuth(): UseAuthResult {
  const { token, user, login, logout } = useAuthStore()

  return {
    token,
    user,
    isAuthenticated: Boolean(token),
    login,
    logout,
  }
}
