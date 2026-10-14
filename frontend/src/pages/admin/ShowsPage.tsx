import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { listMovies } from '../../api/admin-movies'
import { createShow, listShowsByDate, updateShow, updateShowStatus } from '../../api/admin-shows'
import type { ShowRequest, ShowResponse, ShowStatus } from '../../api/types'
import { formatCurrency, formatTime, todayIso } from '../../lib/format'

function emptyForm(date: string): ShowRequest {
  return {
    movieId: '',
    showDate: date,
    startTime: '18:00',
    adultPrice: 500,
    childPrice: 350,
    guaranteed: false,
    onlineBookingEnabled: false,
    onlineAllocationPercent: 0,
    hasInterval: true,
  }
}

const STATUS_BADGE: Record<ShowStatus, string> = {
  SCHEDULED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-red-100 text-red-800',
  COMPLETED: 'bg-neutral-100 text-neutral-600',
}

/** UC-05, BR-01. Online booking requires `guaranteed` — the backend rejects onlineBookingEnabled without it. */
export function ShowsPage() {
  const queryClient = useQueryClient()
  const [date, setDate] = useState(todayIso())
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<ShowRequest>(emptyForm(todayIso()))

  const moviesQuery = useQuery({ queryKey: ['admin-movies'], queryFn: listMovies })
  const showsQuery = useQuery({ queryKey: ['admin-shows', date], queryFn: () => listShowsByDate(date) })

  const saveMutation = useMutation({
    mutationFn: (request: ShowRequest) => (editingId ? updateShow(editingId, request) : createShow(request)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-shows'] })
      setEditingId(null)
      setForm(emptyForm(date))
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ShowStatus }) => updateShowStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-shows'] }),
  })

  function startEdit(show: ShowResponse) {
    setEditingId(show.id)
    setForm({
      movieId: show.movieId,
      showDate: show.showDate,
      startTime: show.startTime.slice(0, 5),
      adultPrice: show.adultPrice,
      childPrice: show.childPrice,
      guaranteed: show.guaranteed,
      onlineBookingEnabled: show.onlineBookingEnabled,
      onlineAllocationPercent: show.onlineAllocationPercent,
      hasInterval: show.hasInterval,
    })
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    saveMutation.mutate({ ...form, startTime: form.startTime.length === 5 ? `${form.startTime}:00` : form.startTime })
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Shows</h1>

      <label className="mt-4 block text-sm font-medium">
        Date
        <input
          type="date"
          value={date}
          onChange={(event) => {
            setDate(event.target.value)
            if (!editingId) setForm(emptyForm(event.target.value))
          }}
          className="mt-1 block rounded-lg border border-neutral-300 px-3 py-2"
        />
      </label>

      <div className="mt-6 grid gap-8 md:grid-cols-[2fr_1fr]">
        <div className="flex flex-col gap-2">
          {showsQuery.isPending && <p className="text-neutral-500">Loading…</p>}
          {showsQuery.data?.length === 0 && <p className="text-neutral-500">No shows on this date.</p>}
          {showsQuery.data?.map((show) => (
            <div key={show.id} className="rounded-xl border border-neutral-200 bg-white px-4 py-3">
              <div className="flex items-center justify-between">
                <button type="button" onClick={() => startEdit(show)} className="text-left">
                  <p className="font-medium">{show.movieTitle}</p>
                  <p className="text-sm text-neutral-500">
                    {formatTime(show.startTime)} · {formatCurrency(show.adultPrice)} / {formatCurrency(show.childPrice)}
                  </p>
                </button>
                <div className="flex items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[show.status]}`}>{show.status}</span>
                  <select
                    value={show.status}
                    onChange={(event) => statusMutation.mutate({ id: show.id, status: event.target.value as ShowStatus })}
                    className="rounded-lg border border-neutral-300 bg-transparent px-1 py-1 text-xs"
                  >
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="CANCELLED">Cancelled</option>
                    <option value="COMPLETED">Completed</option>
                  </select>
                </div>
              </div>
              <div className="mt-1 flex gap-3 text-xs text-neutral-500">
                {show.guaranteed && <span>Guaranteed</span>}
                {show.onlineBookingEnabled && <span>Online {show.onlineAllocationPercent}%</span>}
                <Link
                  to="/admin/seat-allocation"
                  state={{ showId: show.id, showDate: show.showDate }}
                  className="text-brand hover:underline"
                >
                  Seat allocation →
                </Link>
              </div>
            </div>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <h2 className="font-medium">{editingId ? 'Edit Show' : 'New Show'}</h2>
          <label className="text-sm font-medium">
            Movie
            <select
              value={form.movieId}
              onChange={(event) => setForm({ ...form, movieId: event.target.value })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
            >
              <option value="">Select a movie…</option>
              {moviesQuery.data?.map((movie) => (
                <option key={movie.id} value={movie.id}>
                  {movie.title}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium">
            Date
            <input
              type="date"
              value={form.showDate}
              onChange={(event) => setForm({ ...form, showDate: event.target.value })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>
          <label className="text-sm font-medium">
            Start time
            <input
              type="time"
              value={form.startTime}
              onChange={(event) => setForm({ ...form, startTime: event.target.value })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>
          <div className="flex gap-3">
            <label className="text-sm font-medium">
              Adult price
              <input
                type="number"
                min={0}
                value={form.adultPrice}
                onChange={(event) => setForm({ ...form, adultPrice: Number(event.target.value) })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
              />
            </label>
            <label className="text-sm font-medium">
              Child price
              <input
                type="number"
                min={0}
                value={form.childPrice}
                onChange={(event) => setForm({ ...form, childPrice: Number(event.target.value) })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
              />
            </label>
          </div>
          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={form.guaranteed}
              onChange={(event) => setForm({ ...form, guaranteed: event.target.checked })}
            />
            Guaranteed (BR-01)
          </label>
          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={form.onlineBookingEnabled}
              disabled={!form.guaranteed}
              onChange={(event) => setForm({ ...form, onlineBookingEnabled: event.target.checked })}
            />
            Online booking enabled
          </label>
          <label className="text-sm font-medium">
            Online allocation %
            <input
              type="number"
              min={0}
              max={100}
              value={form.onlineAllocationPercent}
              onChange={(event) => setForm({ ...form, onlineAllocationPercent: Number(event.target.value) })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>
          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={form.hasInterval}
              onChange={(event) => setForm({ ...form, hasInterval: event.target.checked })}
            />
            Has interval
          </label>

          {saveMutation.isError && (
            <p className="text-sm text-red-600">
              {(saveMutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                'Could not save this show.'}
            </p>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={saveMutation.isPending || !form.movieId}
              className="rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
            >
              {editingId ? 'Save' : 'Create'}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={() => {
                  setEditingId(null)
                  setForm(emptyForm(date))
                }}
                className="rounded-lg border border-neutral-300 px-4 py-2 font-medium"
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
