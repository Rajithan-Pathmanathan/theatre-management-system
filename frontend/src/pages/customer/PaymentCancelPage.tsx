import { Link, useSearchParams } from 'react-router-dom'

/** UC-09 cancel path. The seat lock keeps counting down — the customer can retry until it expires. */
export function PaymentCancelPage() {
  const [params] = useSearchParams()
  const bookingId = params.get('bookingId')

  return (
    <main className="mx-auto max-w-xl px-6 py-10 text-center">
      <h1 className="text-2xl font-semibold">Payment Cancelled</h1>
      <p className="mt-2 text-neutral-600 dark:text-neutral-400">
        No charge was made. Your seats stay held until the 10-minute lock expires — you can try paying again.
      </p>

      {bookingId && (
        <Link
          to={`/bookings/${bookingId}/confirm`}
          className="mt-6 inline-block rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700"
        >
          Try Payment Again
        </Link>
      )}

      <div className="mt-4">
        <Link to="/" className="text-sm text-emerald-600 hover:underline">
          Back to showtimes
        </Link>
      </div>
    </main>
  )
}
