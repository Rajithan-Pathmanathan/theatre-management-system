import { apiClient } from './client'
import type { PickupTiming, SnackOrderResponse, SnackOrderStatus } from './types'

/** UC-19. CANTEEN_STAFF/ADMIN only. */
export async function listCanteenQueue(date: string, pickupTiming: PickupTiming | null): Promise<SnackOrderResponse[]> {
  const { data } = await apiClient.get<SnackOrderResponse[]>('/api/canteen/orders', {
    params: { date, pickupTiming: pickupTiming ?? undefined },
  })
  return data
}

export async function lookupSnackOrderByToken(orderToken: string): Promise<SnackOrderResponse> {
  const { data } = await apiClient.get<SnackOrderResponse>('/api/canteen/orders/lookup', { params: { orderToken } })
  return data
}

export async function updateSnackOrderStatus(id: string, status: SnackOrderStatus): Promise<SnackOrderResponse> {
  const { data } = await apiClient.patch<SnackOrderResponse>(`/api/canteen/orders/${id}/status`, { status })
  return data
}

export async function updateSnackOrderItemAvailability(orderId: string, itemId: string, unavailable: boolean): Promise<SnackOrderResponse> {
  const { data } = await apiClient.patch<SnackOrderResponse>(`/api/canteen/orders/${orderId}/items/${itemId}/availability`, { unavailable })
  return data
}
