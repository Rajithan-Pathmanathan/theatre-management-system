import type { PickupTiming, SnackItemResponse } from '../api/types'
import { formatCurrency, PICKUP_LABELS } from '../lib/format'

type Props = {
  snacks: SnackItemResponse[] | undefined
  quantities: Record<string, number>
  onQuantitiesChange: (quantities: Record<string, number>) => void
  /** BR-14 — interval pickup is only offered when the show has an interval. */
  hasInterval: boolean
  pickup: PickupTiming
  onPickupChange: (pickup: PickupTiming) => void
  noIntervalNote?: boolean
}

/** Snack quantities plus pickup timing, shared by the standalone order page and the booking payment page (BR-13). */
export function SnackPicker({ snacks, quantities, onQuantitiesChange, hasInterval, pickup, onPickupChange, noIntervalNote }: Props) {
  return (
    <>
      <fieldset>
        <legend className="text-sm font-medium">Snacks</legend>
        {snacks?.length === 0 && <p className="mt-2 text-sm text-neutral-500">The canteen menu is empty right now.</p>}
        <div className="mt-2 flex flex-col gap-2">
          {snacks?.map((snack) => (
            <label key={snack.id} className="flex items-center justify-between rounded-xl border border-neutral-200 bg-white px-4 py-2">
              <span>
                {snack.name} <span className="text-neutral-500">· {formatCurrency(snack.price)}</span>
              </span>
              <input
                type="number"
                min="0"
                max="20"
                value={quantities[snack.id] ?? 0}
                onChange={(event) => onQuantitiesChange({ ...quantities, [snack.id]: Number(event.target.value) })}
                aria-label={`${snack.name} quantity`}
                className="w-16 rounded-lg border border-neutral-300 px-2 py-1 text-right"
              />
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className="text-sm font-medium">Pickup</legend>
        <div className="mt-2 flex flex-col gap-1">
          {(Object.keys(PICKUP_LABELS) as PickupTiming[])
            .filter((timing) => hasInterval || timing === 'BEFORE_MOVIE')
            .map((timing) => (
              <label key={timing} className="flex items-center gap-2">
                <input type="radio" name="pickup" checked={pickup === timing} onChange={() => onPickupChange(timing)} />
                {PICKUP_LABELS[timing]}
              </label>
            ))}
        </div>
        {noIntervalNote && <p className="mt-1 text-sm text-neutral-500">This show has no interval.</p>}
      </fieldset>
    </>
  )
}

