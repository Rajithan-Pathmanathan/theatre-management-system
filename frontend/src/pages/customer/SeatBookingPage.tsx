import { useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createBooking } from '../../api/bookings'
import { getShow, getShowSeats } from '../../api/shows'
import type { PublicSeatResponse } from '../../api/types'
import { formatCurrency, formatDate, formatTime } from '../../lib/format'

const ZONE_LABEL: Record<PublicSeatResponse['zone'], string> = {
  FRONT: 'Front',
  MIDDLE: 'Middle',
  BACK: 'Back',
  SIDE: 'Side',
}

function seatClasses(seat: PublicSeatResponse, isSelected: boolean): string {
  if (seat.status !== 'AVAILABLE') {
    return 'cursor-not-allowed border-neutral-200 bg-neutral-100 text-neutral-400 dark:border-neutral-800 dark:bg-neutral-900 dark:text-neutral-600'
  }
  if (isSelected) {
    return 'border-emerald-600 bg-emerald-600 text-white'
  }
  return 'border-neutral-300 bg-white hover:border-emerald-500 dark:border-neutral-700 dark:bg-neutral-950'
}

/** UC-08, BR-04 — exactly adultCount + childCount seats must be picked before a lock can be requested. */
export function SeatBookingPage() {
  const { showId } = useParams<{ showId: string }>()
  const navigate = useNavigate()

  const [adultCount, setAdultCount] = useState(1)
  const [childCount, setChildCount] = useState(0)
  const [selectedSeatIds, setSelectedSeatIds] = useState<string[]>([])
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [customerEmail, setCustomerEmail] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const showQuery = useQuery({ queryKey: ['show', showId], queryFn: () => getShow(showId!), enabled: !!showId })
  const seatsQuery = useQuery({ queryKey: ['seats', showId], queryFn: () => getShowSeats(showId!), enabled: !!showId })

  const requiredSeats = adultCount + childCount

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

  const bookingMutation = useMutation({
    mutationFn: createBooking,
    onSuccess: (booking) => navigate(`/bookings/${booking.id}/confirm`),
  })

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
    if (selectedSeatIds.length !== requiredSeats) {
      setFormError(`Select exactly ${requiredSeats} seat(s) to match your ticket count.`)
      return
    }
    if (!customerName.trim() || !customerPhone.trim()) {
      setFormError('Name and phone number are required.')
      return
    }
    bookingMutation.mutate({
      showId: showId!,
      adultCount,
      childCount,
      seatAllocationIds: selectedSeatIds,
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      customerEmail: customerEmail.trim() || null,
    })
  }

  if (showQuery.isPending || seatsQuery.isPending) {
    return <p className="px-6 py-10 text-neutral-500">Loading seat map…</p>
  }
  if (showQuery.isError || !showQuery.data) {
    return <p className="px-6 py-10 text-red-600">This show is not available for booking.</p>
  }

  const show = showQuery.data
  const total = adultCount * show.adultPrice + childCount * show.childPrice

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">{show.movieTitle}</h1>
      <p className="text-neutral-600 dark:text-neutral-400">
        {formatDate(show.showDate)} · {formatTime(show.startTime)}
      </p>

      <form onSubmit={handleSubmit} className="mt-8 grid gap-8 md:grid-cols-[2fr_1fr]">
        <div>
          <div className="flex gap-6">
            <label className="text-sm font-medium">
              Adults
              <input
                type="number"
                min={0}
                max={10}
                value={adultCount}
                onChange={(event) => setAdultCount(Math.max(0, Number(event.target.value)))}
                className="mt-1 block w-20 rounded-md border border-neutral-300 px-2 py-1 dark:border-neutral-700 dark:bg-neutral-900"
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
                className="mt-1 block w-20 rounded-md border border-neutral-300 px-2 py-1 dark:border-neutral-700 dark:bg-neutral-900"
              />
            </label>
          </div>

          <p className="mt-4 text-sm text-neutral-500">
            Select {requiredSeats} seat(s) — {selectedSeatIds.length} selected
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
                      title={`${seat.seatNumber} · ${ZONE_LABEL[seat.zone]}`}
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
        </div>

        <div className="flex flex-col gap-3">
          <label className="text-sm font-medium">
            Name
            <input
              value={customerName}
              onChange={(event) => setCustomerName(event.target.value)}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Phone
            <input
              value={customerPhone}
              onChange={(event) => setCustomerPhone(event.target.value)}
              placeholder="+94"
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Email (optional)
            <input
              type="email"
              value={customerEmail}
              onChange={(event) => setCustomerEmail(event.target.value)}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>

          <div className="mt-2 rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
            <p className="flex justify-between text-sm">
              <span>Total</span>
              <span className="font-semibold">{formatCurrency(total)}</span>
            </p>
          </div>

          {formError && <p className="text-sm text-red-600">{formError}</p>}
          {bookingMutation.isError && (
            <p className="text-sm text-red-600">
              {(bookingMutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                'Could not create the booking. Please try again.'}
            </p>
          )}

          <button
            type="submit"
            disabled={bookingMutation.isPending}
            className="mt-2 rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
          >
            {bookingMutation.isPending ? 'Locking seats…' : 'Continue to Payment'}
          </button>
        </div>
      </form>
    </main>
  )
}
