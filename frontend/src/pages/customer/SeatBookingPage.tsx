import { useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createBooking } from '../../api/bookings'
import { getShow, getShowSeats } from '../../api/shows'
import type { PublicSeatResponse } from '../../api/types'
import { formatCurrency, formatDate, formatTime } from '../../lib/format'

const INPUT_CLASS = 'mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2'
const COUNT_CLASS = 'mt-1 block w-20 rounded-lg border border-neutral-300 px-2 py-1.5'

const LEGEND = [
  { label: 'Available', className: 'border-emerald-500 bg-white' },
  { label: 'Selected', className: 'border-emerald-500 bg-emerald-500' },
  { label: 'Booked', className: 'border-neutral-200 bg-neutral-200' },
]

const ZONE_LABEL: Record<PublicSeatResponse['zone'], string> = {
  FRONT: 'Front',
  MIDDLE: 'Middle',
  BACK: 'Back',
  SIDE: 'Side',
}

function seatClasses(seat: PublicSeatResponse, isSelected: boolean): string {
  if (seat.status !== 'AVAILABLE') {
    return 'cursor-not-allowed border-neutral-200 bg-neutral-200 text-neutral-400'
  }
  if (isSelected) {
    return 'border-emerald-500 bg-emerald-500 text-white'
  }
  return 'border-emerald-500 bg-white text-emerald-700 hover:bg-emerald-500 hover:text-white'
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
    <main className="pb-28">
      <div className="border-b border-neutral-200 bg-white">
        <div className="mx-auto max-w-5xl px-4 py-4 sm:px-6">
          <Link to={`/movies/${show.movieId}`} className="text-sm text-neutral-500 hover:text-brand">
            ‹ Back to showtimes
          </Link>
          <h1 className="mt-1 text-xl font-bold sm:text-2xl">{show.movieTitle}</h1>
          <p className="text-sm text-neutral-600">
            Vijaya Theatre, Batticaloa · {formatDate(show.showDate)} · {formatTime(show.startTime)}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="mx-auto mt-6 grid max-w-5xl gap-6 px-4 sm:px-6 md:grid-cols-[2fr_1fr]">
        <div className="flex min-w-0 flex-col gap-6">
          <section className="rounded-xl border border-neutral-200 bg-white p-4">
            <h2 className="font-semibold">How many tickets?</h2>
            <div className="mt-3 flex gap-6">
              <div>
                <label className="text-sm font-medium">
                  Adults
                  <input
                    type="number"
                    min={0}
                    max={10}
                    value={adultCount}
                    onChange={(event) => setAdultCount(Math.max(0, Number(event.target.value)))}
                    className={COUNT_CLASS}
                  />
                </label>
                <p className="mt-1 text-xs text-neutral-500">{formatCurrency(show.adultPrice)} each</p>
              </div>
              <div>
                <label className="text-sm font-medium">
                  Children
                  <input
                    type="number"
                    min={0}
                    max={10}
                    value={childCount}
                    onChange={(event) => setChildCount(Math.max(0, Number(event.target.value)))}
                    className={COUNT_CLASS}
                  />
                </label>
                <p className="mt-1 text-xs text-neutral-500">{formatCurrency(show.childPrice)} each</p>
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-neutral-200 bg-white p-4">
            <h2 className="font-semibold">Pick {requiredSeats} seat(s)</h2>
            <div className="mt-4 overflow-x-auto pb-2">
              <div className="mx-auto flex w-max flex-col gap-2">
                {seatsByRow.map(([rowLabel, seats]) => (
                  <div key={rowLabel} className="flex items-center gap-2">
                    <span className="w-5 text-xs font-mono text-neutral-400">{rowLabel}</span>
                    <div className="flex gap-1.5">
                      {seats.map((seat) => (
                        <button
                          key={seat.seatId}
                          type="button"
                          title={`${seat.seatNumber} · ${ZONE_LABEL[seat.zone]}`}
                          disabled={seat.status !== 'AVAILABLE'}
                          aria-pressed={selectedSeatIds.includes(seat.seatId)}
                          onClick={() => toggleSeat(seat)}
                          className={`h-7 w-7 rounded-md border text-[10px] font-medium transition ${seatClasses(seat, selectedSeatIds.includes(seat.seatId))}`}
                        >
                          {seat.seatInRow}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
                <div className="mt-6 ml-7">
                  <div className="h-1.5 rounded-full bg-gradient-to-r from-transparent via-neutral-400 to-transparent" />
                  <p className="mt-1 text-center text-xs tracking-widest text-neutral-500 uppercase">Screen this way</p>
                </div>
              </div>
            </div>
            <ul className="mt-4 flex flex-wrap justify-center gap-5 text-xs text-neutral-600">
              {LEGEND.map((item) => (
                <li key={item.label} className="flex items-center gap-1.5">
                  <span className={`inline-block h-4 w-4 rounded border ${item.className}`} />
                  {item.label}
                </li>
              ))}
            </ul>
          </section>
        </div>

        <section className="flex flex-col gap-3 self-start rounded-xl border border-neutral-200 bg-white p-4">
          <h2 className="font-semibold">Your details</h2>
          <label className="text-sm font-medium">
            Name
            <input value={customerName} onChange={(event) => setCustomerName(event.target.value)} className={INPUT_CLASS} />
          </label>
          <label className="text-sm font-medium">
            Phone
            <input value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} placeholder="+94" className={INPUT_CLASS} />
          </label>
          <label className="text-sm font-medium">
            Email (optional)
            <input type="email" value={customerEmail} onChange={(event) => setCustomerEmail(event.target.value)} className={INPUT_CLASS} />
          </label>

          {formError && <p className="text-sm text-red-600">{formError}</p>}
          {bookingMutation.isError && (
            <p className="text-sm text-red-600">
              {(bookingMutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                'Could not create the booking. Please try again.'}
            </p>
          )}
        </section>

        <div className="fixed inset-x-0 bottom-0 z-10 border-t border-neutral-200 bg-white shadow-[0_-4px_12px_rgba(0,0,0,0.06)]">
          <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
            <div>
              <p className="text-lg font-bold">{formatCurrency(total)}</p>
              <p className="text-xs text-neutral-500">
                {requiredSeats} ticket(s) · {selectedSeatIds.length} selected
              </p>
            </div>
            <button
              type="submit"
              disabled={bookingMutation.isPending}
              className="rounded-lg bg-brand px-6 py-2.5 font-semibold text-white transition hover:bg-brand-dark disabled:opacity-50"
            >
              {bookingMutation.isPending ? 'Locking seats…' : 'Continue to Payment'}
            </button>
          </div>
        </div>
      </form>
    </main>
  )
}
