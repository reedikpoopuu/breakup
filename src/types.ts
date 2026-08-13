// Domain types per ARCH_SPEC.md §1.3 and §3. These describe the contract the
// calculation and guard modules are tested against; they carry no logic.

export type Country = 'EE' | 'LV' | 'LT';
export type Language = 'en' | 'et' | 'lv' | 'lt';
export type PlanType = 'fixed' | 'spot';

export interface Company {
  id: string;
  name: string;
  registryCode: string;
  eicCode: string;
  annualKwh: number;
}

export interface Contract {
  supplier: string;
  planType: PlanType;
  rate: number; // c/kWh
  expiryDate: string; // ISO date
  penalty: number; // EUR, early-termination penalty
  terms: string[];
}

export interface SignedContract extends Contract {
  contractId: string;
  signedAt: string; // ISO date
  startDate: string; // ISO date
}

export interface ConsumptionMonth {
  label: string;
  kwh: number;
}

export interface ConsumptionMetrics {
  profileDeviationPct: number;
  volatilityPct: number;
  shapeMarginPct: number;
}

export interface Quote {
  id: string;
  supplier: string;
  planType: PlanType;
  rate: number;
  rateDisplay: string;
  isPersonal: boolean;
  netBenefit: number;
  recommended: boolean;
  volatilityFlag: boolean;
}

export type RfqStage = 'none' | 'published' | 'sending' | 'received';

export interface CompanySession {
  company: Company;
  currentContract: Contract | null;
  uploadStatus: 'idle' | 'uploading' | 'done';
  consumptionStatus: 'pending' | 'analyzing' | 'fetching' | 'ready';
  consumption: ConsumptionMonth[];
  rfq: { stage: RfqStage; quotes: Quote[]; requestId: string | null };
  selectedQuoteId: string | null;
  includeMetrics: { profile: boolean; volatility: boolean; shape: boolean };
  monthlyCheckEnabled: boolean;
  signedContract: SignedContract | null;
}

export interface WinVsSpotMonth {
  label: string;
  fixedCost: number;
  spotCost: number;
}

export interface WinVsSpotResult {
  fixedAnnual: number;
  spotAnnual: number;
  savings: number;
  months: WinVsSpotMonth[];
}
