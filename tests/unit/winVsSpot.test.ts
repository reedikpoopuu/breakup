import { describe, expect, it } from 'vitest';
import { winVsSpot } from '../../src/calculations/winVsSpot.js';
import type { ConsumptionMonth } from '../../src/types.js';
import {
  EXPECTED,
  MONTHLY_KWH,
  MONTH_LABELS,
  QUOTE_RATE_BETTER,
  SPOT_INDEX,
} from '../fixtures/nordicTimberWorks.js';

function toConsumption(kwh: number[]): ConsumptionMonth[] {
  return kwh.map((k, i) => ({ label: MONTH_LABELS[i]!, kwh: k }));
}

describe('winVsSpot', () => {
  const consumption = toConsumption(MONTHLY_KWH);

  it('happy path: matches the hand-derived annual comparison for Nordic Timber Works OÜ', () => {
    const result = winVsSpot(consumption, QUOTE_RATE_BETTER, SPOT_INDEX);

    expect(result.fixedAnnual).toBeCloseTo(EXPECTED.winVsSpot.fixedAnnual, 2);
    expect(result.spotAnnual).toBeCloseTo(EXPECTED.winVsSpot.spotAnnual, 2);
    expect(result.savings).toBeCloseTo(EXPECTED.winVsSpot.savings, 2);
    expect(result.months).toHaveLength(12);
    EXPECTED.winVsSpot.months.forEach((expectedMonth, i) => {
      expect(result.months[i]!.fixedCost).toBeCloseTo(expectedMonth.fixedCost, 2);
      expect(result.months[i]!.spotCost).toBeCloseTo(expectedMonth.spotCost, 2);
      expect(result.months[i]!.label).toBe(MONTH_LABELS[i]);
    });
  });

  it('happy path: savings is negative when the signed fixed rate loses to spot (this fixture)', () => {
    const result = winVsSpot(consumption, QUOTE_RATE_BETTER, SPOT_INDEX);
    expect(result.savings).toBeLessThan(0);
  });

  it('edge case: a very low signed rate pushes the fixed-vs-spot delta further negative', () => {
    const result = winVsSpot(consumption, 5, SPOT_INDEX);
    expect(result.savings).toBeLessThan(EXPECTED.winVsSpot.savings);
  });

  it('edge case: zero consumption in every month yields zero cost on both sides', () => {
    const result = winVsSpot(toConsumption(Array(12).fill(0)), QUOTE_RATE_BETTER, SPOT_INDEX);
    expect(result.fixedAnnual).toBe(0);
    expect(result.spotAnnual).toBe(0);
    expect(result.savings).toBe(0);
  });

  it('edge case: a flat spot index of 0 still charges the flat SPOT_MARGIN per kWh', () => {
    const result = winVsSpot(consumption, QUOTE_RATE_BETTER, Array(12).fill(0));
    // spotCost_i = kwh_i * (13*0 + 2.3)/100 = kwh_i * 0.023
    const expectedSpotAnnual = MONTHLY_KWH.reduce((sum, kwh) => sum + kwh * 0.023, 0);
    expect(result.spotAnnual).toBeCloseTo(expectedSpotAnnual, 2);
  });
});
