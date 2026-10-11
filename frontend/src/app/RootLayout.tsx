import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

/** Minimal chrome so both the customer and staff flows are actually navigable end to end. */
export function RootLayout() {
  const { staff, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-white text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <header className="border-b border-neutral-200 dark:border-neutral-800">
        <nav className="mx-auto flex max-w-4xl items-center gap-6 px-6 py-3 text-sm">
          <Link to="/" className="font-semibold">
            Vijaya Theatre
          </Link>
          <Link to="/bookings/track" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
            Track Booking
          </Link>

          {staff?.role === 'COUNTER_STAFF' && (
            <>
              <Link to="/counter/sales" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Sell Tickets
              </Link>
              <Link to="/counter/validate" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Validate
              </Link>
            </>
          )}

          {staff?.role === 'ADMIN' && (
            <>
              <Link to="/admin/bookings" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Bookings
              </Link>
              <Link to="/admin/reports" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Reports
              </Link>
              <Link to="/counter/sales" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Sell Tickets
              </Link>
              <Link to="/counter/validate" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Validate
              </Link>
            </>
          )}

          <span className="flex-1" />

          {staff ? (
            <div className="flex items-center gap-3">
              <span className="text-neutral-500">
                {staff.fullName} · {staff.role}
              </span>
              <button onClick={handleLogout} className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
                Log out
              </button>
            </div>
          ) : (
            <Link to="/login" className="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100">
              Staff Login
            </Link>
          )}
        </nav>
      </header>
      <Outlet />
    </div>
  )
}
