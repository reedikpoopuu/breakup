import type { Quote } from '../types.js';

export class NotImplementedError extends Error {
  constructor(fnName: string) {
    super(`${fnName} is not implemented yet — see ARCH_SPEC.md §2`);
    this.name = 'NotImplementedError';
  }
}

/**
 * ARCH_SPEC.md §2 "Net benefit per quote":
 * round((annualKwh * currentRate/100 - annualKwh * quoteRate/100) - penalty)
 */
export function netBenefit(
  annualKwh: number,
  currentRate: number,
  quoteRate: number,
  penalty: number,
): number {
  throw new NotImplementedError('netBenefit');
}

/**
 * Best offer = max netBenefit across all default + personal quotes combined.
 */
export function selectBestQuote(quotes: Quote[]): Quote | null {
  throw new NotImplementedError('selectBestQuote');
}
