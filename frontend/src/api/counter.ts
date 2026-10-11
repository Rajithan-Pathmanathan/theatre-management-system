import { apiClient } from './client'
import type { BookingResponse, CounterSaleRequest, PublicSeatResponse, ShowResponse } from './types'

/** UC-10 — unlike the public /api/shows listing, this isn't filtered to guaranteed/online-enabled shows. */
export async function listCounterShows(date: string): Promise<ShowResponse[]> {
  const { data } = await apiClient.get<ShowResponse[]>('/api/counter/shows', { params: { date } })
  return data
}

export async function getCounterSeats(showId: string): Promise<PublicSeatResponse[]> {
  const { data } = await apiClient.get<PublicSeatResponse[]>(`/api/counter/shows/${showId}/seats`)
  return data
}

export async function sellCounterTicket(request: CounterSaleRequest): Promise<BookingResponse> {
  const { data } = await apiClient.post<BookingResponse>('/api/counter/sales', request)
  return data
}
