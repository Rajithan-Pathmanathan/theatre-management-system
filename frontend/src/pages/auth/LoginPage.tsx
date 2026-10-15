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
    <main className="mx-4 my-12 max-w-sm rounded-2xl border border-neutral-200 bg-white px-6 py-8 shadow-sm sm:mx-auto">
      <h1 className="text-2xl font-bold">Staff Login</h1>
      <p className="mt-1 text-sm text-neutral-600">
        Counter, canteen, and admin staff only — customers don't need an account.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-3">
        <label className="text-sm font-medium">
          Username
          <input
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
          />
        </label>
        <label className="text-sm font-medium">
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2"
          />
        </label>

        {loginMutation.isError && <p className="text-sm text-red-600">Invalid username or password.</p>}

        <button
          type="submit"
          disabled={loginMutation.isPending}
          className="mt-2 rounded-lg bg-brand px-4 py-2 font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
        >
          {loginMutation.isPending ? 'Signing in…' : 'Sign In'}
        </button>
      </form>
    </main>
  )
}
