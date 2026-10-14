import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createBooking } from '../../api/bookings'
import type { PublicSeatResponse, PublicShowResponse } from '../../api/types'
import { SeatBookingPage } from './SeatBookingPage'

vi.mock('../../api/shows', () => ({
  getShow: vi.fn(async (): Promise<PublicShowResponse> => ({
    id: 'show-1',
    movieId: 'movie-1',
    movieTitle: 'Test Movie',
    showDate: '2026-12-01',
    startTime: '18:30:00',
    adultPrice: 500,
    childPrice: 300,
    hasInterval: true,
    onlineSeatsRemaining: 3,
  })),
  getShowSeats: vi.fn(async (): Promise<PublicSeatResponse[]> => [
    { seatId: 'a1', seatNumber: 'A1', rowLabel: 'A', seatInRow: 1, zone: 'FRONT', status: 'AVAILABLE' },
    { seatId: 'a2', seatNumber: 'A2', rowLabel: 'A', seatInRow: 2, zone: 'FRONT', status: 'AVAILABLE' },
    { seatId: 'a3', seatNumber: 'A3', rowLabel: 'A', seatInRow: 3, zone: 'FRONT', status: 'BOOKED' },
  ]),
}))

vi.mock('../../api/bookings', () => ({ createBooking: vi.fn() }))

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/shows/show-1']}>
        <Routes>
          <Route path="/shows/:showId" element={<SeatBookingPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const seat = (number: string) => screen.findByTitle(new RegExp(`^${number} ·`))

/** UC-08, BR-04 — the seat map is where a UI state bug would actually cost a sale. */
describe('SeatBookingPage', () => {
  beforeEach(() => vi.mocked(createBooking).mockReset())
  afterEach(cleanup)

  it('caps the selection at the ticket count', async () => {
    renderPage()
    await userEvent.click(await seat('A1'))
    await userEvent.click(await seat('A2'))

    expect(screen.getByText(/1 selected/)).toBeTruthy()
  })

  it('does not allow selecting a booked seat', async () => {
    renderPage()
    const booked = await seat('A3')

    expect((booked as HTMLButtonElement).disabled).toBe(true)
  })

  it('prices adults and children separately in the total', async () => {
    renderPage()
    const children = await screen.findByLabelText('Children')
    await userEvent.clear(children)
    await userEvent.type(children, '1')

    // 1 adult × 500 + 1 child × 300
    expect(screen.getByText(/800\.00/)).toBeTruthy()
  })

  it('blocks submission until the seat count matches the tickets', async () => {
    renderPage()
    await userEvent.type(await screen.findByLabelText('Name'), 'Test Customer')
    await userEvent.type(screen.getByLabelText('Phone'), '+94770000000')
    await userEvent.click(screen.getByRole('button', { name: 'Continue to Payment' }))

    expect(screen.getByText(/Select exactly 1 seat/)).toBeTruthy()
    expect(createBooking).not.toHaveBeenCalled()
  })

  it('sends the selected seats once the count matches', async () => {
    vi.mocked(createBooking).mockResolvedValue({ id: 'booking-1' } as Awaited<ReturnType<typeof createBooking>>)
    renderPage()
    await userEvent.click(await seat('A2'))
    await userEvent.type(screen.getByLabelText('Name'), 'Test Customer')
    await userEvent.type(screen.getByLabelText('Phone'), '+94770000000')
    await userEvent.click(screen.getByRole('button', { name: 'Continue to Payment' }))

    expect(vi.mocked(createBooking).mock.calls[0][0]).toMatchObject({
      showId: 'show-1',
      adultCount: 1,
      childCount: 0,
      seatAllocationIds: ['a2'],
    })
  })
})
