// Zod schemas mirroring the API contract in ARCH_SPEC.md §3. These are the executable
// specification the backend responses are validated against in contract tests.
import { z } from 'zod';

export const CountrySchema = z.enum(['EE', 'LV', 'LT']);

export const ContractSchema = z.object({
  supplier: z.string().min(1),
  planType: z.enum(['fixed', 'spot']),
  rate: z.number().nonnegative(),
  expiryDate: z.string(),
  penalty: z.number().nonnegative(),
  terms: z.array(z.string()),
  // ARCH_SPEC.md §3: "Contract gains one field vs. the prototype's shape: source".
  // Kept optional (not `.default()`) so pre-§7 fixtures without it still validate as before,
  // while `generated`/`supplier_email` is still enforced whenever the field is present.
  source: z.enum(['generated', 'supplier_email']).optional(),
});

export const SignedContractSchema = ContractSchema.extend({
  contractId: z.string().min(1),
  signedAt: z.string(),
  startDate: z.string(),
});

export const CompanySchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  registryCode: z.string().min(1),
  eicCode: z.string().min(1),
  annualKwh: z.number().nonnegative(),
});

export const CompanyDetailSchema = CompanySchema.extend({
  currentContract: ContractSchema.nullable(),
});

export const QuoteSchema = z.object({
  id: z.string().min(1),
  supplier: z.string().min(1),
  planType: z.enum(['fixed', 'spot']),
  rate: z.number().nonnegative(),
  rateDisplay: z.string().min(1),
  isPersonal: z.boolean(),
  netBenefit: z.number(),
  recommended: z.boolean(),
  volatilityFlag: z.boolean(),
});

export const ConsumptionMonthSchema = z.object({
  label: z.string().min(1),
  kwh: z.number().nonnegative(),
});

export const ConsumptionMetricsSchema = z.object({
  profileDeviationPct: z.number(),
  volatilityPct: z.number(),
  shapeMarginPct: z.number(),
});

export const WinVsSpotMonthSchema = z.object({
  label: z.string().min(1),
  fixedCost: z.number(),
  spotCost: z.number(),
});

// --- Auth / Smart-ID ---

export const SmartIdStartResponseSchema = z.object({
  sessionId: z.string().min(1),
  verificationCode: z.string().min(1),
});

export const SmartIdPollResponseSchema = z.object({
  status: z.enum(['pending', 'confirmed', 'failed']),
  jwt: z.string().optional(),
});

// --- Companies ---

export const CompaniesListResponseSchema = z.array(CompanySchema);

// --- Contracts / upload ---

export const UploadStartResponseSchema = z.object({
  jobId: z.string().min(1),
  status: z.literal('uploading'),
});

export const UploadPollResponseSchema = z.object({
  status: z.enum(['uploading', 'analyzing', 'done', 'failed']),
  contract: ContractSchema.optional(),
  lowConfidenceFields: z.array(z.string()).optional(),
});

export const CurrentContractResponseSchema = ContractSchema.nullable();

// --- Representative rights ---

export const RepresentativeRightsResponseSchema = z.object({
  verified: z.boolean(),
  source: z.string().min(1),
  verifiedAt: z.string(),
});

// --- Consumption ---

export const ConsumptionFetchStartResponseSchema = z.object({
  jobId: z.string().min(1),
  status: z.literal('fetching'),
});

export const ConsumptionFetchPollResponseSchema = z.object({
  status: z.enum(['fetching', 'ready', 'failed']),
});

export const ConsumptionResponseSchema = z.object({
  months: z.array(ConsumptionMonthSchema).length(12),
  metrics: ConsumptionMetricsSchema,
});

// --- RFQ ---

export const RfqCreateRequestSchema = z.object({
  includeMetrics: z.array(z.string()),
});

export const RfqCreateResponseSchema = z.object({
  rfqId: z.string().min(1),
  stage: z.literal('published'),
  quotes: z.array(QuoteSchema),
});

export const RfqSendResponseSchema = z.object({
  stage: z.literal('sending'),
});

export const RfqGetResponseSchema = z.object({
  stage: z.enum(['none', 'published', 'sending', 'received']),
  quotes: z.array(QuoteSchema),
  requestLetterText: z.string(),
});

export const RfqMonthlyCheckRequestSchema = z.object({
  enabled: z.boolean(),
});

// --- Sign ---

export const SignStartRequestSchema = z.object({
  quoteId: z.string().min(1),
});

export const SignStartResponseSchema = z.object({
  signSessionId: z.string().min(1),
  verificationCode: z.string().min(1),
});

export const SignPollResponseSchema = z
  .object({
    status: z.enum(['pending', 'signed', 'failed']),
    signedContract: SignedContractSchema.optional(),
  })
  .refine((data) => data.status !== 'signed' || data.signedContract !== undefined, {
    message: 'signedContract is required when status is "signed"',
    path: ['signedContract'],
  });

// --- Win vs spot ---

export const WinVsSpotResponseSchema = z.object({
  fixedAnnual: z.number(),
  spotAnnual: z.number(),
  savings: z.number(),
  months: z.array(WinVsSpotMonthSchema).length(12),
});

// --- Admin subsystem (ARCH_SPEC.md §6.2/§6.4): scraped default pricing + AI-assisted review ---

export const PriceSourceSchema = z.object({
  id: z.string().min(1),
  supplier: z.string().min(1),
  country: CountrySchema,
  sourceUrl: z.string().min(1),
  addedByAdminId: z.string().min(1),
  active: z.boolean(),
  lastScrapedAt: z.string().nullable(),
  complianceNote: z.string().nullable(),
});

export const PriceSourceCreateRequestSchema = z.object({
  supplier: z.string().min(1),
  country: CountrySchema,
  sourceUrl: z.string().min(1),
});

export const PriceSourceListResponseSchema = z.array(PriceSourceSchema);

export const ScrapeRunSchema = z.object({
  id: z.string().min(1),
  priceSourceId: z.string().min(1),
  scrapedAt: z.string(),
  rawSnapshotUrl: z.string().min(1),
  extractedRate: z.number().nonnegative(),
  previousRate: z.number().nonnegative().nullable(),
  aiDiffSummary: z.string().nullable(),
  changeDetected: z.boolean(),
  status: z.enum(['pending_review', 'acknowledged', 'corrected']),
  acknowledgedByAdminId: z.string().nullable(),
  acknowledgedAt: z.string().nullable(),
});

export const ScrapeRunListResponseSchema = z.array(ScrapeRunSchema);

export const ScrapeRunAcknowledgeRequestSchema = z.object({
  correctedRate: z.number().nonnegative().optional(),
});

export const BenchmarkProfileResponseSchema = z.object({
  monthlyShares: z.array(z.number()).length(12),
  updatedAt: z.string(),
  updatedBy: z.string().min(1),
});

export const BenchmarkProfileUpdateRequestSchema = z.object({
  monthlyShares: z.array(z.number()).length(12),
});

// --- Contract intake via inbound email (ARCH_SPEC.md §7.2/§7.3) ---

export const InboundContractEmailSchema = z.object({
  id: z.string().min(1),
  receivedAt: z.string(),
  fromAddress: z.string().min(1),
  subject: z.string(),
  matchedCompanyId: z.string().nullable(),
  matchedRfqId: z.string().nullable(),
  matchConfidence: z.number().min(0).max(1).nullable(),
  matchMethod: z.string().nullable(),
  rawEmailUrl: z.string().min(1),
  attachmentPdfUrl: z.string().min(1),
  status: z.enum(['matched', 'needs_triage', 'discarded']),
});

export const InboundContractEmailListResponseSchema = z.array(InboundContractEmailSchema);

export const InboundContractEmailMatchRequestSchema = z.object({
  companyId: z.string().min(1),
  rfqId: z.string().min(1),
});
