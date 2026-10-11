import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import type { StaffProfileResponse } from '../../api/types'

function landingPageFor(staff: StaffProfileResponse): string {
  switch (staff.role) {
    case 'ADMIN':
      return '/admin/bookings'
    case 'COUNTER_STAFF':
      return '/counter/sales'
    case 'CANTEEN_STAFF':
      return '/canteen/fulfillment'
  }
}

/** UC-02. Customers never need an account — this is for counter, canteen, and admin staff only. */
export function LoginPage() {
  const { staff, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation() as { state?: { from?: Location } }

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const loginMutation = useMutation({
    mutationFn: () => login({ username, password }),
    onSuccess: (loggedInStaff) => {
      const from = location.state?.from?.pathname
      navigate(from ?? landingPageFor(loggedInStaff), { replace: true })
    },
  })

  if (staff) {
    return <Navigate to={landingPageFor(staff)} replace />
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    loginMutation.mutate()
  }

  return (
    <main className="mx-auto max-w-sm px-6 py-16">
      <h1 className="text-2xl font-semibold">Staff Login</h1>
      <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
        Counter, canteen, and admin staff only — customers don't need an account.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-3">
        <label className="text-sm font-medium">
          Username
          <input
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>
        <label className="text-sm font-medium">
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
          />
        </label>

        {loginMutation.isError && <p className="text-sm text-red-600">Invalid username or password.</p>}

        <button
          type="submit"
          disabled={loginMutation.isPending}
          className="mt-2 rounded-md bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
        >
          {loginMutation.isPending ? 'Signing in…' : 'Sign In'}
        </button>
      </form>
    </main>
  )
}
