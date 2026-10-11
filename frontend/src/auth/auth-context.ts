import { createContext } from 'react'
import type { LoginRequest, StaffProfileResponse } from '../api/types'

export type AuthState = {
  staff: StaffProfileResponse | null
  isLoading: boolean
  login: (request: LoginRequest) => Promise<StaffProfileResponse>
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
