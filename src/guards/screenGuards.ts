import { NotImplementedError } from '../calculations/netBenefit.js';
import type { CompanySession, SignedContract } from '../types.js';

/**
 * ARCH_SPEC.md §1.4: /dashboard, /rfq, /sign, /win all require activeCompanyId to be set;
 * redirect to / if not authenticated or no company selected.
 */
export function canAccessCompanyScopedRoute(
  isAuthenticated: boolean,
  activeCompanyId: string | null,
): boolean {
  throw new NotImplementedError('canAccessCompanyScopedRoute');
}

/**
 * ARCH_SPEC.md §1.4: /rfq CTA is disabled until
 * uploadStatus === 'done' && consumptionStatus === 'ready'.
 */
export function isRfqCtaEnabled(
  session: Pick<CompanySession, 'uploadStatus' | 'consumptionStatus'>,
): boolean {
  throw new NotImplementedError('isRfqCtaEnabled');
}

/**
 * ARCH_SPEC.md §1.4: /win is reachable only when
 * signedContract != null && !isSpotPlan(signedContract.planType).
 */
export function canAccessWinPage(signedContract: SignedContract | null): boolean {
  throw new NotImplementedError('canAccessWinPage');
}

/**
 * ARCH_SPEC.md §1.4: company switcher chip list only renders once companies.length >= 1
 * with at least one uploadStatus === 'done'.
 */
export function shouldShowCompanySwitcher(
  companies: Pick<CompanySession, 'uploadStatus'>[],
): boolean {
  throw new NotImplementedError('shouldShowCompanySwitcher');
}
