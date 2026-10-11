import { apiClient } from './client'
import type { DailyReportResponse, ShowReportResponse } from './types'

export async function getDailyReport(date: string): Promise<DailyReportResponse> {
  const { data } = await apiClient.get<DailyReportResponse>('/api/admin/reports/daily', { params: { date } })
  return data
}

export async function getShowReport(showId: string): Promise<ShowReportResponse> {
  const { data } = await apiClient.get<ShowReportResponse>(`/api/admin/reports/shows/${showId}`)
  return data
}
