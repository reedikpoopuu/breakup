import { NotImplementedError } from './netBenefit.js';
import type { Company, Language } from '../types.js';

export interface RequestLetterInput {
  company: Company;
  language: Language;
  includeMetrics: { profile: boolean; volatility: boolean; shape: boolean };
}

/**
 * ARCH_SPEC.md §2 "RFQ request-letter text": deterministic template string interpolating
 * company name/registry code/EIC/annual kWh/included-metric labels into a fixed multi-paragraph
 * format with a rate-offer table (spot+margin, fixed 6/12/24mo rows), localized per language.
 */
export function generateRequestLetterText(input: RequestLetterInput): string {
  throw new NotImplementedError('generateRequestLetterText');
}
