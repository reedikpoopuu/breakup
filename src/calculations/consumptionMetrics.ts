import { NotImplementedError } from './netBenefit.js';

/**
 * ARCH_SPEC.md §2 "Consumption-pattern deviation %":
 * sum(|actualMonthShare - benchmarkMonthShare|) across 12 months, halved, ×100.
 * actualMonthShare/benchmarkMonthShare are expressed as percentages (0-100), not fractions,
 * matching the prototype's hardcoded benchmark curve (e.g. `[10, 9.3, ...]`).
 */
export function profileDeviationPct(monthlyKwh: number[], benchmarkShares: number[]): number {
  throw new NotImplementedError('profileDeviationPct');
}

/**
 * ARCH_SPEC.md §2 "Month-to-month volatility %": stddev / mean × 100 (coefficient of variation)
 * of the 12 monthly kWh values.
 */
export function volatilityPct(monthlyKwh: number[]): number {
  throw new NotImplementedError('volatilityPct');
}

/**
 * ARCH_SPEC.md §2 "Shape-vs-flat-load cost %":
 * (Σ(kwh_i × spotIndex_i) − Σ(mean_kwh × spotIndex_i)) / Σ(mean_kwh × spotIndex_i) × 100
 */
export function shapeMarginPct(monthlyKwh: number[], spotIndex: number[]): number {
  throw new NotImplementedError('shapeMarginPct');
}
