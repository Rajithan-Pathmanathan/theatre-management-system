import { createBrowserRouter } from 'react-router-dom'
import { LoginPage } from '../pages/auth/LoginPage'
import { MoviesPage } from '../pages/admin/MoviesPage'
import { ShowsPage } from '../pages/admin/ShowsPage'
import { SeatAllocationPage } from '../pages/admin/SeatAllocationPage'
import { BookingsPage } from '../pages/admin/BookingsPage'
import { ReportsPage } from '../pages/admin/ReportsPage'
import { AnalyticsPage } from '../pages/admin/AnalyticsPage'
import { EmployeesPage } from '../pages/admin/EmployeesPage'
import { SnackMenuPage } from '../pages/admin/SnackMenuPage'
import { CanteenFulfillmentPage } from '../pages/canteen/CanteenFulfillmentPage'
import { BookingConfirmationPage } from '../pages/customer/BookingConfirmationPage'
import { BookingStatusPage } from '../pages/customer/BookingStatusPage'
import { MovieBrowsePage } from '../pages/customer/MovieBrowsePage'
import { SeatBookingPage } from '../pages/customer/SeatBookingPage'
import { SnackOrderPage } from '../pages/customer/SnackOrderPage'
import { CounterSalesPage } from '../pages/counter/CounterSalesPage'
import { ValidationPage } from '../pages/counter/ValidationPage'

/**
 * Route map mirrors the use-case grouping in DEVELOPMENT_PLAN.md §3:
 * customer / counter / canteen / admin, plus staff auth.
 */
export const router = createBrowserRouter([
  { path: '/', element: <MovieBrowsePage /> }, // UC-07
  { path: '/book/:showId', element: <SeatBookingPage /> }, // UC-08
  { path: '/book/:showId/confirmation', element: <BookingConfirmationPage /> }, // UC-09
  { path: '/bookings/track', element: <BookingStatusPage /> }, // UC-12
  { path: '/snacks', element: <SnackOrderPage /> }, // UC-18 (Phase 3)

  { path: '/login', element: <LoginPage /> }, // UC-02

  { path: '/counter/sales', element: <CounterSalesPage /> }, // UC-10
  { path: '/counter/validate', element: <ValidationPage /> }, // UC-11

  { path: '/canteen/fulfillment', element: <CanteenFulfillmentPage /> }, // UC-19 (Phase 3)

  { path: '/admin/movies', element: <MoviesPage /> }, // UC-04
  { path: '/admin/shows', element: <ShowsPage /> }, // UC-05
  { path: '/admin/seat-allocation', element: <SeatAllocationPage /> }, // UC-06
  { path: '/admin/bookings', element: <BookingsPage /> }, // UC-13
  { path: '/admin/reports', element: <ReportsPage /> }, // UC-14
  { path: '/admin/analytics', element: <AnalyticsPage /> }, // UC-15 (Phase 2)
  { path: '/admin/employees', element: <EmployeesPage /> }, // UC-16 (Phase 2)
  { path: '/admin/snack-menu', element: <SnackMenuPage /> }, // UC-17 (Phase 3)
])
