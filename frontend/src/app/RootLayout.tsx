import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const navClass = ({ isActive }: { isActive: boolean }) =>
  `whitespace-nowrap py-1 font-medium transition ${isActive ? 'text-brand' : 'text-neutral-600 hover:text-brand'}`

const staffNavClass = ({ isActive }: { isActive: boolean }) =>
  `whitespace-nowrap py-2 transition ${isActive ? 'text-white' : 'text-neutral-400 hover:text-white'}`

const CUSTOMER_LINKS = [
  { to: '/', label: 'Movies', end: true },
  { to: '/snacks', label: 'Order Snacks', end: false },
  { to: '/bookings/track', label: 'Track Booking', end: false },
]

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

/** One navbar for every flow: customer links on top, plus a dark strip of role-gated links once staff log in. */
export function RootLayout() {
  const { staff, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const staffLinks = [
    ...(staff?.role === 'COUNTER_STAFF' || staff?.role === 'ADMIN' ? COUNTER_LINKS : []),
    ...(staff?.role === 'CANTEEN_STAFF' || staff?.role === 'ADMIN' ? [{ to: '/canteen/fulfillment', label: 'Canteen Orders' }] : []),
    ...(staff?.role === 'ADMIN' ? ADMIN_LINKS : []),
  ]

  return (
    <div className="min-h-screen bg-page text-neutral-900">
      <header className="sticky top-0 z-20 bg-white shadow-sm">
        <nav className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-8 gap-y-1 px-4 py-3 text-sm sm:px-6">
          <Link to="/" className="flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-lg bg-brand text-lg font-black text-white">V</span>
            <span className="leading-tight">
              <span className="block text-lg font-extrabold tracking-tight">
                Vijaya<span className="text-brand">Theatre</span>
              </span>
              <span className="block text-xs text-neutral-500">Batticaloa</span>
            </span>
          </Link>

          <div className="order-last flex w-full gap-6 overflow-x-auto md:order-none md:w-auto">
            {CUSTOMER_LINKS.map((link) => (
              <NavLink key={link.to} to={link.to} end={link.end} className={navClass}>
                {link.label}
              </NavLink>
            ))}
          </div>

          <span className="flex-1" />

          {staff ? (
            <div className="flex items-center gap-3">
              <span className="hidden text-neutral-500 sm:inline">
                {staff.fullName} · {staff.role}
              </span>
              <button onClick={handleLogout} className="rounded-lg border border-neutral-300 px-3 py-1.5 font-medium hover:border-brand hover:text-brand">
                Log out
              </button>
            </div>
          ) : (
            <Link to="/login" className="rounded-lg bg-brand px-3 py-1.5 font-medium text-white transition hover:bg-brand-dark">
              Staff Login
            </Link>
          )}
        </nav>

        {staffLinks.length > 0 && (
          <div className="bg-neutral-800 text-sm">
            <div className="mx-auto flex max-w-6xl gap-6 overflow-x-auto px-4 sm:px-6">
              {staffLinks.map((link) => (
                <NavLink key={link.to} to={link.to} className={staffNavClass}>
                  {link.label}
                </NavLink>
              ))}
            </div>
          </div>
        )}
      </header>
      <Outlet />
    </div>
  )
}
