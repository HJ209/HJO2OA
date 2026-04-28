import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '@/utils/cn'

type ButtonVariant = 'default' | 'outline' | 'ghost'
type ButtonSize = 'default' | 'sm' | 'icon'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
}

const variantClassMap: Record<ButtonVariant, string> = {
  default:
    'bg-slate-900 text-white shadow-sm hover:bg-slate-800 disabled:bg-slate-300 disabled:text-slate-500',
  outline:
    'border border-slate-200 bg-white text-slate-900 hover:bg-slate-50 disabled:border-slate-100 disabled:text-slate-400',
  ghost:
    'bg-transparent text-slate-700 hover:bg-slate-100 disabled:text-slate-400',
}

const sizeClassMap: Record<ButtonSize, string> = {
  default: 'h-10 px-4 py-2',
  sm: 'h-9 px-3 py-2 text-sm',
  icon: 'h-10 w-10 p-0',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  function Button(
    {
      className,
      variant = 'default',
      size = 'default',
      type = 'button',
      ...props
    },
    ref,
  ) {
    return (
      <button
        className={cn(
          'inline-flex items-center justify-center gap-2 rounded-xl text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 focus-visible:ring-offset-2 disabled:pointer-events-none',
          variantClassMap[variant],
          sizeClassMap[size],
          className,
        )}
        ref={ref}
        type={type}
        {...props}
      />
    )
  },
)
