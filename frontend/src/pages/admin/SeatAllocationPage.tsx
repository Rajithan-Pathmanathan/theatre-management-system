import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { getSeatAllocationSummary, updateSeatAllocation } from '../../api/admin-seat-allocation'
import { listShowsByDate } from '../../api/admin-shows'
import type { SeatAllocationItemResponse } from '../../api/types'
import { formatTime } from '../../lib/format'

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

function seatClasses(seat: SeatAllocationItemResponse): string {
  if (seat.status === 'BOOKED') return 'border-neutral-400 bg-neutral-400 text-white dark:border-neutral-600 dark:bg-neutral-600'
  if (seat.status === 'LOCKED') return 'border-amber-400 bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200'
  return seat.channel === 'ONLINE'
    ? 'border-emerald-400 bg-emerald-50 text-emerald-700 dark:border-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
    : 'border-neutral-300 bg-white text-neutral-600 dark:border-neutral-700 dark:bg-neutral-950 dark:text-neutral-400'
}

/** UC-06, BR-05, BR-06. A BOOKED/LOCKED seat is frozen — the backend never moves it when the % changes. */
export function SeatAllocationPage() {
  const location = useLocation() as { state?: { showId?: string; showDate?: string } }
  const queryClient = useQueryClient()
  // The date filter drives which shows the dropdown lists — without the show's own
  // date here, a showId passed in from a different date wouldn't appear as an option,
  // leaving the dropdown showing some other (today's) show while the summary below is correct.
  const [date, setDate] = useState(location.state?.showDate ?? todayIso())
  const [showId, setShowId] = useState<string | null>(location.state?.showId ?? null)
  const [percent, setPercent] = useState<number | null>(null)

  const showsQuery = useQuery({ queryKey: ['admin-shows', date], queryFn: () => listShowsByDate(date) })
  const summaryQuery = useQuery({
    queryKey: ['seat-allocation', showId],
    queryFn: () => getSeatAllocationSummary(showId!),
    enabled: !!showId,
  })

  const updateMutation = useMutation({
    mutationFn: (newPercent: number) => updateSeatAllocation(showId!, newPercent),
    onSuccess: (summary) => {
      queryClient.setQueryData(['seat-allocation', showId], summary)
      setPercent(null)
    },
  })

  const seatsByRow = useMemo(() => {
    const rows = new Map<string, SeatAllocationItemResponse[]>()
    for (const seat of summaryQuery.data?.seats ?? []) {
      const row = rows.get(seat.rowLabel) ?? []
      row.push(seat)
      rows.set(seat.rowLabel, row)
    }
    for (const row of rows.values()) row.sort((a, b) => a.seatInRow - b.seatInRow)
    return [...rows.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [summaryQuery.data])

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Seat Allocation</h1>

      <div className="mt-4 flex gap-4">
        <label className="text-sm font-medium">
          Date
          <input
            type="date"
            value={date}
            onChange={(event) => {
              setDate(event.target.value)
              setShowId(null)
            }}
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>
        <label className="text-sm font-medium">
          Show
          <select
            value={showId ?? ''}
            onChange={(event) => setShowId(event.target.value || null)}
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          >
            <option value="">Select a show…</option>
            {showsQuery.data?.map((show) => (
              <option key={show.id} value={show.id}>
                {show.movieTitle} — {formatTime(show.startTime)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {summaryQuery.isPending && showId && <p className="mt-6 text-neutral-500">Loading…</p>}

      {summaryQuery.data && (
        <>
          <div className="mt-6 flex items-center gap-6">
            <p className="text-sm text-neutral-600 dark:text-neutral-400">
              {summaryQuery.data.onlineCount} online / {summaryQuery.data.physicalCount} physical of {summaryQuery.data.totalSeats}
            </p>
            <label className="flex items-center gap-2 text-sm font-medium">
              Online %
              <input
                type="number"
                min={0}
                max={100}
                value={percent ?? summaryQuery.data.onlineAllocationPercent}
                onChange={(event) => setPercent(Number(event.target.value))}
                className="w-20 rounded-md border border-neutral-300 px-2 py-1 dark:border-neutral-700 dark:bg-neutral-900"
              />
            </label>
            <button
              type="button"
              disabled={percent === null || updateMutation.isPending}
              onClick={() => updateMutation.mutate(percent!)}
              className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
            >
              {updateMutation.isPending ? 'Saving…' : 'Apply'}
            </button>
          </div>

          <div className="mt-4 flex gap-4 text-xs text-neutral-500">
            <span className="flex items-center gap-1">
              <span className="inline-block h-3 w-3 rounded border border-emerald-400 bg-emerald-50 dark:bg-emerald-950" /> Online
            </span>
            <span className="flex items-center gap-1">
              <span className="inline-block h-3 w-3 rounded border border-neutral-300 bg-white dark:bg-neutral-950" /> Physical
            </span>
            <span className="flex items-center gap-1">
              <span className="inline-block h-3 w-3 rounded border border-amber-400 bg-amber-100 dark:bg-amber-900" /> Locked
            </span>
            <span className="flex items-center gap-1">
              <span className="inline-block h-3 w-3 rounded border border-neutral-400 bg-neutral-400 dark:bg-neutral-600" /> Booked
            </span>
          </div>

          <div className="mt-3 flex flex-col gap-2">
            {seatsByRow.map(([rowLabel, seats]) => (
              <div key={rowLabel} className="flex items-center gap-2">
                <span className="w-5 text-xs font-mono text-neutral-400">{rowLabel}</span>
                <div className="flex flex-wrap gap-1">
                  {seats.map((seat) => (
                    <span
                      key={seat.seatId}
                      title={`${seat.seatNumber} · ${seat.channel} · ${seat.status}`}
                      className={`flex h-6 w-6 items-center justify-center rounded border text-[9px] font-medium ${seatClasses(seat)}`}
                    >
                      {seat.seatInRow}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </main>
  )
}
