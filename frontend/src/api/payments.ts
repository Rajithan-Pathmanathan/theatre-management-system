import { apiClient } from './client'
import type { PaymentInitiationResponse } from './types'

export async function initiatePayment(bookingId: string): Promise<PaymentInitiationResponse> {
  const { data } = await apiClient.post<PaymentInitiationResponse>(`/api/bookings/${bookingId}/payment`)
  return data
}

/**
 * PayHere's hosted checkout is a browser redirect, not an API call — it must
 * receive the fields as a real HTML form POST (so the hash/fields travel as
 * form data PayHere's own page expects), not fetched and displayed. This
 * builds that form in memory and submits it immediately.
 */
export function redirectToPayHereCheckout(payment: PaymentInitiationResponse): void {
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = payment.checkoutUrl

  const fields: Record<string, string> = {
    merchant_id: payment.merchantId,
    return_url: payment.returnUrl,
    cancel_url: payment.cancelUrl,
    notify_url: payment.notifyUrl,
    order_id: payment.orderId,
    items: payment.itemsDescription,
    currency: payment.currency,
    amount: payment.amount,
    first_name: payment.firstName,
    last_name: payment.lastName,
    email: payment.email,
    phone: payment.phone,
    address: payment.address,
    city: payment.city,
    country: payment.country,
    hash: payment.hash,
  }

  for (const [name, value] of Object.entries(fields)) {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  }

  document.body.appendChild(form)
  form.submit()
}
