import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createMovie, listMovies, updateMovie, updateMovieStatus } from '../../api/admin-movies'
import type { MovieRequest, MovieResponse, MovieStatus } from '../../api/types'

const EMPTY_FORM: MovieRequest = { title: '', language: '', genre: '', durationMinutes: 120, posterUrl: null, synopsis: null }

const STATUS_BADGE: Record<MovieStatus, string> = {
  SHOWING: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200',
  UPCOMING: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  INACTIVE: 'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300',
}

/** UC-04, FR-01–FR-03. Movies are never deleted, only moved to INACTIVE. */
export function MoviesPage() {
  const queryClient = useQueryClient()
  const moviesQuery = useQuery({ queryKey: ['admin-movies'], queryFn: listMovies })

  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<MovieRequest>(EMPTY_FORM)

  const saveMutation = useMutation({
    mutationFn: (request: MovieRequest) => (editingId ? updateMovie(editingId, request) : createMovie(request)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-movies'] })
      setEditingId(null)
      setForm(EMPTY_FORM)
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: MovieStatus }) => updateMovieStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-movies'] }),
  })

  function startEdit(movie: MovieResponse) {
    setEditingId(movie.id)
    setForm({
      title: movie.title,
      language: movie.language,
      genre: movie.genre,
      durationMinutes: movie.durationMinutes,
      posterUrl: movie.posterUrl,
      synopsis: movie.synopsis,
    })
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Movies</h1>

      <div className="mt-6 grid gap-8 md:grid-cols-[2fr_1fr]">
        <div className="flex flex-col gap-2">
          {moviesQuery.isPending && <p className="text-neutral-500">Loading…</p>}
          {moviesQuery.data?.map((movie) => (
            <div key={movie.id} className="flex items-center justify-between rounded-lg border border-neutral-200 px-4 py-3 dark:border-neutral-800">
              <button type="button" onClick={() => startEdit(movie)} className="text-left">
                <p className="font-medium">{movie.title}</p>
                <p className="text-sm text-neutral-500">
                  {movie.language} · {movie.genre} · {movie.durationMinutes} min
                </p>
              </button>
              <div className="flex items-center gap-2">
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[movie.status]}`}>{movie.status}</span>
                <select
                  value={movie.status}
                  onChange={(event) => statusMutation.mutate({ id: movie.id, status: event.target.value as MovieStatus })}
                  className="rounded-md border border-neutral-300 bg-transparent px-1 py-1 text-xs dark:border-neutral-700"
                >
                  <option value="SHOWING">Showing</option>
                  <option value="UPCOMING">Upcoming</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
            </div>
          ))}
          {moviesQuery.data?.length === 0 && <p className="text-neutral-500">No movies yet.</p>}
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <h2 className="font-medium">{editingId ? 'Edit Movie' : 'New Movie'}</h2>
          <label className="text-sm font-medium">
            Title
            <input
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Language
            <input
              value={form.language}
              onChange={(event) => setForm({ ...form, language: event.target.value })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Genre
            <input
              value={form.genre}
              onChange={(event) => setForm({ ...form, genre: event.target.value })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Duration (minutes)
            <input
              type="number"
              min={1}
              value={form.durationMinutes}
              onChange={(event) => setForm({ ...form, durationMinutes: Number(event.target.value) })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Synopsis (optional)
            <textarea
              value={form.synopsis ?? ''}
              onChange={(event) => setForm({ ...form, synopsis: event.target.value || null })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
              rows={3}
            />
          </label>

          {saveMutation.isError && <p className="text-sm text-red-600">Could not save. Please check the fields and try again.</p>}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={saveMutation.isPending}
              className="rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
            >
              {editingId ? 'Save' : 'Create'}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={() => {
                  setEditingId(null)
                  setForm(EMPTY_FORM)
                }}
                className="rounded-md border border-neutral-300 px-4 py-2 font-medium dark:border-neutral-700"
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>
    </main>
  )
}
