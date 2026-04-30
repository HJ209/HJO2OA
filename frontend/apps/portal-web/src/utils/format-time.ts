const DEFAULT_LOCALE = 'zh-CN'
const LOCALE_STORAGE_KEY = 'hjo2oa.portal.locale'
const TIMEZONE_STORAGE_KEY = 'hjo2oa.portal.timezone'

export function getUserTimezone(): string {
  if (typeof localStorage !== 'undefined') {
    const storedTimezone = localStorage.getItem(TIMEZONE_STORAGE_KEY)

    if (storedTimezone) {
      return storedTimezone
    }
  }

  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}

export function setUserTimezone(timezone: string): void {
  if (typeof localStorage === 'undefined') {
    return
  }

  localStorage.setItem(TIMEZONE_STORAGE_KEY, timezone)
}

export function getUserLocale(): string {
  if (typeof localStorage !== 'undefined') {
    const storedLocale = localStorage.getItem(LOCALE_STORAGE_KEY)

    if (storedLocale) {
      return storedLocale
    }
  }

  return (
    navigator.language || import.meta.env.VITE_DEFAULT_LOCALE || DEFAULT_LOCALE
  )
}

export function setUserLocale(locale: string): void {
  if (typeof localStorage === 'undefined') {
    return
  }

  localStorage.setItem(LOCALE_STORAGE_KEY, locale)
}

export function formatUtcToUserTimezone(
  utcTimestamp: string | undefined,
  locale: string = getUserLocale(),
  timezone: string = getUserTimezone(),
): string {
  if (!utcTimestamp) {
    return 'N/A'
  }

  const parsedDate = new Date(utcTimestamp)

  if (Number.isNaN(parsedDate.getTime())) {
    return 'Invalid time'
  }

  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: timezone,
  }).format(parsedDate)
}
