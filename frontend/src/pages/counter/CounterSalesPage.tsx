import { useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { getCounterSeats, listCounterShows, sellCounterTicket } from '../../api/counter'
import type { PhysicalPaymentMethod, PublicSeatResponse } from '../../api/types'
import { formatCurrency, formatTime, todayIso } from '../../lib/format'

function seatClasses(seat: PublicSeatResponse, isSelected: boolean): string {
  if (seat.status !== 'AVAILABLE') {
    return 'cursor-not-allowed border-neutral-200 bg-neutral-100 text-neutral-400'
  }
  if (isSelected) return 'border-brand bg-brand text-white'
  return 'border-neutral-300 bg-white hover:border-brand'
}

/** UC-10, BR-07, BR-17. A counter sale settles on the spot — CONFIRMED immediately, no payment step. */
export function CounterSalesPage() {
  const [date, setDate] = useState(todayIso())
  const [showId, setShowId] = useState<string | null>(null)
  const [adultCount, setAdultCount] = useState(1)
  const [childCount, setChildCount] = useState(0)
  const [selectedSeatIds, setSelectedSeatIds] = useState<string[]>([])
  const [paymentMethod, setPaymentMethod] = useState<PhysicalPaymentMethod>('CASH')
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const showsQuery = useQuery({ queryKey: ['counter-shows', date], queryFn: () => listCounterShows(date) })
  const scheduledShows = (showsQuery.data ?? []).filter((show) => show.status === 'SCHEDULED')

  const seatsQuery = useQuery({
    queryKey: ['counter-seats', showId],
    queryFn: () => getCounterSeats(showId!),
    enabled: !!showId,
  })

  const requiredSeats = adultCount + childCount
  const selectedShow = scheduledShows.find((show) => show.id === showId)

  const seatsByRow = useMemo(() => {
    const rows = new Map<string, PublicSeatResponse[]>()
    for (const seat of seatsQuery.data ?? []) {
      const row = rows.get(seat.rowLabel) ?? []
      row.push(seat)
      rows.set(seat.rowLabel, row)
    }
    for (const row of rows.values()) row.sort((a, b) => a.seatInRow - b.seatInRow)
    return [...rows.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [seatsQuery.data])

  const saleMutation = useMutation({
    mutationFn: sellCounterTicket,
    onSuccess: () => {
      setSelectedSeatIds([])
      setCustomerName('')
      setCustomerPhone('')
      seatsQuery.refetch()
    },
  })

  function selectShow(id: string) {
    setShowId(id)
    setSelectedSeatIds([])
    saleMutation.reset()
  }

  function toggleSeat(seat: PublicSeatResponse) {
    if (seat.status !== 'AVAILABLE') return
    setSelectedSeatIds((current) => {
      if (current.includes(seat.seatId)) return current.filter((id) => id !== seat.seatId)
      if (current.length >= requiredSeats) return current
      return [...current, seat.seatId]
    })
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setFormError(null)
    if (!showId) {
      setFormError('Select a show first.')
      return
    }
    if (selectedSeatIds.length !== requiredSeats) {
      setFormError(`Select exactly ${requiredSeats} seat(s) to match the ticket count.`)
      return
    }
    saleMutation.mutate({
      showId,
      adultCount,
      childCount,
      seatAllocationIds: selectedSeatIds,
      physicalPaymentMethod: paymentMethod,
      customerName: customerName.trim() || null,
      customerPhone: customerPhone.trim() || null,
    })
  }

  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <h1 className="text-2xl font-bold">Counter Sales</h1>

      <label className="mt-6 block text-sm font-medium">
        Date
        <input
          type="date"
          value={date}
          onChange={(event) => {
            setDate(event.target.value)
            setShowId(null)
          }}
          className="mt-1 block rounded-lg border border-neutral-300 px-3 py-2"
        />
      </label>

      {showsQuery.isPending && <p className="mt-4 text-neutral-500">Loading shows…</p>}
      {scheduledShows.length === 0 && !showsQuery.isPending && (
        <p className="mt-4 text-neutral-500">No scheduled shows on this date.</p>
      )}

      <div className="mt-4 flex flex-wrap gap-2">
        {scheduledShows.map((show) => (
          <button
            key={show.id}
            type="button"
            onClick={() => selectShow(show.id)}
            className={`rounded-lg border px-3 py-2 text-left text-sm ${
              show.id === showId
                ? 'border-brand bg-brand text-white'
                : 'border-neutral-300 bg-white hover:border-brand'
            }`}
          >
            <p className="font-medium">{show.movieTitle}</p>
            <p className="opacity-80">{formatTime(show.startTime)}</p>
          </button>
        ))}
      </div>

      {showId && (
        <form onSubmit={handleSubmit} className="mt-8 grid gap-6 md:grid-cols-[2fr_1fr]">
          <div className="min-w-0 rounded-xl border border-neutral-200 bg-white p-4">
            <div className="flex gap-6">
              <label className="text-sm font-medium">
                Adults
                <input
                  type="number"
                  min={0}
                  max={10}
                  value={adultCount}
                  onChange={(event) => setAdultCount(Math.max(0, Number(event.target.value)))}
                  className="mt-1 block w-20 rounded-lg border border-neutral-300 px-2 py-1"
                />
              </label>
              <label className="text-sm font-medium">
                Children
                <input
                  type="number"
                  min={0}
                  max={10}
                  value={childCount}
                  onChange={(event) => setChildCount(Math.max(0, Number(event.target.value)))}
                  className="mt-1 block w-20 rounded-lg border border-neutral-300 px-2 py-1"
                />
              </label>
            </div>

            {seatsQuery.isPending ? (
              <p className="mt-4 text-neutral-500">Loading seat map…</p>
            ) : (
              <>
                <p className="mt-4 text-sm text-neutral-500">
                  Select {requiredSeats} physical seat(s) — {selectedSeatIds.length} selected
                </p>
                <div className="mt-3 flex flex-col gap-2">
                  {seatsByRow.map(([rowLabel, seats]) => (
                    <div key={rowLabel} className="flex items-center gap-2">
                      <span className="w-5 text-xs font-mono text-neutral-400">{rowLabel}</span>
                      <div className="flex flex-wrap gap-1">
                        {seats.map((seat) => (
                          <button
                            key={seat.seatId}
                            type="button"
                            title={seat.seatNumber}
                            disabled={seat.status !== 'AVAILABLE'}
                            onClick={() => toggleSeat(seat)}
                            className={`h-7 w-7 rounded border text-[10px] font-medium ${seatClasses(seat, selectedSeatIds.includes(seat.seatId))}`}
                          >
                            {seat.seatInRow}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>

          <div className="flex flex-col gap-3 self-start rounded-xl border border-neutral-200 bg-white p-4">
            <label className="text-sm font-medium">
              Payment method
              <select
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value as PhysicalPaymentMethod)}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
              >
                <option value="CASH">Cash</option>
                <option value="CARD">Card</option>
                <option value="OTHER">Other</option>
              </select>
            </label>
            <label className="text-sm font-medium">
              Customer name (optional)
              <input
                value={customerName}
                onChange={(event) => setCustomerName(event.target.value)}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
              />
            </label>
            <label className="text-sm font-medium">
              Customer phone (optional)
              <input
                value={customerPhone}
                onChange={(event) => setCustomerPhone(event.target.value)}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
              />
            </label>

            {selectedShow && (
              <div className="mt-2 rounded-xl border border-neutral-200 bg-white p-4">
                <p className="flex justify-between text-sm">
                  <span>Total</span>
                  <span className="font-semibold">
                    {formatCurrency(adultCount * selectedShow.adultPrice + childCount * selectedShow.childPrice)}
                  </span>
                </p>
              </div>
            )}

            {formError && <p className="text-sm text-red-600">{formError}</p>}
            {saleMutation.isError && <p className="text-sm text-red-600">Could not complete the sale. Please try again.</p>}
            {saleMutation.isSuccess && (
              <p className="text-sm text-brand">
                Sold — {saleMutation.data.bookingReference} ({saleMutation.data.seats.map((s) => s.seatNumber).join(', ')})
              </p>
            )}

            <button
              type="submit"
              disabled={saleMutation.isPending}
              className="mt-2 rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
            >
              {saleMutation.isPending ? 'Selling…' : 'Sell Ticket'}
            </button>
          </div>
        </form>
      )}
    </main>
  )
}
