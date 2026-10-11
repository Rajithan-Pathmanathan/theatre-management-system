import { apiClient } from './client'
import type { LoginRequest, LoginResponse, StaffProfileResponse } from './types'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/api/auth/login', request)
  return data
}

export async function getOwnProfile(): Promise<StaffProfileResponse> {
  const { data } = await apiClient.get<StaffProfileResponse>('/api/auth/me')
  return data
}
