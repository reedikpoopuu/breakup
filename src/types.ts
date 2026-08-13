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

export type ContractSource = 'generated' | 'supplier_email';

export interface Contract {
  supplier: string;
  planType: PlanType;
  rate: number; // c/kWh
  expiryDate: string; // ISO date
  penalty: number; // EUR, early-termination penalty
  terms: string[];
  // ARCH_SPEC.md §3/§7: distinguishes an app-generated draft from one counter-signed off a
  // supplier-emailed document. Optional here (not on SignedContract fixtures predating the
  // §7 revision) — see src/schemas/api.ts ContractSchema for the same accommodation.
  source?: ContractSource;
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

// --- Admin subsystem (ARCH_SPEC.md §6): scraped default pricing + AI-assisted review ---

export interface PriceSource {
  id: string;
  supplier: string;
  country: Country;
  sourceUrl: string;
  addedByAdminId: string;
  active: boolean;
  lastScrapedAt: string | null;
  complianceNote: string | null;
}

export type ScrapeRunStatus = 'pending_review' | 'acknowledged' | 'corrected';

export interface ScrapeRun {
  id: string;
  priceSourceId: string;
  scrapedAt: string;
  rawSnapshotUrl: string;
  extractedRate: number;
  previousRate: number | null;
  aiDiffSummary: string | null;
  changeDetected: boolean;
  status: ScrapeRunStatus;
  acknowledgedByAdminId: string | null;
  acknowledgedAt: string | null;
}

export interface BenchmarkProfile {
  monthlyShares: number[]; // length 12, ARCH_SPEC.md §6.3: "ideally validated to sum to 100"
  updatedAt: string;
  updatedBy: string;
}

// --- Contract intake via inbound email (ARCH_SPEC.md §7) ---

export type InboundContractEmailStatus = 'matched' | 'needs_triage' | 'discarded';

export interface InboundContractEmail {
  id: string;
  receivedAt: string;
  fromAddress: string;
  subject: string;
  matchedCompanyId: string | null;
  matchedRfqId: string | null;
  matchConfidence: number | null;
  matchMethod: string | null;
  rawEmailUrl: string;
  attachmentPdfUrl: string;
  status: InboundContractEmailStatus;
}
