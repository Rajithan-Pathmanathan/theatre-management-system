import { apiClient } from './client'
import type { BookingRequest, BookingResponse } from './types'

export async function createBooking(request: BookingRequest): Promise<BookingResponse> {
  const { data } = await apiClient.post<BookingResponse>('/api/bookings', request)
  return data
}

export async function getBooking(bookingId: string): Promise<BookingResponse> {
  const { data } = await apiClient.get<BookingResponse>(`/api/bookings/${bookingId}`)
  return data
}

export async function getBookingByReference(reference: string): Promise<BookingResponse> {
  const { data } = await apiClient.get<BookingResponse>(`/api/bookings/by-reference/${encodeURIComponent(reference)}`)
  return data
}

export function qrCodeUrl(bookingId: string): string {
  return `${apiClient.defaults.baseURL}/api/bookings/${bookingId}/qr-code`
}
