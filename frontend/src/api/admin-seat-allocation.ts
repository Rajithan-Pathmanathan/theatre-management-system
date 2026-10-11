import { apiClient } from './client'
import type { SeatAllocationSummaryResponse } from './types'

export async function getSeatAllocationSummary(showId: string): Promise<SeatAllocationSummaryResponse> {
  const { data } = await apiClient.get<SeatAllocationSummaryResponse>(`/api/admin/shows/${showId}/seat-allocation`)
  return data
}

export async function updateSeatAllocation(showId: string, onlineAllocationPercent: number): Promise<SeatAllocationSummaryResponse> {
  const { data } = await apiClient.put<SeatAllocationSummaryResponse>(`/api/admin/shows/${showId}/seat-allocation`, {
    onlineAllocationPercent,
  })
  return data
}
