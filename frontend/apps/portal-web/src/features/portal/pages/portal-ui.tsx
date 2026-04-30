import type { ComponentProps, ReactElement, ReactNode } from 'react'
import { AlertCircle, ShieldAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { isBizError } from '@/services/error-mapper'
import { cn } from '@/utils/cn'

export function PortalPanel({
  actions,
  children,
  description,
  title,
}: {
  title: string
  description: string
  actions?: ReactNode
  children: ReactNode
}): ReactElement {
  return (
    <Card className="rounded-2xl">
      <CardHeader className="flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>{title}</CardTitle>
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        </div>
        {actions}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

export function Field({
  children,
  label,
}: {
  label: string
  children: ReactNode
}): ReactElement {
  return (
    <label className="block space-y-1.5 text-sm">
      <span className="font-medium text-slate-700">{label}</span>
      {children}
    </label>
  )
}

export function TextInput({
  onChange,
  value,
  ...props
}: Omit<ComponentProps<typeof Input>, 'onChange' | 'value'> & {
  value: string
  onChange: (value: string) => void
}): ReactElement {
  return (
    <Input
      value={value}
      onChange={(event) => onChange(event.currentTarget.value)}
      {...props}
    />
  )
}

export function SelectInput<TValue extends string>({
  options,
  value,
  onChange,
}: {
  value: TValue
  options: Array<{ value: TValue; label: string }>
  onChange: (value: TValue) => void
}): ReactElement {
  return (
    <select
      className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm shadow-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
      value={value}
      onChange={(event) => onChange(event.currentTarget.value as TValue)}
    >
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
}

export function ToggleField({
  checked,
  label,
  onChange,
}: {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}): ReactElement {
  return (
    <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">
      <input
        checked={checked}
        className="h-4 w-4"
        type="checkbox"
        onChange={(event) => onChange(event.currentTarget.checked)}
      />
      {label}
    </label>
  )
}

export function StatusBadge({
  children,
  tone = 'slate',
}: {
  children: ReactNode
  tone?: 'slate' | 'green' | 'amber' | 'red' | 'sky'
}): ReactElement {
  const toneClassMap = {
    slate: 'bg-slate-100 text-slate-600',
    green: 'bg-emerald-50 text-emerald-700',
    amber: 'bg-amber-50 text-amber-700',
    red: 'bg-red-50 text-red-700',
    sky: 'bg-sky-50 text-sky-700',
  }

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
        toneClassMap[tone],
      )}
    >
      {children}
    </span>
  )
}

export function ErrorBanner({
  error,
  title = '操作失败',
}: {
  error: unknown
  title?: string
}): ReactElement {
  const message = isBizError(error)
    ? (error.backendMessage ?? error.message)
    : error instanceof Error
      ? error.message
      : '请求处理失败，请稍后重试。'

  return (
    <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
      <div className="flex items-center gap-2 font-medium">
        <AlertCircle className="h-4 w-4" />
        {title}
      </div>
      <p className="mt-1">{message}</p>
    </div>
  )
}

export function EmptyState({
  description,
  title,
}: {
  title: string
  description: string
}): ReactElement {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-10 text-center">
      <p className="font-medium text-slate-900">{title}</p>
      <p className="mt-2 text-sm text-slate-500">{description}</p>
    </div>
  )
}

export function LoadingBlock(): ReactElement {
  return (
    <div className="space-y-3">
      <Skeleton className="h-10 w-56" />
      <Skeleton className="h-40 w-full" />
      <Skeleton className="h-40 w-full" />
    </div>
  )
}

export function NoPermissionPanel(): ReactElement {
  return (
    <div className="rounded-2xl border border-amber-100 bg-amber-50 p-6 text-amber-800">
      <div className="flex items-center gap-2 font-medium">
        <ShieldAlert className="h-5 w-5" />
        当前身份暂无门户管理权限
      </div>
      <p className="mt-2 text-sm">
        请切换到门户管理员或设计管理员身份后继续操作。
      </p>
    </div>
  )
}

export function SubmitButton({
  children,
  pending,
}: {
  children: ReactNode
  pending: boolean
}): ReactElement {
  return (
    <Button disabled={pending} type="submit">
      {children}
    </Button>
  )
}
