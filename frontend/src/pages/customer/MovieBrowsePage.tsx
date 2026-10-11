import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { listShowsByDate } from '../../api/shows'
import { formatCurrency, formatTime } from '../../lib/format'

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

/** UC-07, FR-13/14/18 — only guaranteed, online-bookable shows for the chosen date ever appear here. */
export function MovieBrowsePage() {
  const [date, setDate] = useState(todayIso())

  const { data: shows, isPending, isError } = useQuery({
    queryKey: ['shows', date],
    queryFn: () => listShowsByDate(date),
  })

  return (
    <main className="mx-auto max-w-3xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Now Showing</h1>
      <p className="mt-1 text-neutral-600 dark:text-neutral-400">Vijaya Theatre, Batticaloa</p>

      <label className="mt-6 block text-sm font-medium text-neutral-700 dark:text-neutral-300">
        Date
        <input
          type="date"
          value={date}
          min={todayIso()}
          onChange={(event) => setDate(event.target.value)}
          className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
        />
      </label>

      {isPending && <p className="mt-8 text-neutral-500">Loading showtimes…</p>}
      {isError && <p className="mt-8 text-red-600">Could not load showtimes. Please try again.</p>}

      {shows && shows.length === 0 && (
        <p className="mt-8 text-neutral-500">No bookable shows on this date.</p>
      )}

      <ul className="mt-8 flex flex-col gap-3">
        {shows?.map((show) => (
          <li key={show.id}>
            <Link
              to={`/shows/${show.id}`}
              className="flex items-center justify-between rounded-lg border border-neutral-200 px-4 py-3 transition hover:border-neutral-400 dark:border-neutral-800 dark:hover:border-neutral-600"
            >
              <div>
                <p className="font-medium">{show.movieTitle}</p>
                <p className="text-sm text-neutral-500">
                  {formatTime(show.startTime)} · {show.onlineSeatsRemaining} online seats left
                </p>
              </div>
              <p className="text-sm text-neutral-600 dark:text-neutral-400">
                from {formatCurrency(show.childPrice)}
              </p>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  )
}
