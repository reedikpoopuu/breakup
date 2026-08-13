import type { ConsumptionMonth, WinVsSpotResult } from '../types.js';

export const SPOT_BASE_RATE = 13;
export const SPOT_MARGIN = 2.3;

/**
 * ARCH_SPEC.md §2 "Win-vs-spot annual comparison":
 * per month, fixedCost = kwh × signedRate/100;
 * spotCost = kwh × (SPOT_BASE_RATE × spotIndex_month + SPOT_MARGIN)/100;
 * winSavings = ΣfixedCost − ΣspotCost.
 */
export function winVsSpot(
  consumption: ConsumptionMonth[],
  signedRate: number,
  spotIndex: number[],
): WinVsSpotResult {
  const months = consumption.map((month, i) => {
    const fixedCost = (month.kwh * signedRate) / 100;
    const spotCost = (month.kwh * (SPOT_BASE_RATE * (spotIndex[i] ?? 0) + SPOT_MARGIN)) / 100;
    return { label: month.label, fixedCost, spotCost };
  });
  const fixedAnnual = months.reduce((sum, m) => sum + m.fixedCost, 0);
  const spotAnnual = months.reduce((sum, m) => sum + m.spotCost, 0);
  return { fixedAnnual, spotAnnual, savings: fixedAnnual - spotAnnual, months };
}
