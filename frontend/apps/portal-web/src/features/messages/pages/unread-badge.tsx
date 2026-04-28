import { useState, type ReactElement } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useUnreadCount } from '@/features/messages/hooks/use-unread-count'

interface UnreadBadgeProps {
  count?: number
}

function formatCount(count: number): string {
  return count > 99 ? '99+' : String(count)
}

function UnreadBadgeCount({ count }: { count: number }): ReactElement | null {
  if (count <= 0) {
    return null
  }

  return (
    <span
      aria-label={`未读消息 ${count} 条`}
      className="absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full bg-rose-500 px-1.5 text-xs font-semibold leading-5 text-white shadow-sm"
      role="status"
    >
      {formatCount(count)}
    </span>
  )
}

export function UnreadBadge({ count }: UnreadBadgeProps): ReactElement | null {
  if (count !== undefined) {
    return <UnreadBadgeCount count={count} />
  }

  return <UnreadBadgeWithProvider />
}

function UnreadBadgeQueryContent(): ReactElement | null {
  const unreadCountQuery = useUnreadCount()

  return <UnreadBadgeCount count={unreadCountQuery.data?.count ?? 0} />
}

function UnreadBadgeWithProvider(): ReactElement {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
            staleTime: 30000,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      <UnreadBadgeQueryContent />
    </QueryClientProvider>
  )
}
