import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getBookingByReference, qrCodeUrl } from '../../api/bookings'
import { formatCurrency, formatDate, formatTime } from '../../lib/format'

/** UC-12, FR-29 — self-service lookup by the reference given at booking time, no account needed. */
export function BookingStatusPage() {
  const [reference, setReference] = useState('')
  const lookup = useMutation({ mutationFn: getBookingByReference })

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (reference.trim()) lookup.mutate(reference.trim())
  }

  return (
    <main className="mx-auto max-w-xl px-4 py-8 sm:px-6">
      <h1 className="text-2xl font-bold">Track Booking Status</h1>
      <p className="mt-1 text-neutral-600">
        Enter the reference you received when you booked (e.g. VJY-20261201-ABC123).
      </p>

      <form onSubmit={handleSubmit} className="mt-6 flex gap-2">
        <input
          value={reference}
          onChange={(event) => setReference(event.target.value)}
          placeholder="VJY-20261201-ABC123"
          className="flex-1 rounded-lg border border-neutral-300 px-3 py-2 font-mono"
        />
        <button
          type="submit"
          disabled={lookup.isPending}
          className="rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
        >
          {lookup.isPending ? 'Looking up…' : 'Find'}
        </button>
      </form>

      {lookup.isError && <p className="mt-4 text-red-600">No booking found for that reference.</p>}

      {lookup.data && (
        <div className="mt-6 rounded-xl border border-neutral-200 bg-white p-4">
          <p className="flex justify-between">
            <span className="text-neutral-500">Status</span>
            <span className="font-semibold">{lookup.data.status.replace('_', ' ')}</span>
          </p>
          <p className="mt-2 text-neutral-600">
            {lookup.data.movieTitle} · {formatDate(lookup.data.showDate)} · {formatTime(lookup.data.startTime)}
          </p>
          <p className="mt-2">Seats: {lookup.data.seats.map((seat) => seat.seatNumber).join(', ')}</p>
          <p className="mt-2 flex justify-between font-semibold">
            <span>Total</span>
            <span>{formatCurrency(lookup.data.totalAmount)}</span>
          </p>

          {lookup.data.status === 'CONFIRMED' && (
            <img
              src={qrCodeUrl(lookup.data.id)}
              alt="Booking QR code"
              className="mx-auto mt-4 h-48 w-48 rounded-lg border border-neutral-200"
            />
          )}
          {lookup.data.status === 'PENDING_PAYMENT' && (
            <Link
              to={`/bookings/${lookup.data.id}/confirm`}
              className="mt-4 inline-block text-sm text-brand hover:underline"
            >
              Complete payment →
            </Link>
          )}
        </div>
      )}
    </main>
  )
}
