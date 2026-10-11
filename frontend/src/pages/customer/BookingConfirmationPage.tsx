import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getBooking } from '../../api/bookings'
import { initiatePayment, redirectToPayHereCheckout } from '../../api/payments'
import { formatCurrency, formatDate, formatTime } from '../../lib/format'

function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

/** UC-09. The booking is still PENDING_PAYMENT here — BR-03's 10-minute lock is ticking. */
export function BookingConfirmationPage() {
  const { bookingId } = useParams<{ bookingId: string }>()
  const bookingQuery = useQuery({ queryKey: ['booking', bookingId], queryFn: () => getBooking(bookingId!), enabled: !!bookingId })

  // Ticks once a second purely to force a re-render; secondsLeft itself is derived below, not stored.
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(interval)
  }, [])

  const expiresAt = bookingQuery.data?.expiresAt
  const secondsLeft = expiresAt ? Math.max(0, Math.floor((new Date(expiresAt).getTime() - now) / 1000)) : null

  const paymentMutation = useMutation({
    mutationFn: () => initiatePayment(bookingId!),
    onSuccess: redirectToPayHereCheckout,
  })

  if (bookingQuery.isPending) return <p className="px-6 py-10 text-neutral-500">Loading…</p>
  if (bookingQuery.isError || !bookingQuery.data) {
    return <p className="px-6 py-10 text-red-600">Booking not found.</p>
  }

  const booking = bookingQuery.data
  const expired = secondsLeft !== null && secondsLeft <= 0

  return (
    <main className="mx-auto max-w-xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Confirm & Pay</h1>
      <p className="text-neutral-600 dark:text-neutral-400">
        {booking.movieTitle} · {formatDate(booking.showDate)} · {formatTime(booking.startTime)}
      </p>

      <div className="mt-6 rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
        <p className="text-sm text-neutral-500">Booking reference</p>
        <p className="font-mono text-lg">{booking.bookingReference}</p>

        <p className="mt-4 text-sm text-neutral-500">Seats</p>
        <p>{booking.seats.map((seat) => seat.seatNumber).join(', ')}</p>

        <p className="mt-4 flex justify-between text-sm">
          <span>Tickets</span>
          <span>
            {booking.adultCount} adult, {booking.childCount} child
          </span>
        </p>
        <p className="mt-1 flex justify-between font-semibold">
          <span>Total</span>
          <span>{formatCurrency(booking.totalAmount)}</span>
        </p>
      </div>

      {booking.status !== 'PENDING_PAYMENT' ? (
        <p className="mt-6 text-neutral-600 dark:text-neutral-400">
          This booking is already {booking.status.toLowerCase().replace('_', ' ')}.
        </p>
      ) : expired ? (
        <p className="mt-6 text-red-600">Your seat hold has expired. Please start a new booking.</p>
      ) : (
        <>
          {secondsLeft !== null && (
            <p className="mt-6 text-sm text-neutral-500">
              Seats held for <span className="font-mono font-semibold">{formatCountdown(secondsLeft)}</span>
            </p>
          )}

          {paymentMutation.isError && (
            <p className="mt-2 text-sm text-red-600">Could not start payment. Please try again.</p>
          )}

          <button
            onClick={() => paymentMutation.mutate()}
            disabled={paymentMutation.isPending}
            className="mt-4 w-full rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
          >
            {paymentMutation.isPending ? 'Redirecting to PayHere…' : 'Pay Now'}
          </button>
        </>
      )}
    </main>
  )
}
