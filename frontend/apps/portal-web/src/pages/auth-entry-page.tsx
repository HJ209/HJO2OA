import { useEffect, type ReactElement } from 'react'
import { DoorOpen, ShieldCheck } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useAuth } from '@/hooks/use-auth'
import { useIdentity } from '@/hooks/use-identity'
import { createDemoAuthSession } from '@/stores/auth-store'

const COPY = {
  titleKey: 'auth.entry.title',
  titleText: '进入门户演示环境',
  descriptionKey: 'auth.entry.description',
  descriptionText: '当前为前端骨架阶段，先通过演示登录态进入受保护路由。',
  actionKey: 'auth.entry.action',
  actionText: '进入工作台',
} as const

interface AuthNavigationState {
  from?: string
}

export default function AuthEntryPage(): ReactElement {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, login } = useAuth()
  const { refresh } = useIdentity()
  const redirectTo = (location.state as AuthNavigationState | null)?.from ?? '/'

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true })
    }
  }, [isAuthenticated, navigate, redirectTo])

  async function handleEnterWorkspace(): Promise<void> {
    login(createDemoAuthSession())
    await refresh()
    navigate(redirectTo, { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <Card className="w-full max-w-lg border-sky-100">
        <CardHeader className="text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-sky-50 text-sky-600">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <CardTitle className="mt-4 text-2xl">{COPY.titleText}</CardTitle>
          <CardDescription className="mt-2 text-base">
            {COPY.descriptionText}
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">
            受保护路由会在未登录时统一跳转到这里，后续可以替换成真实认证流程。
          </div>
          <Button className="w-full" onClick={handleEnterWorkspace}>
            <DoorOpen className="h-4 w-4" />
            {COPY.actionText}
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
