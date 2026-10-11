import { apiClient } from './client'
import type { BookingAdminResponse, BookingAdminSearchParams } from './types'

export async function searchBookings(params: BookingAdminSearchParams): Promise<BookingAdminResponse[]> {
  const { data } = await apiClient.get<BookingAdminResponse[]>('/api/admin/bookings', { params })
  return data
}

export async function getBookingAdmin(id: string): Promise<BookingAdminResponse> {
  const { data } = await apiClient.get<BookingAdminResponse>(`/api/admin/bookings/${id}`)
  return data
}
