import { Link, useSearchParams } from 'react-router-dom'

/** UC-09 cancel path. The seat lock keeps counting down — the customer can retry until it expires. */
export function PaymentCancelPage() {
  const [params] = useSearchParams()
  const bookingId = params.get('bookingId')

  return (
    <main className="mx-4 my-8 max-w-xl rounded-2xl border border-neutral-200 bg-white px-4 py-8 text-center shadow-sm sm:mx-auto sm:px-8">
      <h1 className="text-2xl font-bold">Payment Cancelled</h1>
      <p className="mt-2 text-neutral-600">
        No charge was made. Your seats stay held until the 10-minute lock expires — you can try paying again.
      </p>

      {bookingId && (
        <Link
          to={`/bookings/${bookingId}/confirm`}
          className="mt-6 inline-block rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark"
        >
          Try Payment Again
        </Link>
      )}

      <div className="mt-4">
        <Link to="/" className="text-sm text-brand hover:underline">
          Back to showtimes
        </Link>
      </div>
    </main>
  )
}
