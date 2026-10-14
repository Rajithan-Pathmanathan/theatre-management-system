import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getSnackOrder } from '../../api/snacks'
import type { SnackOrderResponse } from '../../api/types'
import { SnackOrderTrackingPage } from './SnackOrderTrackingPage'

vi.mock('../../api/snacks', () => ({ getSnackOrder: vi.fn(), snackOrderQrCodeUrl: () => 'qr.png' }))

function order(overrides: Partial<SnackOrderResponse>): SnackOrderResponse {
  return {
    id: 'order-1',
    orderDate: '2026-10-15',
    orderNumber: 7,
    showId: 'show-1',
    movieTitle: 'Test Movie',
    showDate: '2026-10-15',
    startTime: '18:30:00',
    customerName: 'Test Customer',
    customerPhone: '+94770000000',
    pickupTiming: 'BEFORE_MOVIE',
    status: 'PLACED',
    totalAmount: 1700,
    items: [
      { id: 'i1', itemName: 'Popcorn', unitPrice: 850, quantity: 2, unavailable: false },
      { id: 'i2', itemName: 'Coke', unitPrice: 300, quantity: 1, unavailable: true },
    ],
    linkedBookingReference: null,
    ...overrides,
  }
}

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/snacks/orders/order-1']}>
        <Routes>
          <Route path="/snacks/orders/:orderId" element={<SnackOrderTrackingPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

/** FR-64 — the customer must be told when the canteen runs out of something they ordered. */
describe('SnackOrderTrackingPage', () => {
  afterEach(cleanup)

  it('tells the customer which items ran out and the adjusted total', async () => {
    vi.mocked(getSnackOrder).mockResolvedValue(order({}))
    renderPage()

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Coke')
    expect(alert.textContent).not.toContain('Popcorn')
    expect(alert.textContent).toMatch(/1,700\.00/)
  })

  it('shows no notice when everything is in stock', async () => {
    vi.mocked(getSnackOrder).mockResolvedValue(order({ items: [{ id: 'i1', itemName: 'Popcorn', unitPrice: 850, quantity: 2, unavailable: false }] }))
    renderPage()

    await screen.findByText('#7')
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
