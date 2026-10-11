import { apiClient } from './client'
import type { MovieAnalyticsResponse, ShowOccupancyResponse } from './types'

export async function getMovieAnalytics(movieId: string): Promise<MovieAnalyticsResponse> {
  const { data } = await apiClient.get<MovieAnalyticsResponse>(`/api/admin/analytics/movies/${movieId}`)
  return data
}

export async function getOccupancyTrend(from: string, to: string): Promise<ShowOccupancyResponse[]> {
  const { data } = await apiClient.get<ShowOccupancyResponse[]>('/api/admin/analytics/occupancy', { params: { from, to } })
  return data
}
