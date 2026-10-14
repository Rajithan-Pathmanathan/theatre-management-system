import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createSnackItem, listSnackItems, updateSnackItem, updateSnackItemAvailability } from '../../api/admin-snacks'
import type { SnackItemResponse } from '../../api/types'
import { formatCurrency } from '../../lib/format'

const EMPTY_FORM = { name: '', price: '' }

/** UC-17, FR-49. Unavailable items stay on the menu here but are hidden from customers. */
export function SnackMenuPage() {
  const queryClient = useQueryClient()
  const itemsQuery = useQuery({ queryKey: ['admin-snack-items'], queryFn: listSnackItems })

  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const saveMutation = useMutation({
    mutationFn: () => {
      const request = { name: form.name, price: Number(form.price) }
      return editingId ? updateSnackItem(editingId, request) : createSnackItem(request)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-snack-items'] })
      setEditingId(null)
      setForm(EMPTY_FORM)
    },
  })

  const availabilityMutation = useMutation({
    mutationFn: ({ id, available }: { id: string; available: boolean }) => updateSnackItemAvailability(id, available),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-snack-items'] }),
  })

  function startEdit(item: SnackItemResponse) {
    setEditingId(item.id)
    setForm({ name: item.name, price: String(item.price) })
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    saveMutation.mutate()
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Snack Menu</h1>

      <div className="mt-6 grid gap-8 md:grid-cols-[2fr_1fr]">
        <div className="flex flex-col gap-2">
          {itemsQuery.isPending && <p className="text-neutral-500">Loading…</p>}
          {itemsQuery.data?.length === 0 && <p className="text-neutral-500">No snack items yet.</p>}
          {itemsQuery.data?.map((item) => (
            <div key={item.id} className="flex items-center justify-between rounded-xl border border-neutral-200 bg-white px-4 py-3">
              <button type="button" onClick={() => startEdit(item)} className="text-left">
                <p className="font-medium">{item.name}</p>
                <p className="text-sm text-neutral-500">{formatCurrency(item.price)}</p>
              </button>
              <div className="flex items-center gap-2">
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                    item.available
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-neutral-100 text-neutral-600'
                  }`}
                >
                  {item.available ? 'Available' : 'Unavailable'}
                </span>
                <button
                  type="button"
                  onClick={() => availabilityMutation.mutate({ id: item.id, available: !item.available })}
                  className="rounded-lg border border-neutral-300 px-2 py-1 text-xs"
                >
                  {item.available ? 'Mark unavailable' : 'Mark available'}
                </button>
              </div>
            </div>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <h2 className="font-medium">{editingId ? 'Edit Item' : 'New Item'}</h2>
          <label className="text-sm font-medium">
            Name
            <input
              required
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>
          <label className="text-sm font-medium">
            Price (LKR)
            <input
              required
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={(event) => setForm({ ...form, price: event.target.value })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
            />
          </label>

          {saveMutation.isError && (
            <p className="text-sm text-red-600">
              {(saveMutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                'Could not save this item.'}
            </p>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={saveMutation.isPending}
              className="rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
            >
              {editingId ? 'Save' : 'Create'}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={() => {
                  setEditingId(null)
                  setForm(EMPTY_FORM)
                }}
                className="rounded-lg border border-neutral-300 px-4 py-2 font-medium"
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>
    </main>
  )
}
