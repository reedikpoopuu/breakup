import { describe, expect, it } from 'vitest';
import { isValidBenchmarkProfile } from '../../src/calculations/benchmarkProfile.js';

// Note: fixtures/nordicTimberWorks.ts's BENCHMARK_SHARES is a deliberately non-normalized
// "typical Baltic SME load profile" placeholder used as the benchmark input to
// profileDeviationPct (it sums to 96.7, not 100 — see consumptionMetrics.test.ts, whose golden
// values were hand-derived against that exact curve). It is not a valid saved BenchmarkProfile
// record, so it must not be reused here; this seasonal curve is a separate fixture that does
// sum to 100, for testing isValidBenchmarkProfile's happy path against a realistic non-uniform
// curve.
const VALID_SEASONAL_SHARES = [10.3, 9.6, 8.9, 8.1, 7.3, 6.7, 6.4, 6.7, 7.6, 8.6, 9.6, 10.2];

describe('isValidBenchmarkProfile (§6.3: 12 admin-editable shares, ideally summing to 100)', () => {
  it('happy path: a realistic seasonal curve summing to 100 is valid', () => {
    expect(VALID_SEASONAL_SHARES.reduce((sum, s) => sum + s, 0)).toBeCloseTo(100, 5);
    expect(isValidBenchmarkProfile([...VALID_SEASONAL_SHARES])).toBe(true);
  });

  it('happy path: an evenly-split 12-month curve is valid', () => {
    const even = Array(12).fill(100 / 12);
    expect(isValidBenchmarkProfile(even)).toBe(true);
  });

  it('edge case: a curve summing to under 100 is invalid', () => {
    const shares = Array(12).fill(5); // sums to 60
    expect(isValidBenchmarkProfile(shares)).toBe(false);
  });

  it('edge case: a curve summing to over 100 is invalid', () => {
    const shares = Array(12).fill(10); // sums to 120
    expect(isValidBenchmarkProfile(shares)).toBe(false);
  });

  it('edge case: floating-point rounding within tolerance is still valid', () => {
    const shares = [8.33, 8.33, 8.33, 8.34, 8.33, 8.33, 8.34, 8.33, 8.33, 8.34, 8.33, 8.34];
    expect(shares.reduce((sum, s) => sum + s, 0)).toBeCloseTo(100, 1);
    expect(isValidBenchmarkProfile(shares)).toBe(true);
  });

  it('edge case: fewer than 12 values is invalid regardless of sum', () => {
    const shares = Array(11).fill(100 / 11);
    expect(isValidBenchmarkProfile(shares)).toBe(false);
  });

  it('edge case: more than 12 values is invalid regardless of sum', () => {
    const shares = Array(13).fill(100 / 13);
    expect(isValidBenchmarkProfile(shares)).toBe(false);
  });

  it('edge case: a negative share is invalid even if the total sums to 100', () => {
    const shares = [-10, 20, 10, 10, 10, 10, 10, 10, 10, 10, 5, 5];
    expect(shares.reduce((sum, s) => sum + s, 0)).toBe(100);
    expect(isValidBenchmarkProfile(shares)).toBe(false);
  });

  it('edge case: an all-zero curve is invalid (sums to 0, not 100)', () => {
    expect(isValidBenchmarkProfile(Array(12).fill(0))).toBe(false);
  });

  it('edge case: NaN in the input is invalid', () => {
    const shares = [NaN, ...Array(11).fill(100 / 11)];
    expect(isValidBenchmarkProfile(shares)).toBe(false);
  });
});
