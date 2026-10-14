import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getBookingByReference } from '../../api/bookings'
import { listShowsByDate } from '../../api/shows'
import { createSnackOrder, listAvailableSnacks, snackOrderQrCodeUrl } from '../../api/snacks'
import type { PickupTiming } from '../../api/types'
import { formatCurrency, formatDate, formatTime, PICKUP_LABELS } from '../../lib/format'

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

const INPUT_CLASS = 'mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900'

/**
 * UC-18, BR-08, BR-13–BR-15. No booking or account needed; a booking reference is optional.
 * Pay at collection, so there is no checkout step. A `?showId=` link (e.g. from a theatre QR) preselects the show.
 */
export function SnackOrderPage() {
  const [searchParams] = useSearchParams()
  const [date, setDate] = useState(todayIso())
  const [showId, setShowId] = useState(searchParams.get('showId') ?? '')
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [bookingReference, setBookingReference] = useState('')
  const [pickupTiming, setPickupTiming] = useState<PickupTiming>('BEFORE_MOVIE')

  const showsQuery = useQuery({ queryKey: ['shows', date], queryFn: () => listShowsByDate(date) })
  const snacksQuery = useQuery({ queryKey: ['snacks'], queryFn: listAvailableSnacks })

  const selectedShow = showsQuery.data?.find((show) => show.id === showId)
  const hasInterval = selectedShow?.hasInterval ?? false
  const effectivePickup: PickupTiming = hasInterval ? pickupTiming : 'BEFORE_MOVIE' // BR-14

  const lines = (snacksQuery.data ?? []).filter((snack) => (quantities[snack.id] ?? 0) > 0)
  const total = lines.reduce((sum, snack) => sum + snack.price * quantities[snack.id], 0)

  const orderMutation = useMutation({
    mutationFn: async () => {
      const reference = bookingReference.trim()
      const bookingId = reference ? (await getBookingByReference(reference)).id : null
      return createSnackOrder({
        bookingId,
        showId: bookingId ? null : showId,
        customerName,
        customerPhone,
        pickupTiming: effectivePickup,
        items: lines.map((snack) => ({ snackItemId: snack.id, quantity: quantities[snack.id] })),
      })
    },
  })

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    orderMutation.mutate()
  }

  if (orderMutation.data) {
    const order = orderMutation.data
    return (
      <main className="mx-auto max-w-xl px-6 py-10 text-center">
        <h1 className="text-2xl font-semibold">Order placed</h1>
        <p className="mt-4 text-neutral-500">Your order number</p>
        <p className="text-5xl font-bold">#{order.orderNumber}</p>
        <img src={snackOrderQrCodeUrl(order.id)} alt="Snack order QR code" className="mx-auto mt-6 h-48 w-48" />
        <p className="mt-4">
          {order.movieTitle} · {formatDate(order.showDate)} · {formatTime(order.startTime)}
        </p>
        <p className="mt-1 text-neutral-600 dark:text-neutral-400">Pick up {PICKUP_LABELS[order.pickupTiming].toLowerCase()}</p>
        <p className="mt-4 text-xl font-semibold">{formatCurrency(order.totalAmount)}</p>
        <p className="text-sm text-neutral-500">Pay at the canteen when you collect. Show this QR code or your order number.</p>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Order Snacks</h1>
      <p className="mt-1 text-neutral-600 dark:text-neutral-400">No ticket booking needed. Pay when you collect.</p>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <div className="grid grid-cols-[auto_1fr] gap-3">
          <label className="text-sm font-medium">
            Date
            <input type="date" value={date} onChange={(event) => setDate(event.target.value)} className={INPUT_CLASS} />
          </label>
          <label className="text-sm font-medium">
            Show
            <select required value={showId} onChange={(event) => setShowId(event.target.value)} className={INPUT_CLASS}>
              <option value="">Select a show</option>
              {showsQuery.data?.map((show) => (
                <option key={show.id} value={show.id}>
                  {show.movieTitle} · {formatTime(show.startTime)}
                </option>
              ))}
            </select>
          </label>
        </div>
        {showsQuery.data?.length === 0 && <p className="text-sm text-neutral-500">No shows on this date.</p>}

        <fieldset>
          <legend className="text-sm font-medium">Snacks</legend>
          {snacksQuery.data?.length === 0 && <p className="mt-2 text-sm text-neutral-500">The canteen menu is empty right now.</p>}
          <div className="mt-2 flex flex-col gap-2">
            {snacksQuery.data?.map((snack) => (
              <label key={snack.id} className="flex items-center justify-between rounded-lg border border-neutral-200 px-4 py-2 dark:border-neutral-800">
                <span>
                  {snack.name} <span className="text-neutral-500">· {formatCurrency(snack.price)}</span>
                </span>
                <input
                  type="number"
                  min="0"
                  max="20"
                  value={quantities[snack.id] ?? 0}
                  onChange={(event) => setQuantities({ ...quantities, [snack.id]: Number(event.target.value) })}
                  aria-label={`${snack.name} quantity`}
                  className="w-16 rounded-md border border-neutral-300 px-2 py-1 text-right dark:border-neutral-700 dark:bg-neutral-900"
                />
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend className="text-sm font-medium">Pickup</legend>
          <div className="mt-2 flex flex-col gap-1">
            {(Object.keys(PICKUP_LABELS) as PickupTiming[])
              .filter((timing) => hasInterval || timing === 'BEFORE_MOVIE')
              .map((timing) => (
                <label key={timing} className="flex items-center gap-2">
                  <input type="radio" name="pickup" checked={effectivePickup === timing} onChange={() => setPickupTiming(timing)} />
                  {PICKUP_LABELS[timing]}
                </label>
              ))}
          </div>
          {selectedShow && !hasInterval && <p className="mt-1 text-sm text-neutral-500">This show has no interval.</p>}
        </fieldset>

        <label className="text-sm font-medium">
          Name
          <input required value={customerName} onChange={(event) => setCustomerName(event.target.value)} className={INPUT_CLASS} />
        </label>
        <label className="text-sm font-medium">
          Phone
          <input
            required
            type="tel"
            value={customerPhone}
            onChange={(event) => setCustomerPhone(event.target.value)}
            placeholder="+94771234567"
            className={INPUT_CLASS}
          />
        </label>
        <label className="text-sm font-medium">
          Booking reference (optional)
          <input
            value={bookingReference}
            onChange={(event) => setBookingReference(event.target.value)}
            placeholder="VJY-20261201-ABC123"
            className={`${INPUT_CLASS} font-mono`}
          />
        </label>

        {orderMutation.isError && (
          <p className="text-sm text-red-600">
            {(orderMutation.error as { response?: { status?: number; data?: { message?: string } } })?.response?.data?.message ??
              'Could not place this order.'}
          </p>
        )}

        <div className="flex items-center justify-between border-t border-neutral-200 pt-4 dark:border-neutral-800">
          <p className="text-lg font-semibold">{formatCurrency(total)}</p>
          <button
            type="submit"
            disabled={lines.length === 0 || !showId || orderMutation.isPending}
            className="rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
          >
            {orderMutation.isPending ? 'Placing order…' : 'Place order'}
          </button>
        </div>
      </form>
    </main>
  )
}
