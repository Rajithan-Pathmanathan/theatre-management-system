export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', minimumFractionDigits: 2 }).format(amount)
}

export function formatTime(time: string): string {
  const [hourStr, minuteStr] = time.split(':')
  const hour = Number(hourStr)
  const period = hour >= 12 ? 'PM' : 'AM'
  const displayHour = ((hour + 11) % 12) + 1
  return `${displayHour}:${minuteStr} ${period}`
}

export function formatDate(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString('en-LK', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  })
}

export const PICKUP_LABELS = {
  BEFORE_MOVIE: 'Before the movie',
  DURING_INTERVAL: 'During the interval',
  BOTH: 'Before the movie and at the interval',
} as const

/** YYYY-MM-DD in the browser's own time zone. toISOString() is UTC, which is still "yesterday" in Sri Lanka until 05:30. */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function todayIso(): string {
  return toIsoDate(new Date())
}
