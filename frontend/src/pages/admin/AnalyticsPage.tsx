import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getMovieAnalytics, getOccupancyTrend } from '../../api/admin-analytics'
import { listMovies } from '../../api/admin-movies'
import { formatCurrency, formatDate, formatTime, todayIso, toIsoDate } from '../../lib/format'

function plusDaysIso(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

/** UC-15, FR-40–FR-45. Historical decision-support — occupancy, attendance, and channel-performance trends, not a forecast. */
export function AnalyticsPage() {
  const [movieId, setMovieId] = useState('')
  const [from, setFrom] = useState(todayIso())
  const [to, setTo] = useState(plusDaysIso(30))

  const moviesQuery = useQuery({ queryKey: ['admin-movies'], queryFn: listMovies })
  const movieAnalyticsQuery = useQuery({
    queryKey: ['movie-analytics', movieId],
    queryFn: () => getMovieAnalytics(movieId),
    enabled: !!movieId,
  })
  const occupancyQuery = useQuery({ queryKey: ['occupancy-trend', from, to], queryFn: () => getOccupancyTrend(from, to) })

  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <h1 className="text-2xl font-bold">Analytics</h1>

      <section className="mt-6">
        <h2 className="text-lg font-semibold">Movie performance</h2>
        <label className="mt-2 block text-sm font-medium">
          Movie
          <select
            value={movieId}
            onChange={(event) => setMovieId(event.target.value)}
            className="mt-1 block w-full max-w-sm rounded-lg border border-neutral-300 px-3 py-2"
          >
            <option value="">Select a movie…</option>
            {moviesQuery.data?.map((movie) => (
              <option key={movie.id} value={movie.id}>
                {movie.title}
              </option>
            ))}
          </select>
        </label>

        {movieAnalyticsQuery.data && (
          <div className="mt-4 grid grid-cols-2 gap-4 md:grid-cols-4">
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Shows</p>
              <p className="text-2xl font-bold">{movieAnalyticsQuery.data.totalShows}</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Avg. occupancy</p>
              <p className="text-2xl font-bold">{movieAnalyticsQuery.data.averageOccupancyPercent}%</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Attendance rate</p>
              <p className="text-2xl font-bold">{movieAnalyticsQuery.data.attendanceRatePercent}%</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Collections</p>
              <p className="text-2xl font-bold">{formatCurrency(movieAnalyticsQuery.data.collections)}</p>
            </div>
            <div className="col-span-2 rounded-xl border border-neutral-200 bg-white p-4 md:col-span-4">
              <p className="text-sm text-neutral-500">
                {movieAnalyticsQuery.data.ticketsSold} tickets sold ({movieAnalyticsQuery.data.ticketsUsed} checked in) — online{' '}
                {movieAnalyticsQuery.data.online.ticketsSold} ({formatCurrency(movieAnalyticsQuery.data.online.collections)}), physical{' '}
                {movieAnalyticsQuery.data.physical.ticketsSold} ({formatCurrency(movieAnalyticsQuery.data.physical.collections)})
              </p>
            </div>
          </div>
        )}
      </section>

      <section className="mt-10">
        <h2 className="text-lg font-semibold">Occupancy trend</h2>
        <div className="mt-2 flex gap-4">
          <label className="text-sm font-medium">
            From
            <input
              type="date"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
              className="mt-1 block rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>
          <label className="text-sm font-medium">
            To
            <input
              type="date"
              value={to}
              onChange={(event) => setTo(event.target.value)}
              className="mt-1 block rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>
        </div>

        {occupancyQuery.isError && <p className="mt-4 text-red-600">Could not load the trend for this range.</p>}
        {occupancyQuery.data?.length === 0 && <p className="mt-4 text-neutral-500">No shows in this date range.</p>}

        <div className="mt-4 flex flex-col gap-2">
          {occupancyQuery.data?.map((show) => (
            <div key={show.showId} className="flex items-center justify-between rounded-xl border border-neutral-200 bg-white px-4 py-3">
              <div>
                <p className="font-medium">{show.movieTitle}</p>
                <p className="text-sm text-neutral-500">
                  {formatDate(show.showDate)} · {formatTime(show.startTime)}
                </p>
              </div>
              <div className="text-right text-sm">
                <p>
                  {show.ticketsSold}/{show.totalSeats} seats ({show.occupancyPercent}%)
                </p>
                <p className="text-neutral-500">{show.attendanceRatePercent}% attendance</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}
