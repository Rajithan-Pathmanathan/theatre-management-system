import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listPublicMovies } from '../../api/movies'
import { listShowsByDate } from '../../api/shows'
import type { MovieResponse } from '../../api/types'
import { MoviePoster } from '../../components/MoviePoster'
import { todayIso } from '../../lib/format'

/** Rotating banner of the movies now showing; auto-advances, but the arrows and dots stay in the customer's control. */
function HeroCarousel({ movies }: { movies: MovieResponse[] }) {
  const [index, setIndex] = useState(0)
  const current = index % movies.length

  useEffect(() => {
    if (movies.length < 2) return
    const timer = setInterval(() => setIndex((i) => i + 1), 6000)
    return () => clearInterval(timer)
  }, [movies.length, index])

  return (
    <section aria-roledescription="carousel" aria-label="Now showing" className="relative overflow-hidden rounded-2xl bg-neutral-900">
      <div className="flex transition-transform duration-700 motion-reduce:transition-none" style={{ transform: `translateX(-${current * 100}%)` }}>
        {movies.map((movie, i) => (
          <div key={movie.id} aria-hidden={i !== current} className="relative w-full shrink-0">
            {movie.posterUrl && (
              <img src={movie.posterUrl} alt="" className="absolute inset-0 h-full w-full scale-110 object-cover opacity-40 blur-md" />
            )}
            <div className="relative flex items-center gap-5 bg-gradient-to-r from-neutral-950/90 via-neutral-950/60 to-transparent p-5 pb-10 sm:gap-8 sm:p-10">
              <MoviePoster title={movie.title} posterUrl={movie.posterUrl} className="w-28 shrink-0 shadow-2xl sm:w-44" />
              <div className="min-w-0 text-white">
                <p className="text-xs font-semibold tracking-widest text-brand uppercase">Now showing</p>
                <h2 className="mt-1 text-2xl font-bold sm:text-4xl">{movie.title}</h2>
                <p className="mt-2 text-sm text-neutral-300">
                  {movie.language} · {movie.genre}
                </p>
                {movie.synopsis && <p className="mt-3 hidden max-w-xl text-sm text-neutral-300 sm:line-clamp-2">{movie.synopsis}</p>}
                <Link
                  to={`/movies/${movie.id}`}
                  tabIndex={i === current ? 0 : -1}
                  className="mt-5 inline-block rounded-lg bg-brand px-5 py-2.5 font-semibold text-white transition hover:bg-brand-dark"
                >
                  Book tickets
                </Link>
              </div>
            </div>
          </div>
        ))}
      </div>

      {movies.length > 1 && (
        <>
          <button
            onClick={() => setIndex(current + movies.length - 1)}
            aria-label="Previous movie"
            className="absolute top-1/2 left-2 hidden h-9 w-9 -translate-y-1/2 rounded-full bg-white/80 text-xl text-neutral-900 hover:bg-white sm:block"
          >
            ‹
          </button>
          <button
            onClick={() => setIndex(current + 1)}
            aria-label="Next movie"
            className="absolute top-1/2 right-2 hidden h-9 w-9 -translate-y-1/2 rounded-full bg-white/80 text-xl text-neutral-900 hover:bg-white sm:block"
          >
            ›
          </button>
          <div className="absolute inset-x-0 bottom-3 flex justify-center gap-2">
            {movies.map((movie, i) => (
              <button
                key={movie.id}
                onClick={() => setIndex(i)}
                aria-label={`Show ${movie.title}`}
                aria-current={i === current}
                className={`h-2 rounded-full transition-all ${i === current ? 'w-6 bg-white' : 'w-2 bg-white/50'}`}
              />
            ))}
          </div>
        </>
      )}
    </section>
  )
}

function PosterGrid({ title, movies }: { title: string; movies: MovieResponse[] }) {
  return (
    <section className="mt-10">
      <h2 className="text-xl font-bold">{title}</h2>
      <ul className="mt-4 grid grid-cols-2 gap-x-4 gap-y-6 sm:grid-cols-3 lg:grid-cols-5">
        {movies.map((movie) => (
          <li key={movie.id}>
            <Link to={`/movies/${movie.id}`} className="group block">
              <MoviePoster title={movie.title} posterUrl={movie.posterUrl} className="w-full shadow-sm transition group-hover:-translate-y-1 group-hover:shadow-lg" />
              <p className="mt-2 font-semibold group-hover:text-brand">{movie.title}</p>
              <p className="text-sm text-neutral-500">
                {movie.language} · {movie.genre}
              </p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** UC-07. Movies first; a movie's page then lists only its guaranteed, online-bookable shows (FR-13/14/18). */
export function MovieBrowsePage() {
  const { data: movies, isPending, isError } = useQuery({ queryKey: ['movies'], queryFn: listPublicMovies })
  const today = todayIso()
  const { data: todaysShows } = useQuery({ queryKey: ['shows', today], queryFn: () => listShowsByDate(today) })

  // A movie with a bookable show today is "now showing" even if admin still has it as UPCOMING.
  const isShowing = (movie: MovieResponse) => movie.status === 'SHOWING' || !!todaysShows?.some((show) => show.movieId === movie.id)
  const showing = movies?.filter(isShowing) ?? []
  const upcoming = movies?.filter((movie) => !isShowing(movie)) ?? []

  return (
    <main className="mx-auto max-w-6xl px-4 py-6 sm:px-6">
      {isPending && <p className="py-10 text-neutral-500">Loading movies…</p>}
      {isError && <p className="py-10 text-red-600">Could not load movies. Please try again.</p>}
      {movies && showing.length === 0 && upcoming.length === 0 && <p className="py-10 text-neutral-500">No movies are listed right now.</p>}

      {showing.length > 0 && <HeroCarousel movies={showing} />}
      {showing.length > 0 && <PosterGrid title="Now Showing" movies={showing} />}
      {upcoming.length > 0 && <PosterGrid title="Coming Soon" movies={upcoming} />}
    </main>
  )
}
