import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { getSnackOrder, snackOrderQrCodeUrl } from '../../api/snacks'
import type { SnackOrderStatus } from '../../api/types'
import { formatCurrency, formatDate, formatTime, PICKUP_LABELS } from '../../lib/format'

const STATUS_TEXT: Record<SnackOrderStatus, string> = {
  AWAITING_PAYMENT: 'Waiting for your booking payment.',
  PLACED: 'Order received. The canteen is preparing it.',
  READY: 'Ready for pickup.',
  COLLECTED: 'Collected. Enjoy the movie!',
  CLOSED: 'This order was closed by the canteen.',
}

/**
 * UC-18, FR-64. The customer's own view of their order, at a link they can keep. It polls while the
 * order is active, so if the canteen runs out of an item the customer sees it, with the adjusted total.
 */
export function SnackOrderTrackingPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const orderQuery = useQuery({
    queryKey: ['snack-order', orderId],
    queryFn: () => getSnackOrder(orderId!),
    enabled: !!orderId,
    refetchInterval: (query) => (['PLACED', 'READY'].includes(query.state.data?.status ?? 'PLACED') ? 15_000 : false),
  })

  if (orderQuery.isPending) return <p className="px-6 py-10 text-neutral-500">Loading your order…</p>
  if (orderQuery.isError) return <p className="px-6 py-10 text-red-600">We couldn't find this snack order.</p>

  const order = orderQuery.data
  const unavailable = order.items.filter((item) => item.unavailable)
  const active = order.status === 'PLACED' || order.status === 'READY'

  return (
    <main className="mx-4 my-8 max-w-xl rounded-2xl border border-neutral-200 bg-white px-4 py-8 text-center shadow-sm sm:mx-auto sm:px-8">
      <p className="text-neutral-500">Your order number</p>
      <p className="text-5xl font-bold">#{order.orderNumber}</p>
      <p className="mt-2 font-medium">{STATUS_TEXT[order.status]}</p>

      {unavailable.length > 0 && active && (
        <div role="alert" className="mt-6 rounded-lg border border-amber-300 bg-amber-50 p-4 text-left text-amber-900">
          <p className="font-medium">Sorry, the canteen has run out of:</p>
          <ul className="mt-1 list-inside list-disc">
            {unavailable.map((item) => (
              <li key={item.id}>{item.itemName}</li>
            ))}
          </ul>
          <p className="mt-2 text-sm">
            {order.prepaidAmount !== null
              ? `The canteen will refund ${formatCurrency(order.prepaidAmount - order.totalAmount)} when you collect.`
              : `You won't be charged for these. Your new total is ${formatCurrency(order.totalAmount)}.`}
          </p>
        </div>
      )}

      {active && <img src={snackOrderQrCodeUrl(order.id)} alt="Snack order QR code" className="mx-auto mt-6 h-48 w-48" />}
      <p className="mt-4">
        {order.movieTitle} · {formatDate(order.showDate)} · {formatTime(order.startTime)}
      </p>
      <p className="mt-1 text-neutral-600">Pick up {PICKUP_LABELS[order.pickupTiming].toLowerCase()}</p>

      <ul className="mx-auto mt-4 max-w-xs text-left text-sm">
        {order.items.map((item) => (
          <li key={item.id} className={`flex justify-between ${item.unavailable ? 'text-neutral-400 line-through' : ''}`}>
            <span>
              {item.quantity} × {item.itemName}
            </span>
            <span>{formatCurrency(item.unitPrice * item.quantity)}</span>
          </li>
        ))}
      </ul>
      <p className="mt-4 text-xl font-semibold">{formatCurrency(order.totalAmount)}</p>
      {active && order.prepaidAmount !== null && <p className="text-sm text-neutral-500">Paid online with your tickets.</p>}
      {active && order.prepaidAmount === null && (
        <p className="text-sm text-neutral-500">
          Pay at the canteen when you collect. Show this QR code or your order number. Keep this page open, or bookmark it, to see
          updates.
        </p>
      )}
    </main>
  )
}
