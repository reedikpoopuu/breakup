import { describe, expect, it } from 'vitest';
import {
  canAccessCompanyScopedRoute,
  canAccessWinPage,
  isRfqCtaEnabled,
  shouldShowCompanySwitcher,
} from '../../src/guards/screenGuards.js';
import type { CompanySession, SignedContract } from '../../src/types.js';

function signedContract(overrides: Partial<SignedContract>): SignedContract {
  return {
    supplier: 'Elektrum',
    planType: 'fixed',
    rate: 13.9,
    expiryDate: '2028-01-01',
    penalty: 0,
    terms: [],
    contractId: 'sc1',
    signedAt: '2026-08-13',
    startDate: '2026-09-01',
    ...overrides,
  };
}

describe('canAccessCompanyScopedRoute (§1.4: /dashboard, /rfq, /sign, /win)', () => {
  it('happy path: authenticated with an active company can access', () => {
    expect(canAccessCompanyScopedRoute(true, 'company-1')).toBe(true);
  });

  it('edge case: not authenticated is blocked even with an active company id', () => {
    expect(canAccessCompanyScopedRoute(false, 'company-1')).toBe(false);
  });

  it('edge case: authenticated but no active company is blocked', () => {
    expect(canAccessCompanyScopedRoute(true, null)).toBe(false);
  });

  it('edge case: neither authenticated nor an active company is blocked', () => {
    expect(canAccessCompanyScopedRoute(false, null)).toBe(false);
  });
});

describe('isRfqCtaEnabled (§1.4: /rfq CTA)', () => {
  it('happy path: enabled once upload is done and consumption is ready', () => {
    expect(isRfqCtaEnabled({ uploadStatus: 'done', consumptionStatus: 'ready' })).toBe(true);
  });

  it('edge case: disabled while upload is still in progress', () => {
    expect(isRfqCtaEnabled({ uploadStatus: 'uploading', consumptionStatus: 'ready' })).toBe(
      false,
    );
  });

  it('edge case: disabled while consumption is still analyzing/fetching', () => {
    expect(isRfqCtaEnabled({ uploadStatus: 'done', consumptionStatus: 'analyzing' })).toBe(
      false,
    );
    expect(isRfqCtaEnabled({ uploadStatus: 'done', consumptionStatus: 'fetching' })).toBe(
      false,
    );
  });

  it('edge case: disabled when both prerequisites are still pending', () => {
    expect(isRfqCtaEnabled({ uploadStatus: 'idle', consumptionStatus: 'pending' })).toBe(false);
  });
});

describe('canAccessWinPage (§1.4: /win)', () => {
  it('happy path: reachable with a signed fixed-plan contract', () => {
    expect(canAccessWinPage(signedContract({ planType: 'fixed' }))).toBe(true);
  });

  it('edge case: blocked when no contract has been signed yet', () => {
    expect(canAccessWinPage(null)).toBe(false);
  });

  it('edge case: blocked when the signed contract is itself a spot plan (nothing to compare)', () => {
    expect(canAccessWinPage(signedContract({ planType: 'spot' }))).toBe(false);
  });
});

describe('shouldShowCompanySwitcher (§1.4: company switcher chip list)', () => {
  it('happy path: shown once at least one company has finished uploading', () => {
    const companies: Pick<CompanySession, 'uploadStatus'>[] = [
      { uploadStatus: 'done' },
      { uploadStatus: 'uploading' },
    ];
    expect(shouldShowCompanySwitcher(companies)).toBe(true);
  });

  it('edge case: hidden when there are no companies at all', () => {
    expect(shouldShowCompanySwitcher([])).toBe(false);
  });

  it('edge case: hidden when companies exist but none have finished uploading', () => {
    const companies: Pick<CompanySession, 'uploadStatus'>[] = [
      { uploadStatus: 'idle' },
      { uploadStatus: 'uploading' },
    ];
    expect(shouldShowCompanySwitcher(companies)).toBe(false);
  });
});
