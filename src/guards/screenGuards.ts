import type { CompanySession, SignedContract } from '../types.js';

/**
 * ARCH_SPEC.md §1.4: /dashboard, /rfq, /sign, /win all require activeCompanyId to be set;
 * redirect to / if not authenticated or no company selected.
 */
export function canAccessCompanyScopedRoute(
  isAuthenticated: boolean,
  activeCompanyId: string | null,
): boolean {
  return isAuthenticated && activeCompanyId !== null;
}

/**
 * ARCH_SPEC.md §1.4: /rfq CTA is disabled until
 * uploadStatus === 'done' && consumptionStatus === 'ready'.
 */
export function isRfqCtaEnabled(
  session: Pick<CompanySession, 'uploadStatus' | 'consumptionStatus'>,
): boolean {
  return session.uploadStatus === 'done' && session.consumptionStatus === 'ready';
}

/**
 * ARCH_SPEC.md §1.4: /win is reachable only when
 * signedContract != null && !isSpotPlan(signedContract.planType).
 */
export function canAccessWinPage(signedContract: SignedContract | null): boolean {
  return signedContract !== null && signedContract.planType !== 'spot';
}

/**
 * ARCH_SPEC.md §1.4: company switcher chip list only renders once companies.length >= 1
 * with at least one uploadStatus === 'done'.
 */
export function shouldShowCompanySwitcher(
  companies: Pick<CompanySession, 'uploadStatus'>[],
): boolean {
  return companies.some((c) => c.uploadStatus === 'done');
}
