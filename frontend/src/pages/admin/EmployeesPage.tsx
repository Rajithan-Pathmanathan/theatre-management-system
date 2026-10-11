import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createEmployee, listEmployees, updateEmployee, updateEmployeeStatus } from '../../api/admin-employees'
import { listStaff } from '../../api/admin-staff'
import type { EmployeeRequest, EmployeeResponse } from '../../api/types'

const EMPTY_FORM: EmployeeRequest = { fullName: '', role: '', phoneNumber: null, email: null, linkedStaffAccountId: null }

/** UC-16, FR-46–FR-48, BR-09. Deliberately basic — name, role, contact, status; no attendance/shifts/payroll. */
export function EmployeesPage() {
  const queryClient = useQueryClient()
  const employeesQuery = useQuery({ queryKey: ['admin-employees'], queryFn: listEmployees })
  const staffQuery = useQuery({ queryKey: ['admin-staff'], queryFn: listStaff })

  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<EmployeeRequest>(EMPTY_FORM)

  const saveMutation = useMutation({
    mutationFn: (request: EmployeeRequest) => (editingId ? updateEmployee(editingId, request) : createEmployee(request)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] })
      setEditingId(null)
      setForm(EMPTY_FORM)
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => updateEmployeeStatus(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-employees'] }),
  })

  function startEdit(employee: EmployeeResponse) {
    setEditingId(employee.id)
    setForm({
      fullName: employee.fullName,
      role: employee.role,
      phoneNumber: employee.phoneNumber,
      email: employee.email,
      linkedStaffAccountId: employee.linkedStaffAccountId,
    })
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <h1 className="text-2xl font-semibold">Employees</h1>

      <div className="mt-6 grid gap-8 md:grid-cols-[2fr_1fr]">
        <div className="flex flex-col gap-2">
          {employeesQuery.isPending && <p className="text-neutral-500">Loading…</p>}
          {employeesQuery.data?.length === 0 && <p className="text-neutral-500">No employees yet.</p>}
          {employeesQuery.data?.map((employee) => (
            <div key={employee.id} className="flex items-center justify-between rounded-lg border border-neutral-200 px-4 py-3 dark:border-neutral-800">
              <button type="button" onClick={() => startEdit(employee)} className="text-left">
                <p className="font-medium">{employee.fullName}</p>
                <p className="text-sm text-neutral-500">
                  {employee.role}
                  {employee.phoneNumber ? ` · ${employee.phoneNumber}` : ''}
                  {employee.linkedStaffAccountUsername ? ` · login: ${employee.linkedStaffAccountUsername}` : ''}
                </p>
              </button>
              <div className="flex items-center gap-2">
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                    employee.active
                      ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200'
                      : 'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300'
                  }`}
                >
                  {employee.active ? 'Active' : 'Inactive'}
                </span>
                <button
                  type="button"
                  onClick={() => statusMutation.mutate({ id: employee.id, active: !employee.active })}
                  className="rounded-md border border-neutral-300 px-2 py-1 text-xs dark:border-neutral-700"
                >
                  {employee.active ? 'Deactivate' : 'Reactivate'}
                </button>
              </div>
            </div>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <h2 className="font-medium">{editingId ? 'Edit Employee' : 'New Employee'}</h2>
          <label className="text-sm font-medium">
            Full name
            <input
              value={form.fullName}
              onChange={(event) => setForm({ ...form, fullName: event.target.value })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Role
            <input
              value={form.role}
              onChange={(event) => setForm({ ...form, role: event.target.value })}
              placeholder="e.g. Usher, Cashier, Projectionist"
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Phone (optional)
            <input
              value={form.phoneNumber ?? ''}
              onChange={(event) => setForm({ ...form, phoneNumber: event.target.value || null })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Email (optional)
            <input
              type="email"
              value={form.email ?? ''}
              onChange={(event) => setForm({ ...form, email: event.target.value || null })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            />
          </label>
          <label className="text-sm font-medium">
            Linked staff login (optional)
            <select
              value={form.linkedStaffAccountId ?? ''}
              onChange={(event) => setForm({ ...form, linkedStaffAccountId: event.target.value || null })}
              className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
            >
              <option value="">None</option>
              {staffQuery.data?.map((staff) => (
                <option key={staff.id} value={staff.id}>
                  {staff.username} ({staff.role})
                </option>
              ))}
            </select>
          </label>

          {saveMutation.isError && (
            <p className="text-sm text-red-600">
              {(saveMutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                'Could not save this employee.'}
            </p>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={saveMutation.isPending}
              className="rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
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
                className="rounded-md border border-neutral-300 px-4 py-2 font-medium dark:border-neutral-700"
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
