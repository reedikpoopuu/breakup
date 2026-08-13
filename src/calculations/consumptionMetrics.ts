/**
 * ARCH_SPEC.md §2 "Consumption-pattern deviation %":
 * sum(|actualMonthShare - benchmarkMonthShare|) across 12 months, halved, ×100.
 * actualMonthShare/benchmarkMonthShare are expressed as percentages (0-100), not fractions,
 * matching the prototype's hardcoded benchmark curve (e.g. `[10, 9.3, ...]`).
 */
export function profileDeviationPct(monthlyKwh: number[], benchmarkShares: number[]): number {
  const total = monthlyKwh.reduce((sum, kwh) => sum + kwh, 0);
  const actualShares = monthlyKwh.map((kwh) => (total === 0 ? 0 : (kwh / total) * 100));
  const sumAbsDelta = actualShares.reduce(
    (sum, share, i) => sum + Math.abs(share - (benchmarkShares[i] ?? 0)),
    0,
  );
  return sumAbsDelta / 2;
}

/**
 * ARCH_SPEC.md §2 "Month-to-month volatility %": stddev / mean × 100 (coefficient of variation)
 * of the 12 monthly kWh values.
 */
export function volatilityPct(monthlyKwh: number[]): number {
  const mean = monthlyKwh.reduce((sum, kwh) => sum + kwh, 0) / monthlyKwh.length;
  if (mean === 0) return 0;
  const variance =
    monthlyKwh.reduce((sum, kwh) => sum + (kwh - mean) ** 2, 0) / monthlyKwh.length;
  const stddev = Math.sqrt(variance);
  return (stddev / mean) * 100;
}

/**
 * ARCH_SPEC.md §2 "Shape-vs-flat-load cost %":
 * (Σ(kwh_i × spotIndex_i) − Σ(mean_kwh × spotIndex_i)) / Σ(mean_kwh × spotIndex_i) × 100
 */
export function shapeMarginPct(monthlyKwh: number[], spotIndex: number[]): number {
  const mean = monthlyKwh.reduce((sum, kwh) => sum + kwh, 0) / monthlyKwh.length;
  const actualCost = monthlyKwh.reduce((sum, kwh, i) => sum + kwh * (spotIndex[i] ?? 0), 0);
  const flatCost = spotIndex.reduce((sum, idx) => sum + mean * idx, 0);
  if (flatCost === 0) return 0;
  return ((actualCost - flatCost) / flatCost) * 100;
}
