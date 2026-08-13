/**
 * ARCH_SPEC.md §6.3: Admin Controls needs a simple form for the global BenchmarkProfile —
 * 12 numeric inputs "ideally validated to sum to 100" + save.
 */

const EXPECTED_SUM = 100;
const SUM_TOLERANCE = 0.01;

export function isValidBenchmarkProfile(monthlyShares: number[]): boolean {
  if (monthlyShares.length !== 12) return false;
  if (monthlyShares.some((share) => share < 0 || !Number.isFinite(share))) return false;
  const sum = monthlyShares.reduce((total, share) => total + share, 0);
  return Math.abs(sum - EXPECTED_SUM) <= SUM_TOLERANCE;
}
