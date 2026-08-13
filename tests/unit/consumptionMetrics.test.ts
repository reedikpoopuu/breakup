import { describe, expect, it } from 'vitest';
import {
  profileDeviationPct,
  shapeMarginPct,
  volatilityPct,
} from '../../src/calculations/consumptionMetrics.js';
import {
  BENCHMARK_SHARES,
  EXPECTED,
  MONTHLY_KWH,
  SPOT_INDEX,
} from '../fixtures/nordicTimberWorks.js';

const FLAT_LOAD = Array(12).fill(12000);
const ZERO_LOAD = Array(12).fill(0);

describe('profileDeviationPct', () => {
  it('happy path: matches the hand-derived value for Nordic Timber Works OÜ vs the benchmark curve', () => {
    expect(profileDeviationPct(MONTHLY_KWH, BENCHMARK_SHARES)).toBeCloseTo(
      EXPECTED.profileDeviationPct,
      2,
    );
  });

  it('edge case: a perfectly flat 12-month load still deviates from a seasonal benchmark', () => {
    expect(profileDeviationPct(FLAT_LOAD, BENCHMARK_SHARES)).toBeCloseTo(7.3833, 2);
  });

  it('edge case: all consumption concentrated in a single month produces near-maximal deviation', () => {
    const spike = [131000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    expect(profileDeviationPct(spike, BENCHMARK_SHARES)).toBeCloseTo(88.35, 1);
  });

  it('edge case: zero total consumption does not throw or return NaN/Infinity (divide-by-zero guard)', () => {
    const result = profileDeviationPct(ZERO_LOAD, BENCHMARK_SHARES);
    expect(Number.isFinite(result)).toBe(true);
  });
});

describe('volatilityPct', () => {
  it('happy path: matches the hand-derived coefficient of variation for Nordic Timber Works OÜ', () => {
    expect(volatilityPct(MONTHLY_KWH)).toBeCloseTo(EXPECTED.volatilityPct, 2);
  });

  it('edge case: a perfectly flat 12-month load has zero volatility', () => {
    expect(volatilityPct(FLAT_LOAD)).toBe(0);
  });

  it('edge case: zero-mean consumption does not throw or return NaN/Infinity (divide-by-zero guard)', () => {
    const result = volatilityPct(ZERO_LOAD);
    expect(Number.isFinite(result)).toBe(true);
  });
});

describe('shapeMarginPct', () => {
  it('happy path: matches the hand-derived shape-vs-flat-load cost for Nordic Timber Works OÜ', () => {
    expect(shapeMarginPct(MONTHLY_KWH, SPOT_INDEX)).toBeCloseTo(EXPECTED.shapeMarginPct, 2);
  });

  it('edge case: a perfectly flat load has zero shape margin regardless of the spot-index curve', () => {
    expect(shapeMarginPct(FLAT_LOAD, SPOT_INDEX)).toBeCloseTo(0, 6);
  });

  it('edge case: zero consumption does not throw or return NaN/Infinity (divide-by-zero guard)', () => {
    const result = shapeMarginPct(ZERO_LOAD, SPOT_INDEX);
    expect(Number.isFinite(result)).toBe(true);
  });
});
