import { describe, expect, it } from 'vitest'
import { toIsoDate } from './format'

describe('toIsoDate', () => {
  it('uses the local calendar date, not UTC', () => {
    // 00:30 local on 15 Oct is still 14 Oct in UTC for any zone east of Greenwich (e.g. Sri Lanka, +05:30).
    expect(toIsoDate(new Date(2026, 9, 15, 0, 30))).toBe('2026-10-15')
  })

  it('zero-pads month and day', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05')
  })
})
