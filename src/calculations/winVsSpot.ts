import { NotImplementedError } from './netBenefit.js';
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
  throw new NotImplementedError('winVsSpot');
}
