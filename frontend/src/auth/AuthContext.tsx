import { useEffect, useState, type ReactNode } from 'react'
import { getOwnProfile, login as loginRequest } from '../api/auth'
import { getStoredToken, setStoredToken } from '../api/client'
import type { LoginRequest, StaffProfileResponse } from '../api/types'
import { AuthContext } from './auth-context'

/** UC-01/UC-02/UC-03. Restores the session from the stored JWT on load, by re-fetching the profile it belongs to. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [staff, setStaff] = useState<StaffProfileResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const token = getStoredToken()
    if (!token) {
      setIsLoading(false)
      return
    }
    getOwnProfile()
      .then(setStaff)
      .catch(() => setStoredToken(null))
      .finally(() => setIsLoading(false))
  }, [])

  async function login(request: LoginRequest): Promise<StaffProfileResponse> {
    const response = await loginRequest(request)
    setStoredToken(response.token)
    setStaff(response.staff)
    return response.staff
  }

  function logout() {
    setStoredToken(null)
    setStaff(null)
  }

  return <AuthContext.Provider value={{ staff, isLoading, login, logout }}>{children}</AuthContext.Provider>
}
