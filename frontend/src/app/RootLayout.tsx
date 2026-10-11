import { Link, Outlet } from 'react-router-dom'

/** Minimal chrome so the customer flow is actually navigable end to end. */
export function RootLayout() {
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
        </nav>
      </header>
      <Outlet />
    </div>
  )
}
