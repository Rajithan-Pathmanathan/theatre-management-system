import { apiClient } from './client'
import type { SnackItemResponse, SnackOrderRequest, SnackOrderResponse } from './types'

export async function listAvailableSnacks(): Promise<SnackItemResponse[]> {
  const { data } = await apiClient.get<SnackItemResponse[]>('/api/snacks')
  return data
}

export async function createSnackOrder(request: SnackOrderRequest): Promise<SnackOrderResponse> {
  const { data } = await apiClient.post<SnackOrderResponse>('/api/snack-orders', request)
  return data
}

export async function getSnackOrder(id: string): Promise<SnackOrderResponse> {
  const { data } = await apiClient.get<SnackOrderResponse>(`/api/snack-orders/${id}`)
  return data
}

export function snackOrderQrCodeUrl(orderId: string): string {
  return `${apiClient.defaults.baseURL}/api/snack-orders/${orderId}/qr-code`
}
