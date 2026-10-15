import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { listPublicMovies } from '../../api/movies'
import { listShowsByDate } from '../../api/shows'
import { MoviePoster } from '../../components/MoviePoster'
import { formatCurrency, formatTime, toIsoDate } from '../../lib/format'

const DAYS_AHEAD = 7

function nextDays(): Date[] {
  const today = new Date()
  return Array.from({ length: DAYS_AHEAD }, (_, i) => new Date(today.getFullYear(), today.getMonth(), today.getDate() + i))
}

/** UC-07 → UC-08. Movie details, then a date strip and show-time chips; only online-bookable shows come back from the API. */
export function MovieDetailPage() {
  const { movieId } = useParams<{ movieId: string }>()
  const days = nextDays()
  const [date, setDate] = useState(toIsoDate(days[0]))

  const moviesQuery = useQuery({ queryKey: ['movies'], queryFn: listPublicMovies })
  const showsQuery = useQuery({ queryKey: ['shows', date], queryFn: () => listShowsByDate(date) })

  if (moviesQuery.isPending) return <p className="px-6 py-10 text-neutral-500">Loading…</p>
  const movie = moviesQuery.data?.find((m) => m.id === movieId)
  if (!movie) return <p className="px-6 py-10 text-red-600">This movie is not available.</p>

  const shows = showsQuery.data?.filter((show) => show.movieId === movie.id).sort((a, b) => a.startTime.localeCompare(b.startTime))
  const hours = Math.floor(movie.durationMinutes / 60)

  return (
    <main>
      <section className="relative overflow-hidden bg-neutral-900 text-white">
        {movie.posterUrl && <img src={movie.posterUrl} alt="" className="absolute inset-0 h-full w-full scale-110 object-cover opacity-30 blur-lg" />}
        <div className="absolute inset-0 bg-gradient-to-r from-neutral-950/90 to-neutral-950/40" />
        <div className="relative mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:flex-row sm:items-center sm:gap-10 sm:px-6 sm:py-12">
          <MoviePoster title={movie.title} posterUrl={movie.posterUrl} className="w-40 shrink-0 shadow-2xl sm:w-60" />
          <div>
            <h1 className="text-3xl font-bold sm:text-4xl">{movie.title}</h1>
            <div className="mt-4 flex flex-wrap gap-2 text-sm">
              <span className="rounded bg-white/90 px-2 py-0.5 font-medium text-neutral-900">{movie.language}</span>
              <span className="rounded bg-white/90 px-2 py-0.5 font-medium text-neutral-900">{movie.genre}</span>
            </div>
            <p className="mt-3 text-neutral-300">
              {hours > 0 && `${hours}h `}
              {movie.durationMinutes % 60}m
            </p>
            <a
              href="#showtimes"
              className="mt-6 inline-block rounded-lg bg-brand px-8 py-3 text-lg font-semibold text-white transition hover:bg-brand-dark"
            >
              Book tickets
            </a>
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        {movie.synopsis && (
          <section className="mt-8">
            <h2 className="text-xl font-bold">About the movie</h2>
            <p className="mt-2 max-w-3xl text-neutral-700">{movie.synopsis}</p>
          </section>
        )}

        <section id="showtimes" className="mt-8 mb-12 scroll-mt-24 rounded-xl border border-neutral-200 bg-white">
          <div className="flex gap-2 overflow-x-auto border-b border-neutral-200 p-3">
            {days.map((day) => {
              const iso = toIsoDate(day)
              const selected = iso === date
              return (
                <button
                  key={iso}
                  onClick={() => setDate(iso)}
                  aria-pressed={selected}
                  className={`flex w-14 shrink-0 flex-col items-center rounded-lg py-1.5 text-xs font-medium transition ${
                    selected ? 'bg-brand text-white' : 'text-neutral-600 hover:bg-brand-soft'
                  }`}
                >
                  <span className="uppercase">{day.toLocaleDateString('en-LK', { weekday: 'short' })}</span>
                  <span className="text-lg font-bold">{day.getDate()}</span>
                  <span className="uppercase">{day.toLocaleDateString('en-LK', { month: 'short' })}</span>
                </button>
              )
            })}
          </div>

          <div className="p-4 sm:p-6">
            <p className="font-semibold">Vijaya Theatre, Batticaloa</p>
            {showsQuery.isPending && <p className="mt-3 text-neutral-500">Loading showtimes…</p>}
            {showsQuery.isError && <p className="mt-3 text-red-600">Could not load showtimes. Please try again.</p>}
            {shows?.length === 0 && <p className="mt-3 text-neutral-500">No bookable shows on this date.</p>}
            <ul className="mt-3 flex flex-wrap gap-3">
              {shows?.map((show) => (
                <li key={show.id}>
                  <Link
                    to={`/shows/${show.id}`}
                    title={`From ${formatCurrency(show.childPrice)} · ${show.onlineSeatsRemaining} online seats left`}
                    className="flex flex-col items-center rounded-lg border border-emerald-500 px-5 py-2 text-emerald-700 transition hover:bg-emerald-50"
                  >
                    <span className="font-semibold">{formatTime(show.startTime)}</span>
                    <span className="text-[11px] text-neutral-500">{show.onlineSeatsRemaining} seats left</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </section>
      </div>
    </main>
  )
}
