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

/**
 * A plain <a href> download can't carry the Authorization header this ADMIN-only
 * endpoint needs, so fetch it as a blob through the authenticated client instead
 * and trigger the browser's save dialog ourselves.
 */
export async function downloadDailyReportCsv(date: string): Promise<void> {
  const response = await apiClient.get('/api/admin/reports/daily/export', { params: { date }, responseType: 'blob' })
  triggerDownload(response.data as Blob, `daily-report-${date}.csv`)
}

export async function downloadShowReportCsv(showId: string, movieTitle: string): Promise<void> {
  const response = await apiClient.get(`/api/admin/reports/shows/${showId}/export`, { responseType: 'blob' })
  triggerDownload(response.data as Blob, `show-report-${movieTitle}.csv`)
}

function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
