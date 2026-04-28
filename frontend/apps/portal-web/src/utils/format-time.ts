const DEFAULT_LOCALE = 'zh-CN'

export function getUserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}

export function formatUtcToUserTimezone(
  utcTimestamp: string | undefined,
  locale: string = DEFAULT_LOCALE,
  timezone: string = getUserTimezone(),
): string {
  if (!utcTimestamp) {
    return '暂无时间'
  }

  const parsedDate = new Date(utcTimestamp)

  if (Number.isNaN(parsedDate.getTime())) {
    return '时间格式异常'
  }

  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: timezone,
  }).format(parsedDate)
}
