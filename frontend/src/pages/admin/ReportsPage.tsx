import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { downloadDailyReportCsv, downloadShowReportCsv, getDailyReport } from '../../api/admin-reports'
import { getCanteenSalesReport } from '../../api/admin-snacks'
import { formatCurrency, formatTime } from '../../lib/format'

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

/** UC-14, FR-33–FR-39. Only CONFIRMED bookings count — totals tie back to real realized sales. */
export function ReportsPage() {
  const [date, setDate] = useState(todayIso())
  const reportQuery = useQuery({ queryKey: ['daily-report', date], queryFn: () => getDailyReport(date) })
  const canteenQuery = useQuery({ queryKey: ['canteen-sales-report', date], queryFn: () => getCanteenSalesReport(date) })
  const dailyExportMutation = useMutation({ mutationFn: () => downloadDailyReportCsv(date) })
  const showExportMutation = useMutation({
    mutationFn: ({ showId, movieTitle }: { showId: string; movieTitle: string }) => downloadShowReportCsv(showId, movieTitle),
  })

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Daily Report</h1>
        <button
          type="button"
          onClick={() => dailyExportMutation.mutate()}
          disabled={!reportQuery.data || dailyExportMutation.isPending}
          className="rounded-md border border-neutral-300 px-3 py-1.5 text-sm font-medium transition hover:border-emerald-500 disabled:opacity-50 dark:border-neutral-700"
        >
          {dailyExportMutation.isPending ? 'Exporting…' : 'Export CSV'}
        </button>
      </div>

      <label className="mt-4 block text-sm font-medium">
        Date
        <input
          type="date"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          className="mt-1 block rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700 dark:bg-neutral-900"
        />
      </label>

      {reportQuery.isPending && <p className="mt-6 text-neutral-500">Loading…</p>}

      {reportQuery.data && (
        <>
          <div className="mt-6 grid grid-cols-3 gap-4">
            <div className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
              <p className="text-sm text-neutral-500">Total tickets</p>
              <p className="text-2xl font-semibold">{reportQuery.data.totalTicketsSold}</p>
            </div>
            <div className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
              <p className="text-sm text-neutral-500">Total collections</p>
              <p className="text-2xl font-semibold">{formatCurrency(reportQuery.data.totalCollections)}</p>
            </div>
            <div className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
              <p className="text-sm text-neutral-500">Online / Physical</p>
              <p className="text-2xl font-semibold">
                {reportQuery.data.online.ticketsSold} / {reportQuery.data.physical.ticketsSold}
              </p>
            </div>
          </div>

          <h2 className="mt-8 font-medium">By show</h2>
          {reportQuery.data.shows.length === 0 && <p className="mt-2 text-neutral-500">No confirmed bookings on this date.</p>}
          <div className="mt-3 flex flex-col gap-2">
            {reportQuery.data.shows.map((show) => (
              <div key={show.showId} className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
                <div className="flex items-center justify-between">
                  <p className="font-medium">
                    {show.movieTitle} · {formatTime(show.startTime)}
                  </p>
                  <div className="flex items-center gap-3">
                    <p className="font-semibold">{formatCurrency(show.collections)}</p>
                    <button
                      type="button"
                      onClick={() => showExportMutation.mutate({ showId: show.showId, movieTitle: show.movieTitle })}
                      disabled={showExportMutation.isPending}
                      className="text-xs text-emerald-600 hover:underline disabled:opacity-50"
                    >
                      Export
                    </button>
                  </div>
                </div>
                <p className="mt-1 text-sm text-neutral-500">
                  {show.ticketsSold} tickets — online {show.online.ticketsSold} ({formatCurrency(show.online.collections)}), physical{' '}
                  {show.physical.ticketsSold} ({formatCurrency(show.physical.collections)})
                </p>
              </div>
            ))}
          </div>
        </>
      )}

      {/* FR-57 — collected orders only; snacks are pay-at-collection. */}
      <h2 className="mt-10 font-medium">Canteen sales</h2>
      {canteenQuery.data && (
        <>
          <p className="mt-2 text-sm text-neutral-500">
            {canteenQuery.data.ordersCollected} orders collected · {formatCurrency(canteenQuery.data.totalSales)}
          </p>
          {canteenQuery.data.items.length === 0 && <p className="mt-2 text-neutral-500">No collected snack orders on this date.</p>}
          <div className="mt-3 flex flex-col gap-2">
            {canteenQuery.data.items.map((item) => (
              <div key={item.itemName} className="flex justify-between rounded-lg border border-neutral-200 px-4 py-2 dark:border-neutral-800">
                <p>
                  {item.itemName} × {item.quantitySold}
                </p>
                <p className="font-medium">{formatCurrency(item.sales)}</p>
              </div>
            ))}
          </div>
        </>
      )}
    </main>
  )
}
