import { apiClient } from './client'
import type { StaffProfileResponse } from './types'

export async function listStaff(): Promise<StaffProfileResponse[]> {
  const { data } = await apiClient.get<StaffProfileResponse[]>('/api/admin/staff')
  return data
}
