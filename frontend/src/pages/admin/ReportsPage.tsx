import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { downloadDailyReportCsv, downloadShowReportCsv, getDailyReport } from '../../api/admin-reports'
import { getCanteenSalesReport } from '../../api/admin-snacks'
import { formatCurrency, formatTime, todayIso } from '../../lib/format'

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
    <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Daily Report</h1>
        <button
          type="button"
          onClick={() => dailyExportMutation.mutate()}
          disabled={!reportQuery.data || dailyExportMutation.isPending}
          className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium transition hover:border-brand disabled:opacity-50"
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
          className="mt-1 block rounded-lg border border-neutral-300 px-3 py-2"
        />
      </label>

      {reportQuery.isPending && <p className="mt-6 text-neutral-500">Loading…</p>}

      {reportQuery.data && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Total tickets</p>
              <p className="text-2xl font-bold">{reportQuery.data.totalTicketsSold}</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Total collections</p>
              <p className="text-2xl font-bold">{formatCurrency(reportQuery.data.totalCollections)}</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-sm text-neutral-500">Online / Physical</p>
              <p className="text-2xl font-bold">
                {reportQuery.data.online.ticketsSold} / {reportQuery.data.physical.ticketsSold}
              </p>
            </div>
          </div>

          <h2 className="mt-8 text-lg font-semibold">By show</h2>
          {reportQuery.data.shows.length === 0 && <p className="mt-2 text-neutral-500">No confirmed bookings on this date.</p>}
          <div className="mt-3 flex flex-col gap-2">
            {reportQuery.data.shows.map((show) => (
              <div key={show.showId} className="rounded-xl border border-neutral-200 bg-white p-4">
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
                      className="text-xs text-brand hover:underline disabled:opacity-50"
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
      <h2 className="mt-10 text-lg font-semibold">Canteen sales</h2>
      {canteenQuery.data && (
        <>
          <p className="mt-2 text-sm text-neutral-500">
            {canteenQuery.data.ordersCollected} {canteenQuery.data.ordersCollected === 1 ? 'order' : 'orders'} collected · {formatCurrency(canteenQuery.data.totalSales)}
          </p>
          {canteenQuery.data.items.length === 0 && <p className="mt-2 text-neutral-500">No collected snack orders on this date.</p>}
          <div className="mt-3 flex flex-col gap-2">
            {canteenQuery.data.items.map((item) => (
              <div key={item.itemName} className="flex justify-between rounded-xl border border-neutral-200 bg-white px-4 py-2">
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
