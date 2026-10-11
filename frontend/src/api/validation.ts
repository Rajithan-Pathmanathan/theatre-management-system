import { apiClient } from './client'
import type { BookingLookupResponse, CheckInRequest, CheckInResponse } from './types'

export type LookupParams = { reference: string } | { qrToken: string } | { phone: string }

export async function lookupBookings(params: LookupParams): Promise<BookingLookupResponse[]> {
  const { data } = await apiClient.get<BookingLookupResponse[]>('/api/validation/lookup', { params })
  return data
}

export async function checkIn(bookingId: string, request: CheckInRequest): Promise<CheckInResponse> {
  const { data } = await apiClient.post<CheckInResponse>(`/api/validation/bookings/${bookingId}/check-in`, request)
  return data
}
