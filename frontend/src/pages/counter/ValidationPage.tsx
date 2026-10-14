import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { checkIn, lookupBookings, type LookupParams } from '../../api/validation'
import type { BookingLookupResponse } from '../../api/types'
import { formatDate, formatTime } from '../../lib/format'

type LookupMode = 'reference' | 'phone'

/** UC-11, BR-11, BR-12. Partial entry: a group can be admitted in stages, not all-or-nothing. */
export function ValidationPage() {
  const [mode, setMode] = useState<LookupMode>('reference')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<BookingLookupResponse[]>([])
  const [ticketsToAdmit, setTicketsToAdmit] = useState<Record<string, number>>({})

  const lookupMutation = useMutation({
    mutationFn: (params: LookupParams) => lookupBookings(params),
    onSuccess: setResults,
  })

  const checkInMutation = useMutation({
    mutationFn: ({ bookingId, tickets }: { bookingId: string; tickets: number }) =>
      checkIn(bookingId, { ticketsToAdmit: tickets }),
    onSuccess: (response) => {
      setResults((current) =>
        current.map((booking) =>
          booking.id === response.bookingId
            ? { ...booking, ticketsUsed: response.ticketsUsed, remainingTickets: response.remainingTickets }
            : booking,
        ),
      )
    },
  })

  function handleSearch(event: React.FormEvent) {
    event.preventDefault()
    if (!query.trim()) return
    setResults([])
    lookupMutation.mutate(mode === 'reference' ? { reference: query.trim() } : { phone: query.trim() })
  }

  function handleAdmit(booking: BookingLookupResponse) {
    const tickets = ticketsToAdmit[booking.id] ?? booking.remainingTickets
    checkInMutation.mutate({ bookingId: booking.id, tickets })
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Validate Booking</h1>

      <form onSubmit={handleSearch} className="mt-6 flex flex-col gap-3">
        <div className="flex gap-4 text-sm">
          <label className="flex items-center gap-1">
            <input type="radio" checked={mode === 'reference'} onChange={() => setMode('reference')} />
            By reference
          </label>
          <label className="flex items-center gap-1">
            <input type="radio" checked={mode === 'phone'} onChange={() => setMode('phone')} />
            By phone
          </label>
        </div>
        <div className="flex gap-2">
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={mode === 'reference' ? 'VJY-20261201-ABC123' : '+94...'}
            className="flex-1 rounded-lg border border-neutral-300 px-3 py-2 font-mono"
          />
          <button
            type="submit"
            disabled={lookupMutation.isPending}
            className="rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
          >
            {lookupMutation.isPending ? 'Searching…' : 'Search'}
          </button>
        </div>
      </form>

      {lookupMutation.isError && <p className="mt-4 text-red-600">Lookup failed. Please try again.</p>}
      {lookupMutation.isSuccess && results.length === 0 && <p className="mt-4 text-neutral-500">No booking found.</p>}

      <div className="mt-6 flex flex-col gap-4">
        {results.map((booking) => {
          const fullyCheckedIn = booking.remainingTickets <= 0
          return (
            <div key={booking.id} className="rounded-xl border border-neutral-200 bg-white p-4">
              <div className="flex items-center justify-between">
                <p className="font-mono font-semibold">{booking.bookingReference}</p>
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                    booking.status === 'CONFIRMED'
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-neutral-100 text-neutral-600'
                  }`}
                >
                  {booking.status}
                </span>
              </div>
              <p className="mt-1 text-sm text-neutral-600">
                {booking.movieTitle} · {formatDate(booking.showDate)} · {formatTime(booking.startTime)}
              </p>
              <p className="mt-1 text-sm">{booking.customerName}</p>
              <p className="mt-1 text-sm">Seats: {booking.seats.map((seat) => seat.seatNumber).join(', ')}</p>
              <p className="mt-2 text-sm">
                Admitted {booking.ticketsUsed} of {booking.adultCount + booking.childCount} —{' '}
                <span className="font-medium">{booking.remainingTickets} remaining</span>
              </p>

              {booking.status !== 'CONFIRMED' ? (
                <p className="mt-3 text-sm text-red-600">This booking cannot be validated (not confirmed).</p>
              ) : fullyCheckedIn ? (
                <p className="mt-3 text-sm text-neutral-500">Fully checked in.</p>
              ) : (
                <div className="mt-3 flex items-center gap-2">
                  <input
                    type="number"
                    min={1}
                    max={booking.remainingTickets}
                    value={ticketsToAdmit[booking.id] ?? booking.remainingTickets}
                    onChange={(event) =>
                      setTicketsToAdmit((current) => ({ ...current, [booking.id]: Number(event.target.value) }))
                    }
                    className="w-20 rounded-lg border border-neutral-300 px-2 py-1"
                  />
                  <button
                    type="button"
                    onClick={() => handleAdmit(booking)}
                    disabled={checkInMutation.isPending}
                    className="rounded-lg bg-brand px-3 py-1.5 text-sm font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
                  >
                    Admit
                  </button>
                </div>
              )}
              {checkInMutation.isError && checkInMutation.variables?.bookingId === booking.id && (
                <p className="mt-2 text-sm text-red-600">Could not admit — please recheck the ticket count.</p>
              )}
            </div>
          )
        })}
      </div>
    </main>
  )
}
