import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const LINK_CLASS = 'text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100'

const COUNTER_LINKS = [
  { to: '/counter/sales', label: 'Sell Tickets' },
  { to: '/counter/validate', label: 'Validate' },
]

const ADMIN_LINKS = [
  { to: '/admin/movies', label: 'Movies' },
  { to: '/admin/shows', label: 'Shows' },
  { to: '/admin/seat-allocation', label: 'Seat Allocation' },
  { to: '/admin/bookings', label: 'Bookings' },
  { to: '/admin/reports', label: 'Reports' },
  { to: '/admin/analytics', label: 'Analytics' },
  { to: '/admin/employees', label: 'Employees' },
  { to: '/admin/snack-menu', label: 'Snack Menu' },
]

/** Minimal chrome so the customer, counter, and admin flows are all actually navigable end to end. */
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
        <nav className="mx-auto flex max-w-4xl flex-wrap items-center gap-x-6 gap-y-2 px-6 py-3 text-sm">
          <Link to="/" className="font-semibold">
            Vijaya Theatre
          </Link>
          <Link to="/bookings/track" className={LINK_CLASS}>
            Track Booking
          </Link>
          <Link to="/snacks" className={LINK_CLASS}>
            Order Snacks
          </Link>

          {(staff?.role === 'COUNTER_STAFF' || staff?.role === 'ADMIN') &&
            COUNTER_LINKS.map((link) => (
              <Link key={link.to} to={link.to} className={LINK_CLASS}>
                {link.label}
              </Link>
            ))}

          {(staff?.role === 'CANTEEN_STAFF' || staff?.role === 'ADMIN') && (
            <Link to="/canteen/fulfillment" className={LINK_CLASS}>
              Canteen Orders
            </Link>
          )}

          {staff?.role === 'ADMIN' &&
            ADMIN_LINKS.map((link) => (
              <Link key={link.to} to={link.to} className={LINK_CLASS}>
                {link.label}
              </Link>
            ))}

          <span className="flex-1" />

          {staff ? (
            <div className="flex items-center gap-3">
              <span className="text-neutral-500">
                {staff.fullName} · {staff.role}
              </span>
              <button onClick={handleLogout} className={LINK_CLASS}>
                Log out
              </button>
            </div>
          ) : (
            <Link to="/login" className={LINK_CLASS}>
              Staff Login
            </Link>
          )}
        </nav>
      </header>
      <Outlet />
    </div>
  )
}
