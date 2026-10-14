import type { SnackItemResponse } from '../api/types'

/** The chosen snacks as order lines, and their total. */
export function snackSelection(snacks: SnackItemResponse[] | undefined, quantities: Record<string, number>) {
  const lines = (snacks ?? []).filter((snack) => (quantities[snack.id] ?? 0) > 0)
  const total = lines.reduce((sum, snack) => sum + snack.price * quantities[snack.id], 0)
  return { lines, total, items: lines.map((snack) => ({ snackItemId: snack.id, quantity: quantities[snack.id] })) }
}
