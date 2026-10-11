import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { searchBookings } from '../../api/admin-bookings'
import type { BookingAdminResponse, BookingAdminSearchParams, BookingStatus } from '../../api/types'
import { formatCurrency, formatDate, formatTime } from '../../lib/format'

const STATUS_BADGE: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  CONFIRMED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200',
  EXPIRED: 'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  REFUNDED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
}

/** UC-13. A unified view of both sales channels — richer than the guest-facing booking response. */
export function BookingsPage() {
  const [showDate, setShowDate] = useState('')
  const [status, setStatus] = useState('')
  const [channel, setChannel] = useState('')
  const [reference, setReference] = useState('')
  const [selected, setSelected] = useState<BookingAdminResponse | null>(null)

  const searchMutation = useMutation({ mutationFn: (params: BookingAdminSearchParams) => searchBookings(params) })

  function runSearch() {
    const params: BookingAdminSearchParams = {}
    if (showDate) params.showDate = showDate
    if (status) params.status = status as BookingStatus
    if (channel) params.channel = channel as 'ONLINE' | 'COUNTER'
    if (reference) params.reference = reference
    searchMutation.mutate(params)
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Bookings</h1>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <label className="text-sm font-medium">
          Show date
          <input
            type="date"
            value={showDate}
            onChange={(event) => setShowDate(event.target.value)}
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>
        <label className="text-sm font-medium">
          Status
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          >
            <option value="">Any</option>
            <option value="PENDING_PAYMENT">Pending Payment</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="EXPIRED">Expired</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="REFUNDED">Refunded</option>
          </select>
        </label>
        <label className="text-sm font-medium">
          Channel
          <select
            value={channel}
            onChange={(event) => setChannel(event.target.value)}
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          >
            <option value="">Any</option>
            <option value="ONLINE">Online</option>
            <option value="COUNTER">Counter</option>
          </select>
        </label>
        <label className="text-sm font-medium">
          Reference
          <input
            value={reference}
            onChange={(event) => setReference(event.target.value)}
            placeholder="VJY-20261201-ABC123"
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 font-mono dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>
        <button
          type="button"
          onClick={runSearch}
          disabled={searchMutation.isPending}
          className="rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
        >
          {searchMutation.isPending ? 'Searching…' : 'Search'}
        </button>
      </div>

      <div className="mt-6 grid gap-6 md:grid-cols-[2fr_1fr]">
        <div className="flex flex-col gap-2">
          {!searchMutation.data && !searchMutation.isPending && (
            <p className="text-neutral-500">Set filters and search — showing nothing by default to avoid loading every booking.</p>
          )}
          {searchMutation.data?.length === 0 && <p className="text-neutral-500">No bookings match these filters.</p>}
          {searchMutation.data?.map((booking) => (
            <button
              key={booking.id}
              type="button"
              onClick={() => setSelected(booking)}
              className={`flex items-center justify-between rounded-lg border px-4 py-3 text-left ${
                selected?.id === booking.id ? 'border-emerald-500' : 'border-neutral-200 dark:border-neutral-800'
              }`}
            >
              <div>
                <p className="font-mono text-sm">{booking.bookingReference}</p>
                <p className="text-sm text-neutral-500">
                  {booking.movieTitle} · {formatDate(booking.showDate)} · {formatTime(booking.startTime)} · {booking.channel}
                </p>
              </div>
              <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[booking.status]}`}>{booking.status}</span>
            </button>
          ))}
        </div>

        {selected && (
          <div className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
            <p className="font-mono text-lg">{selected.bookingReference}</p>
            <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[selected.status]}`}>
              {selected.status}
            </span>
            <p className="mt-3 text-sm text-neutral-600 dark:text-neutral-400">
              {selected.movieTitle} · {formatDate(selected.showDate)} · {formatTime(selected.startTime)}
            </p>
            <p className="mt-2 text-sm">
              {selected.customerName} · {selected.customerPhone}
            </p>
            <p className="mt-2 text-sm">Seats: {selected.seats.map((seat) => seat.seatNumber).join(', ')}</p>
            <p className="mt-2 text-sm">
              Checked in {selected.ticketsUsed} of {selected.adultCount + selected.childCount}
            </p>
            {selected.channel === 'COUNTER' && (
              <p className="mt-2 text-sm">
                {selected.physicalPaymentMethod} · sold by {selected.soldByStaffUsername}
              </p>
            )}
            <p className="mt-3 flex justify-between font-semibold">
              <span>Total</span>
              <span>{formatCurrency(selected.totalAmount)}</span>
            </p>
          </div>
        )}
      </div>
    </main>
  )
}
