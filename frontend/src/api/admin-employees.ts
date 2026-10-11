import { apiClient } from './client'
import type { EmployeeRequest, EmployeeResponse } from './types'

export async function listEmployees(): Promise<EmployeeResponse[]> {
  const { data } = await apiClient.get<EmployeeResponse[]>('/api/admin/employees')
  return data
}

export async function createEmployee(request: EmployeeRequest): Promise<EmployeeResponse> {
  const { data } = await apiClient.post<EmployeeResponse>('/api/admin/employees', request)
  return data
}

export async function updateEmployee(id: string, request: EmployeeRequest): Promise<EmployeeResponse> {
  const { data } = await apiClient.put<EmployeeResponse>(`/api/admin/employees/${id}`, request)
  return data
}

export async function updateEmployeeStatus(id: string, active: boolean): Promise<EmployeeResponse> {
  const { data } = await apiClient.patch<EmployeeResponse>(`/api/admin/employees/${id}/status`, { active })
  return data
}
