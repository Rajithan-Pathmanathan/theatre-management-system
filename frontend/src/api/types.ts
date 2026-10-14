/** Mirrors the backend's public/guest-facing DTOs exactly — see backend/.../show/dto and booking/dto. */

export type SeatZone = 'FRONT' | 'MIDDLE' | 'BACK' | 'SIDE'
export type SeatStatus = 'AVAILABLE' | 'LOCKED' | 'BOOKED'
export type BookingStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED' | 'REFUNDED'

export type PublicShowResponse = {
  id: string
  movieId: string
  movieTitle: string
  showDate: string // ISO date, e.g. 2026-12-01
  startTime: string // HH:mm:ss
  adultPrice: number
  childPrice: number
  hasInterval: boolean
  onlineSeatsRemaining: number
}

export type PublicSeatResponse = {
  seatId: string
  seatNumber: string
  rowLabel: string
  seatInRow: number
  zone: SeatZone
  status: SeatStatus
}

export type BookedSeatResponse = {
  seatNumber: string
  zone: string
}

export type BookingRequest = {
  showId: string
  adultCount: number
  childCount: number
  seatAllocationIds: string[]
  customerName: string
  customerPhone: string
  customerEmail: string | null
}

export type BookingResponse = {
  id: string
  bookingReference: string
  status: BookingStatus
  showId: string
  movieTitle: string
  showDate: string
  startTime: string
  adultCount: number
  childCount: number
  totalAmount: number
  seats: BookedSeatResponse[]
  expiresAt: string | null
}

export type StaffRole = 'ADMIN' | 'COUNTER_STAFF' | 'CANTEEN_STAFF'
export type ShowStatus = 'SCHEDULED' | 'CANCELLED' | 'COMPLETED'
export type PhysicalPaymentMethod = 'CASH' | 'CARD' | 'OTHER'

export type StaffProfileResponse = {
  id: string
  username: string
  fullName: string
  email: string | null
  phoneNumber: string | null
  role: StaffRole
  active: boolean
}

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
  expiresInSeconds: number
  staff: StaffProfileResponse
}

/** The full admin/counter show shape — richer than the guest-facing PublicShowResponse. */
export type ShowResponse = {
  id: string
  movieId: string
  movieTitle: string
  showDate: string
  startTime: string
  status: ShowStatus
  adultPrice: number
  childPrice: number
  guaranteed: boolean
  onlineBookingEnabled: boolean
  onlineAllocationPercent: number
  hasInterval: boolean
}

export type CounterSaleRequest = {
  showId: string
  adultCount: number
  childCount: number
  seatAllocationIds: string[]
  physicalPaymentMethod: PhysicalPaymentMethod
  customerName: string | null
  customerPhone: string | null
}

export type BookingLookupResponse = {
  id: string
  bookingReference: string
  status: BookingStatus
  movieTitle: string
  showDate: string
  startTime: string
  customerName: string
  adultCount: number
  childCount: number
  ticketsUsed: number
  remainingTickets: number
  seats: BookedSeatResponse[]
}

export type CheckInRequest = {
  ticketsToAdmit: number
}

export type CheckInResponse = {
  bookingId: string
  bookingReference: string
  ticketsAdmittedThisEntry: number
  ticketsUsed: number
  remainingTickets: number
  fullyCheckedIn: boolean
  validatedAt: string
}

export type MovieStatus = 'SHOWING' | 'UPCOMING' | 'INACTIVE'

export type MovieResponse = {
  id: string
  title: string
  language: string
  genre: string
  durationMinutes: number
  posterUrl: string | null
  synopsis: string | null
  status: MovieStatus
}

export type MovieRequest = {
  title: string
  language: string
  genre: string
  durationMinutes: number
  posterUrl: string | null
  synopsis: string | null
}

export type ShowRequest = {
  movieId: string
  showDate: string
  startTime: string
  adultPrice: number
  childPrice: number
  guaranteed: boolean
  onlineBookingEnabled: boolean
  onlineAllocationPercent: number
  hasInterval: boolean
}

export type SeatAllocationItemResponse = {
  seatId: string
  seatNumber: string
  rowLabel: string
  seatInRow: number
  zone: SeatZone
  channel: 'ONLINE' | 'PHYSICAL'
  status: SeatStatus
}

export type SeatAllocationSummaryResponse = {
  showId: string
  onlineAllocationPercent: number
  totalSeats: number
  onlineCount: number
  physicalCount: number
  seats: SeatAllocationItemResponse[]
}

/** The full admin view of a booking, across both sales channels — see backend's BookingAdminResponse. */
export type BookingAdminResponse = {
  id: string
  bookingReference: string
  status: BookingStatus
  channel: 'ONLINE' | 'COUNTER'
  showId: string
  movieTitle: string
  showDate: string
  startTime: string
  customerName: string
  customerPhone: string
  customerEmail: string | null
  adultCount: number
  childCount: number
  totalAmount: number
  ticketsUsed: number
  remainingTickets: number
  physicalPaymentMethod: PhysicalPaymentMethod | null
  soldByStaffUsername: string | null
  seats: BookedSeatResponse[]
  confirmedAt: string | null
  createdAt: string
}

export type BookingAdminSearchParams = {
  showId?: string
  showDate?: string
  status?: BookingStatus
  channel?: 'ONLINE' | 'COUNTER'
  reference?: string
}

export type ChannelBreakdown = {
  ticketsSold: number
  collections: number
}

export type ShowReportResponse = {
  showId: string
  movieTitle: string
  showDate: string
  startTime: string
  ticketsSold: number
  collections: number
  online: ChannelBreakdown
  physical: ChannelBreakdown
}

export type DailyReportResponse = {
  date: string
  totalTicketsSold: number
  totalCollections: number
  online: ChannelBreakdown
  physical: ChannelBreakdown
  shows: ShowReportResponse[]
}

export type PaymentInitiationResponse = {
  checkoutUrl: string
  merchantId: string
  orderId: string
  itemsDescription: string
  amount: string
  currency: string
  hash: string
  firstName: string
  lastName: string
  email: string
  phone: string
  address: string
  city: string
  country: string
  returnUrl: string
  cancelUrl: string
  notifyUrl: string
}

export type EmployeeResponse = {
  id: string
  fullName: string
  role: string
  phoneNumber: string | null
  email: string | null
  active: boolean
  linkedStaffAccountId: string | null
  linkedStaffAccountUsername: string | null
}

export type EmployeeRequest = {
  fullName: string
  role: string
  phoneNumber: string | null
  email: string | null
  linkedStaffAccountId: string | null
}

export type MovieAnalyticsResponse = {
  movieId: string
  movieTitle: string
  totalShows: number
  ticketsSold: number
  collections: number
  averageOccupancyPercent: number
  ticketsUsed: number
  attendanceRatePercent: number
  online: ChannelBreakdown
  physical: ChannelBreakdown
}

export type ShowOccupancyResponse = {
  showId: string
  movieTitle: string
  showDate: string
  startTime: string
  totalSeats: number
  ticketsSold: number
  occupancyPercent: number
  ticketsUsed: number
  attendanceRatePercent: number
}

export type SnackItemResponse = {
  id: string
  name: string
  price: number
  available: boolean
}

export type SnackItemRequest = {
  name: string
  price: number
}

export type CanteenItemSales = {
  itemName: string
  quantitySold: number
  sales: number
}

export type CanteenSalesReportResponse = {
  date: string
  ordersCollected: number
  totalSales: number
  items: CanteenItemSales[]
}
