import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { getBooking, qrCodeUrl } from '../../api/bookings'

/**
 * UC-09. PayHere redirects the browser here after checkout, but the booking
 * is only actually confirmed once PayHere's separate server-to-server notify
 * call lands (which can trail slightly behind this redirect) — so this page
 * polls rather than trusting its own arrival as proof of payment.
 */
export function PaymentSuccessPage() {
  const [params] = useSearchParams()
  const bookingId = params.get('bookingId')

  const bookingQuery = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => getBooking(bookingId!),
    enabled: !!bookingId,
    refetchInterval: (query) => (query.state.data?.status === 'CONFIRMED' ? false : 2000),
  })

  if (!bookingId) {
    return <p className="px-6 py-10 text-red-600">Missing booking reference.</p>
  }
  if (bookingQuery.isPending) {
    return <p className="px-6 py-10 text-neutral-500">Checking payment status…</p>
  }
  if (bookingQuery.isError || !bookingQuery.data) {
    return <p className="px-6 py-10 text-red-600">Could not find this booking.</p>
  }

  const booking = bookingQuery.data

  if (booking.status !== 'CONFIRMED') {
    return (
      <main className="mx-auto max-w-xl px-6 py-10 text-center">
        <p className="text-neutral-600 dark:text-neutral-400">
          Waiting for payment confirmation from PayHere — this usually takes a few seconds.
        </p>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-xl px-6 py-10 text-center">
      <h1 className="text-2xl font-semibold text-emerald-600">Booking Confirmed</h1>
      <p className="mt-2 font-mono text-lg">{booking.bookingReference}</p>

      <img
        src={qrCodeUrl(booking.id)}
        alt={`QR code for booking ${booking.bookingReference}`}
        className="mx-auto mt-6 h-56 w-56 rounded-lg border border-neutral-200 dark:border-neutral-800"
      />

      <p className="mt-6 text-neutral-600 dark:text-neutral-400">
        {booking.movieTitle} · Seats: {booking.seats.map((seat) => seat.seatNumber).join(', ')}
      </p>
      <p className="mt-1 text-sm text-neutral-500">Show this QR code at the theatre entrance.</p>

      <Link to="/" className="mt-8 inline-block text-sm text-emerald-600 hover:underline">
        Back to showtimes
      </Link>
    </main>
  )
}
