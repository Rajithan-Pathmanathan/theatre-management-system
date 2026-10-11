import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { StaffRole } from '../api/types'
import { useAuth } from './useAuth'

/** Gates a route subtree on being logged in, and optionally on role (mirrors the backend's SecurityConfig rules). */
export function ProtectedRoute({ allowedRoles }: { allowedRoles?: StaffRole[] }) {
  const { staff, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) return <p className="px-6 py-10 text-neutral-500">Loading…</p>
  if (!staff) return <Navigate to="/login" state={{ from: location }} replace />
  if (allowedRoles && !allowedRoles.includes(staff.role)) {
    return <p className="px-6 py-10 text-red-600">You don't have permission to view this page.</p>
  }
  return <Outlet />
}
