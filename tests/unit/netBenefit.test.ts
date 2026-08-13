import { describe, expect, it } from 'vitest';
import { netBenefit, selectBestQuote } from '../../src/calculations/netBenefit.js';
import type { Quote } from '../../src/types.js';
import {
  ANNUAL_KWH,
  CURRENT_RATE,
  EXPECTED,
  PENALTY,
  QUOTE_RATE_BETTER,
  QUOTE_RATE_WORSE,
} from '../fixtures/nordicTimberWorks.js';

function quote(overrides: Partial<Quote>): Quote {
  return {
    id: 'q1',
    supplier: 'Test Supplier',
    planType: 'fixed',
    rate: 14,
    rateDisplay: '14.0 c/kWh',
    isPersonal: false,
    netBenefit: 0,
    recommended: false,
    volatilityFlag: false,
    ...overrides,
  };
}

describe('netBenefit', () => {
  it('happy path: cheaper quote nets a positive benefit after penalty (Nordic Timber Works OÜ)', () => {
    expect(netBenefit(ANNUAL_KWH, CURRENT_RATE, QUOTE_RATE_BETTER, PENALTY)).toBe(
      EXPECTED.netBenefitBetter,
    );
  });

  it('edge case: a more expensive quote produces a negative net benefit', () => {
    expect(netBenefit(ANNUAL_KWH, CURRENT_RATE, QUOTE_RATE_WORSE, PENALTY)).toBe(
      EXPECTED.netBenefitWorse,
    );
  });

  it('edge case: zero penalty nets the full annual rate delta', () => {
    expect(netBenefit(ANNUAL_KWH, CURRENT_RATE, QUOTE_RATE_BETTER, 0)).toBe(
      EXPECTED.netBenefitNoPenalty,
    );
  });

  it('edge case: identical rate to current contract nets exactly -penalty', () => {
    expect(netBenefit(ANNUAL_KWH, CURRENT_RATE, CURRENT_RATE, PENALTY)).toBe(-PENALTY);
  });

  it('edge case: zero annual consumption nets exactly -penalty regardless of rates', () => {
    expect(netBenefit(0, CURRENT_RATE, QUOTE_RATE_BETTER, PENALTY)).toBe(-PENALTY);
  });

  it('rounds to the nearest whole currency unit', () => {
    // annualKwh=100, currentRate=10, quoteRate=9.995, penalty=0 -> raw delta = 0.005 -> rounds to 0
    expect(netBenefit(100, 10, 9.995, 0)).toBe(0);
  });
});

describe('selectBestQuote', () => {
  it('happy path: picks the quote with the highest netBenefit across default + personal quotes', () => {
    const quotes = [
      quote({ id: 'a', netBenefit: 500, isPersonal: false }),
      quote({ id: 'b', netBenefit: 1800, isPersonal: true }),
      quote({ id: 'c', netBenefit: 900, isPersonal: false }),
    ];
    expect(selectBestQuote(quotes)?.id).toBe('b');
  });

  it('edge case: empty quote list returns null rather than throwing', () => {
    expect(selectBestQuote([])).toBeNull();
  });

  it('edge case: a single quote is trivially the best', () => {
    const only = quote({ id: 'solo', netBenefit: -200 });
    expect(selectBestQuote([only])?.id).toBe('solo');
  });

  it('edge case: all-negative net benefits still returns the least-bad quote', () => {
    const quotes = [
      quote({ id: 'a', netBenefit: -900 }),
      quote({ id: 'b', netBenefit: -100 }),
      quote({ id: 'c', netBenefit: -500 }),
    ];
    expect(selectBestQuote(quotes)?.id).toBe('b');
  });

  it('edge case: tied best net benefit resolves deterministically to the first match', () => {
    const quotes = [
      quote({ id: 'first', netBenefit: 1000 }),
      quote({ id: 'second', netBenefit: 1000 }),
    ];
    expect(selectBestQuote(quotes)?.id).toBe('first');
  });
});
