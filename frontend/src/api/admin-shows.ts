import { apiClient } from './client'
import type { ShowRequest, ShowResponse, ShowStatus } from './types'

export async function listShowsByDate(date: string): Promise<ShowResponse[]> {
  const { data } = await apiClient.get<ShowResponse[]>('/api/admin/shows', { params: { date } })
  return data
}

export async function createShow(request: ShowRequest): Promise<ShowResponse> {
  const { data } = await apiClient.post<ShowResponse>('/api/admin/shows', request)
  return data
}

export async function updateShow(id: string, request: ShowRequest): Promise<ShowResponse> {
  const { data } = await apiClient.put<ShowResponse>(`/api/admin/shows/${id}`, request)
  return data
}

export async function updateShowStatus(id: string, status: ShowStatus): Promise<ShowResponse> {
  const { data } = await apiClient.patch<ShowResponse>(`/api/admin/shows/${id}/status`, { status })
  return data
}
