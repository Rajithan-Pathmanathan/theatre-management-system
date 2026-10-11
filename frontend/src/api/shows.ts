import { apiClient } from './client'
import type { PublicSeatResponse, PublicShowResponse } from './types'

export async function listShowsByDate(date: string): Promise<PublicShowResponse[]> {
  const { data } = await apiClient.get<PublicShowResponse[]>('/api/shows', { params: { date } })
  return data
}

export async function getShow(showId: string): Promise<PublicShowResponse> {
  const { data } = await apiClient.get<PublicShowResponse>(`/api/shows/${showId}`)
  return data
}

export async function getShowSeats(showId: string): Promise<PublicSeatResponse[]> {
  const { data } = await apiClient.get<PublicSeatResponse[]>(`/api/shows/${showId}/seats`)
  return data
}
