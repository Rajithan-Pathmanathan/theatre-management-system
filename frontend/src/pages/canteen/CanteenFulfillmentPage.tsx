import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  listCanteenQueue,
  lookupSnackOrderByToken,
  updateSnackOrderItemAvailability,
  updateSnackOrderStatus,
} from '../../api/canteen'
import type { PickupTiming, SnackOrderResponse, SnackOrderStatus } from '../../api/types'
import { formatCurrency, formatTime, PICKUP_LABELS, todayIso } from '../../lib/format'

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const ACTIVE: SnackOrderStatus[] = ['PLACED', 'READY']

const TABS: { label: string; timing: PickupTiming | null }[] = [
  { label: 'All', timing: null },
  { label: 'Before movie', timing: 'BEFORE_MOVIE' },
  { label: 'Interval', timing: 'DURING_INTERVAL' },
]

const STATUS_CLASS: Record<SnackOrderStatus, string> = {
  PLACED: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  READY: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200',
  COLLECTED: 'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300',
  CLOSED: 'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300',
}

/**
 * UC-19, BR-16, FR-64. The queue polls so new orders appear without a reload. Nothing expires
 * on its own: staff close uncollected orders by hand. The search box takes an order number,
 * or the token from a scanned pickup QR.
 */
export function CanteenFulfillmentPage() {
  const queryClient = useQueryClient()
  const [date, setDate] = useState(todayIso())
  const [timing, setTiming] = useState<PickupTiming | null>(null)
  const [showFinished, setShowFinished] = useState(false)
  const [search, setSearch] = useState('')

  const queueKey = ['canteen-queue', date, timing]
  const queueQuery = useQuery({ queryKey: queueKey, queryFn: () => listCanteenQueue(date, timing), refetchInterval: 15_000 })

  const scanQuery = useQuery({
    queryKey: ['canteen-scan', search.trim()],
    queryFn: () => lookupSnackOrderByToken(search.trim()),
    enabled: UUID_PATTERN.test(search.trim()),
    retry: false,
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['canteen-queue'] })
    queryClient.invalidateQueries({ queryKey: ['canteen-scan'] })
  }

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: SnackOrderStatus }) => updateSnackOrderStatus(id, status),
    onSuccess: refresh,
  })

  const itemMutation = useMutation({
    mutationFn: ({ orderId, itemId, unavailable }: { orderId: string; itemId: string; unavailable: boolean }) =>
      updateSnackOrderItemAvailability(orderId, itemId, unavailable),
    onSuccess: refresh,
  })

  const term = search.trim()
  let orders: SnackOrderResponse[]
  if (UUID_PATTERN.test(term)) {
    orders = scanQuery.data ? [scanQuery.data] : []
  } else {
    orders = (queueQuery.data ?? [])
      .filter((order) => showFinished || ACTIVE.includes(order.status))
      .filter((order) => !term || String(order.orderNumber) === term.replace('#', ''))
  }

  const error = statusMutation.error ?? itemMutation.error

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Canteen Orders</h1>

      <div className="mt-6 flex flex-wrap items-end gap-4">
        <label className="text-sm font-medium">
          Date
          <input
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
            className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>
        <label className="flex-1 text-sm font-medium">
          Order number or scanned QR
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="e.g. 42"
            className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 font-mono dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>
        <label className="flex items-center gap-2 pb-2 text-sm">
          <input type="checkbox" checked={showFinished} onChange={(event) => setShowFinished(event.target.checked)} />
          Show collected / closed
        </label>
      </div>

      <div className="mt-4 flex gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            onClick={() => setTiming(tab.timing)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium ${
              timing === tab.timing ? 'bg-emerald-600 text-white' : 'border border-neutral-300 dark:border-neutral-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <p className="mt-4 text-sm text-red-600">
          {(error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Could not update this order.'}
        </p>
      )}
      {scanQuery.isError && <p className="mt-4 text-sm text-red-600">No order found for this QR code.</p>}
      {queueQuery.isPending && <p className="mt-6 text-neutral-500">Loading…</p>}
      {queueQuery.isSuccess && orders.length === 0 && !scanQuery.isFetching && <p className="mt-6 text-neutral-500">No orders to show.</p>}

      <div className="mt-6 grid gap-3 md:grid-cols-2">
        {orders.map((order) => {
          const active = ACTIVE.includes(order.status)
          return (
            <div key={order.id} className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-2xl font-bold">#{order.orderNumber}</p>
                  <p className="text-sm text-neutral-500">
                    {order.customerName} · {order.customerPhone}
                  </p>
                </div>
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[order.status]}`}>{order.status}</span>
              </div>
              <p className="mt-2 text-sm">
                {order.movieTitle} · {formatTime(order.startTime)} · {PICKUP_LABELS[order.pickupTiming]}
                {order.linkedBookingReference ? ` · ${order.linkedBookingReference}` : ''}
              </p>

              <ul className="mt-3 flex flex-col gap-1 text-sm">
                {order.items.map((item) => (
                  <li key={item.id} className="flex items-center justify-between">
                    <span className={item.unavailable ? 'text-neutral-400 line-through' : ''}>
                      {item.quantity} × {item.itemName}
                    </span>
                    {active && (
                      <button
                        type="button"
                        onClick={() => itemMutation.mutate({ orderId: order.id, itemId: item.id, unavailable: !item.unavailable })}
                        className="text-xs text-neutral-500 hover:underline"
                      >
                        {item.unavailable ? 'Back in stock' : 'Out of stock'}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
              <p className="mt-3 flex justify-between border-t border-neutral-200 pt-2 font-semibold dark:border-neutral-800">
                <span>To collect</span>
                <span>{formatCurrency(order.totalAmount)}</span>
              </p>

              {active && (
                <div className="mt-3 flex gap-2">
                  {order.status === 'PLACED' && (
                    <button
                      type="button"
                      onClick={() => statusMutation.mutate({ id: order.id, status: 'READY' })}
                      className="rounded-md border border-neutral-300 px-3 py-1.5 text-sm font-medium dark:border-neutral-700"
                    >
                      Mark ready
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => statusMutation.mutate({ id: order.id, status: 'COLLECTED' })}
                    className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700"
                  >
                    Collected
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Close order #${order.orderNumber} without collection?`)) {
                        statusMutation.mutate({ id: order.id, status: 'CLOSED' })
                      }
                    }}
                    className="ml-auto text-sm text-neutral-500 hover:underline"
                  >
                    Close
                  </button>
                </div>
              )}
            </div>
          )
        })}
      </div>
    </main>
  )
}
