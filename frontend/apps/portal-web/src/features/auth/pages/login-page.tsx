import { useEffect, useState, type FormEvent, type ReactElement } from 'react'
import { LockKeyhole, LogIn } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useLogin } from '@/features/auth/hooks/use-login'
import { useIdentityContext } from '@/features/identity/hooks/use-identity-context'
import { useAuth } from '@/hooks/use-auth'

const COPY = {
  titleKey: 'auth.login.title',
  titleText: '进入门户演示环境',
  descriptionKey: 'auth.login.description',
  descriptionText: '使用账号密码登录 HJO2OA 门户工作台。',
  usernameKey: 'auth.login.username',
  usernameText: '用户名',
  usernamePlaceholderKey: 'auth.login.username.placeholder',
  usernamePlaceholderText: '请输入用户名',
  passwordKey: 'auth.login.password',
  passwordText: '密码',
  passwordPlaceholderKey: 'auth.login.password.placeholder',
  passwordPlaceholderText: '请输入密码',
  actionKey: 'auth.login.submit',
  actionText: '进入工作台',
  submittingKey: 'auth.login.submitting',
  submittingText: '登录中...',
} as const

interface LoginNavigationState {
  from?: string
}

export default function LoginPage(): ReactElement {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated } = useAuth()
  const { loadContext } = useIdentityContext()
  const { isSubmitting, errorMessage, submitLogin } = useLogin()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const redirectTo =
    (location.state as LoginNavigationState | null)?.from ?? '/'

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true })
    }
  }, [isAuthenticated, navigate, redirectTo])

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault()

    const isSuccess = await submitLogin(username.trim(), password)

    if (!isSuccess) {
      return
    }

    await loadContext()
    navigate(redirectTo, { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <Card className="w-full max-w-md border-sky-100">
        <CardHeader className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-sky-50 text-sky-600">
            <LockKeyhole className="h-6 w-6" />
          </div>
          <CardTitle className="mt-4 text-2xl">{COPY.titleText}</CardTitle>
          <CardDescription className="mt-2 text-base">
            {COPY.descriptionText}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={handleSubmit}>
            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-700">
                {COPY.usernameText}
              </span>
              <Input
                autoComplete="username"
                disabled={isSubmitting}
                onChange={(event) => setUsername(event.target.value)}
                placeholder={COPY.usernamePlaceholderText}
                required
                value={username}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-700">
                {COPY.passwordText}
              </span>
              <Input
                autoComplete="current-password"
                disabled={isSubmitting}
                onChange={(event) => setPassword(event.target.value)}
                placeholder={COPY.passwordPlaceholderText}
                required
                type="password"
                value={password}
              />
            </label>

            {errorMessage ? (
              <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {errorMessage}
              </div>
            ) : null}

            <Button className="w-full" disabled={isSubmitting} type="submit">
              <LogIn className="h-4 w-4" />
              {isSubmitting ? COPY.submittingText : COPY.actionText}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
