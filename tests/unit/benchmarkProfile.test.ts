import { describe, expect, it } from 'vitest';
import { isValidBenchmarkProfile } from '../../src/calculations/benchmarkProfile.js';
import { BENCHMARK_SHARES } from '../fixtures/nordicTimberWorks.js';

describe('isValidBenchmarkProfile (§6.3: 12 admin-editable shares, ideally summing to 100)', () => {
  it('happy path: the fixture benchmark curve sums to 100 and is valid', () => {
    expect(BENCHMARK_SHARES.reduce((sum, s) => sum + s, 0)).toBeCloseTo(100, 5);
    expect(isValidBenchmarkProfile([...BENCHMARK_SHARES])).toBe(true);
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
