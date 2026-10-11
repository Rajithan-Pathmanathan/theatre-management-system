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
