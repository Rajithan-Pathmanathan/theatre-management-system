import { apiClient } from './client'
import type { PickupTiming, SnackItemResponse, SnackOrderRequest, SnackOrderResponse } from './types'

export async function listAvailableSnacks(): Promise<SnackItemResponse[]> {
  const { data } = await apiClient.get<SnackItemResponse[]>('/api/snacks')
  return data
}

export async function createSnackOrder(request: SnackOrderRequest): Promise<SnackOrderResponse> {
  const { data } = await apiClient.post<SnackOrderResponse>('/api/snack-orders', request)
  return data
}

/** BR-13 — the snacks to pay for with a booking's tickets. Replaces any saved earlier; an empty list clears them. */
export async function setBookingSnacks(
  bookingId: string,
  request: { pickupTiming: PickupTiming; items: { snackItemId: string; quantity: number }[] },
): Promise<void> {
  await apiClient.put(`/api/snack-orders/booking/${bookingId}`, request)
}

export async function getSnackOrder(id: string): Promise<SnackOrderResponse> {
  const { data } = await apiClient.get<SnackOrderResponse>(`/api/snack-orders/${id}`)
  return data
}

export function snackOrderQrCodeUrl(orderId: string): string {
  return `${apiClient.defaults.baseURL}/api/snack-orders/${orderId}/qr-code`
}
