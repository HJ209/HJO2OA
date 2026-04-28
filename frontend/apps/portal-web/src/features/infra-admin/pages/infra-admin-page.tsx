import type { ReactElement } from 'react'
import { Navigate, NavLink, Outlet, useLocation } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { INFRA_COPY } from '@/features/infra-admin/infra-copy'
import { cn } from '@/utils/cn'

const navItems = [
  ['dictionary', INFRA_COPY.nav.dictionary],
  ['config', INFRA_COPY.nav.config],
  ['error-codes', INFRA_COPY.nav.errorCodes],
  ['cache', INFRA_COPY.nav.cache],
  ['audit', INFRA_COPY.nav.audit],
  ['tenant', INFRA_COPY.nav.tenant],
  ['security', INFRA_COPY.nav.security],
  ['scheduler', INFRA_COPY.nav.scheduler],
  ['i18n', INFRA_COPY.nav.i18n],
  ['data-i18n', INFRA_COPY.nav.dataI18n],
  ['timezone', INFRA_COPY.nav.timezone],
  ['attachment', INFRA_COPY.nav.attachment],
  ['event-bus', INFRA_COPY.nav.eventBus],
] as const

export default function InfraAdminPage(): ReactElement {
  const location = useLocation()

  if (location.pathname === '/admin') {
    return <Navigate replace to="/admin/dictionary" />
  }

  return (
    <div className="space-y-5">
      <Card className="rounded-2xl">
        <CardHeader>
          <Badge className="w-fit">{INFRA_COPY.title}</Badge>
          <CardTitle className="mt-2 text-2xl">{INFRA_COPY.title}</CardTitle>
          <CardDescription className="text-base">
            {INFRA_COPY.description}
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-5 lg:grid-cols-[220px_minmax(0,1fr)]">
        <nav className="rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
          {navItems.map(([path, label]) => (
            <NavLink
              className={({ isActive }) =>
                cn(
                  'block rounded-xl px-3 py-2 text-sm font-medium transition',
                  isActive
                    ? 'bg-sky-600 text-white'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                )
              }
              key={path}
              to={`/admin/${path}`}
            >
              {label}
            </NavLink>
          ))}
        </nav>
        <Outlet />
      </div>
    </div>
  )
}
