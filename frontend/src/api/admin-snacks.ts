import { apiClient } from './client'
import type { CanteenSalesReportResponse, SnackItemRequest, SnackItemResponse } from './types'

export async function listSnackItems(): Promise<SnackItemResponse[]> {
  const { data } = await apiClient.get<SnackItemResponse[]>('/api/admin/snack-items')
  return data
}

export async function createSnackItem(request: SnackItemRequest): Promise<SnackItemResponse> {
  const { data } = await apiClient.post<SnackItemResponse>('/api/admin/snack-items', request)
  return data
}

export async function updateSnackItem(id: string, request: SnackItemRequest): Promise<SnackItemResponse> {
  const { data } = await apiClient.put<SnackItemResponse>(`/api/admin/snack-items/${id}`, request)
  return data
}

export async function updateSnackItemAvailability(id: string, available: boolean): Promise<SnackItemResponse> {
  const { data } = await apiClient.patch<SnackItemResponse>(`/api/admin/snack-items/${id}/availability`, { available })
  return data
}

/** FR-57. */
export async function getCanteenSalesReport(date: string): Promise<CanteenSalesReportResponse> {
  const { data } = await apiClient.get<CanteenSalesReportResponse>('/api/admin/canteen/reports/daily', { params: { date } })
  return data
}
