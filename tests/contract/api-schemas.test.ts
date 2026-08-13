// Contract tests validating example API payloads against the Zod schemas derived from
// ARCH_SPEC.md §3 (core SME flow) plus §6 (admin scraping/benchmark subsystem) and §7
// (inbound-email contract intake). Since no backend exists yet (greenfield build, per
// PM_ANSWERS.txt), these tests exercise the schemas directly against fixture JSON rather than a
// live/mocked HTTP layer — they are the executable version of the API contract the backend must
// satisfy, and they double as documentation of which fields are optional/nullable vs required.
import { describe, expect, it } from 'vitest';
import {
  BenchmarkProfileResponseSchema,
  BenchmarkProfileUpdateRequestSchema,
  CompaniesListResponseSchema,
  CompanyDetailSchema,
  ConsumptionFetchPollResponseSchema,
  ConsumptionFetchStartResponseSchema,
  ConsumptionResponseSchema,
  ContractSchema,
  CurrentContractResponseSchema,
  InboundContractEmailListResponseSchema,
  InboundContractEmailMatchRequestSchema,
  InboundContractEmailSchema,
  PriceSourceCreateRequestSchema,
  PriceSourceListResponseSchema,
  PriceSourceSchema,
  RepresentativeRightsResponseSchema,
  RfqCreateResponseSchema,
  RfqGetResponseSchema,
  RfqMonthlyCheckRequestSchema,
  RfqSendResponseSchema,
  ScrapeRunAcknowledgeRequestSchema,
  ScrapeRunListResponseSchema,
  ScrapeRunSchema,
  SignPollResponseSchema,
  SignStartResponseSchema,
  SmartIdPollResponseSchema,
  SmartIdStartResponseSchema,
  UploadPollResponseSchema,
  UploadStartResponseSchema,
  WinVsSpotResponseSchema,
} from '../../src/schemas/api.js';

describe('POST /auth/smartid/start', () => {
  it('happy path', () => {
    const payload = { sessionId: 'sess-1', verificationCode: '4821' };
    expect(SmartIdStartResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('missing response field: no verificationCode', () => {
    expect(SmartIdStartResponseSchema.safeParse({ sessionId: 'sess-1' }).success).toBe(false);
  });
});

describe('GET /auth/smartid/poll/:sessionId', () => {
  it('happy path: pending has no jwt yet', () => {
    expect(SmartIdPollResponseSchema.safeParse({ status: 'pending' }).success).toBe(true);
  });

  it('happy path: confirmed includes a jwt', () => {
    const payload = { status: 'confirmed', jwt: 'header.payload.signature' };
    expect(SmartIdPollResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('edge case: failed status with no jwt is valid (jwt is optional)', () => {
    expect(SmartIdPollResponseSchema.safeParse({ status: 'failed' }).success).toBe(true);
  });

  it('malformed: unknown status value is rejected', () => {
    expect(SmartIdPollResponseSchema.safeParse({ status: 'expired' }).success).toBe(false);
  });
});

describe('GET /companies + GET /companies/:id', () => {
  const company = {
    id: 'c1',
    name: 'Nordic Timber Works OÜ',
    registryCode: '12345678',
    eicCode: '38ZEE-00000001-X',
    annualKwh: 131_000,
  };

  it('happy path: companies list', () => {
    expect(CompaniesListResponseSchema.safeParse([company]).success).toBe(true);
  });

  it('edge case: empty companies list (new user, no companies yet) is valid', () => {
    expect(CompaniesListResponseSchema.safeParse([]).success).toBe(true);
  });

  it('happy path: company detail with a current contract', () => {
    const detail = {
      ...company,
      currentContract: {
        supplier: 'Eesti Energia (Enefit)',
        planType: 'fixed',
        rate: 15.6,
        expiryDate: '2027-01-01',
        penalty: 410,
        terms: ['12-month fixed term'],
      },
    };
    expect(CompanyDetailSchema.safeParse(detail).success).toBe(true);
  });

  it('edge case: company detail with no current contract yet (currentContract: null)', () => {
    const detail = { ...company, currentContract: null };
    expect(CompanyDetailSchema.safeParse(detail).success).toBe(true);
  });

  it('missing API response: currentContract key entirely absent is rejected (must be explicit null)', () => {
    expect(CompanyDetailSchema.safeParse(company).success).toBe(false);
  });

  it('malformed: negative annualKwh is rejected', () => {
    const detail = { ...company, annualKwh: -500, currentContract: null };
    expect(CompanyDetailSchema.safeParse(detail).success).toBe(false);
  });
});

describe('POST /companies/:id/contracts/upload + poll', () => {
  it('happy path: upload start', () => {
    expect(
      UploadStartResponseSchema.safeParse({ jobId: 'job-1', status: 'uploading' }).success,
    ).toBe(true);
  });

  it('happy path: poll done with a fully extracted contract', () => {
    const payload = {
      status: 'done',
      contract: {
        supplier: 'Elektrum',
        planType: 'fixed',
        rate: 15.6,
        expiryDate: '2027-01-01',
        penalty: 410,
        terms: [],
      },
    };
    expect(UploadPollResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('happy path: poll done with low-confidence fields flagged for review', () => {
    const payload = {
      status: 'done',
      contract: {
        supplier: 'Elektrum',
        planType: 'fixed',
        rate: 15.6,
        expiryDate: '2027-01-01',
        penalty: 410,
        terms: [],
      },
      lowConfidenceFields: ['penalty', 'expiryDate'],
    };
    expect(UploadPollResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('missing API response: still-in-progress states have neither contract nor lowConfidenceFields', () => {
    expect(UploadPollResponseSchema.safeParse({ status: 'uploading' }).success).toBe(true);
    expect(UploadPollResponseSchema.safeParse({ status: 'analyzing' }).success).toBe(true);
  });

  it('edge case: failed status carries no contract (spec defines no error-detail field for this case)', () => {
    expect(UploadPollResponseSchema.safeParse({ status: 'failed' }).success).toBe(true);
  });

  it('malformed: done status with an incomplete contract object is rejected', () => {
    const payload = { status: 'done', contract: { supplier: 'Elektrum' } };
    expect(UploadPollResponseSchema.safeParse(payload).success).toBe(false);
  });

  it('happy path: current contract can be null when none exists yet', () => {
    expect(CurrentContractResponseSchema.safeParse(null).success).toBe(true);
  });
});

describe('POST /companies/:id/representative-rights/verify', () => {
  it('happy path', () => {
    const payload = { verified: true, source: 'e-Business Register', verifiedAt: '2026-08-13' };
    expect(RepresentativeRightsResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('happy path: verification can come back negative', () => {
    const payload = { verified: false, source: 'e-Business Register', verifiedAt: '2026-08-13' };
    expect(RepresentativeRightsResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('missing API response: no source given is rejected', () => {
    expect(
      RepresentativeRightsResponseSchema.safeParse({ verified: true, verifiedAt: '2026-08-13' })
        .success,
    ).toBe(false);
  });
});

describe('POST /companies/:id/consumption/fetch + poll + GET', () => {
  it('happy path: fetch start', () => {
    expect(
      ConsumptionFetchStartResponseSchema.safeParse({ jobId: 'job-2', status: 'fetching' })
        .success,
    ).toBe(true);
  });

  it('happy path: fetch poll ready', () => {
    expect(ConsumptionFetchPollResponseSchema.safeParse({ status: 'ready' }).success).toBe(true);
  });

  it('edge case: fetch poll failed (e.g. datahub outage/no access yet, per PM_QUESTIONS.txt #3)', () => {
    expect(ConsumptionFetchPollResponseSchema.safeParse({ status: 'failed' }).success).toBe(
      true,
    );
  });

  it('happy path: consumption response with exactly 12 months and metrics', () => {
    const months = Array.from({ length: 12 }, (_, i) => ({ label: `M${i + 1}`, kwh: 1000 + i }));
    const payload = {
      months,
      metrics: { profileDeviationPct: 8.07, volatilityPct: 35.42, shapeMarginPct: 6.07 },
    };
    expect(ConsumptionResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('malformed: consumption response with fewer than 12 months is rejected', () => {
    const months = Array.from({ length: 11 }, (_, i) => ({ label: `M${i + 1}`, kwh: 1000 }));
    const payload = {
      months,
      metrics: { profileDeviationPct: 0, volatilityPct: 0, shapeMarginPct: 0 },
    };
    expect(ConsumptionResponseSchema.safeParse(payload).success).toBe(false);
  });

  it('missing API response: metrics object entirely absent is rejected', () => {
    const months = Array.from({ length: 12 }, (_, i) => ({ label: `M${i + 1}`, kwh: 1000 }));
    expect(ConsumptionResponseSchema.safeParse({ months }).success).toBe(false);
  });
});

describe('RFQ endpoints', () => {
  const quote = {
    id: 'q1',
    supplier: 'Ignitis',
    planType: 'fixed',
    rate: 13.9,
    rateDisplay: '13.9 c/kWh',
    isPersonal: false,
    netBenefit: 1817,
    recommended: true,
    volatilityFlag: false,
  };

  it('happy path: rfq create returns published stage with quotes', () => {
    const payload = { rfqId: 'rfq-1', stage: 'published', quotes: [quote] };
    expect(RfqCreateResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('edge case: rfq create with zero default quotes available yet is still valid', () => {
    const payload = { rfqId: 'rfq-1', stage: 'published', quotes: [] };
    expect(RfqCreateResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('happy path: rfq send acknowledges sending stage', () => {
    expect(RfqSendResponseSchema.safeParse({ stage: 'sending' }).success).toBe(true);
  });

  it('happy path: rfq get with received quotes and a letter', () => {
    const payload = {
      stage: 'received',
      quotes: [quote],
      requestLetterText: 'Dear Supplier, ...',
    };
    expect(RfqGetResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('missing API response: requestLetterText absent is rejected (frontend must not fabricate legal text)', () => {
    const payload = { stage: 'received', quotes: [quote] };
    expect(RfqGetResponseSchema.safeParse(payload).success).toBe(false);
  });

  it('malformed: quote missing required rate field is rejected', () => {
    const { rate, ...quoteWithoutRate } = quote;
    const payload = { rfqId: 'rfq-1', stage: 'published', quotes: [quoteWithoutRate] };
    expect(RfqCreateResponseSchema.safeParse(payload).success).toBe(false);
  });

  it('malformed: unknown planType on a quote is rejected', () => {
    const badQuote = { ...quote, planType: 'variable' };
    const payload = { rfqId: 'rfq-1', stage: 'published', quotes: [badQuote] };
    expect(RfqCreateResponseSchema.safeParse(payload).success).toBe(false);
  });
});

describe('POST /companies/:id/rfq/:rfqId/monthly-check', () => {
  it('happy path: enabling the monthly check', () => {
    expect(RfqMonthlyCheckRequestSchema.safeParse({ enabled: true }).success).toBe(true);
  });

  it('happy path: disabling the monthly check', () => {
    expect(RfqMonthlyCheckRequestSchema.safeParse({ enabled: false }).success).toBe(true);
  });

  it('missing API response: enabled field absent is rejected', () => {
    expect(RfqMonthlyCheckRequestSchema.safeParse({}).success).toBe(false);
  });

  it('malformed: non-boolean enabled value is rejected', () => {
    expect(RfqMonthlyCheckRequestSchema.safeParse({ enabled: 'true' }).success).toBe(false);
  });
});

describe('Contract.source (§3/§7: generated draft vs. supplier-emailed document)', () => {
  const baseContract = {
    supplier: 'Elektrum',
    planType: 'fixed' as const,
    rate: 15.6,
    expiryDate: '2027-01-01',
    penalty: 410,
    terms: [],
  };

  it('happy path: source omitted is still valid (pre-§7 contracts / backward compatibility)', () => {
    expect(ContractSchema.safeParse(baseContract).success).toBe(true);
  });

  it('happy path: source "generated" is valid (app-generated draft)', () => {
    expect(ContractSchema.safeParse({ ...baseContract, source: 'generated' }).success).toBe(
      true,
    );
  });

  it('happy path: source "supplier_email" is valid (received via inbound-email intake, §7)', () => {
    expect(
      ContractSchema.safeParse({ ...baseContract, source: 'supplier_email' }).success,
    ).toBe(true);
  });

  it('malformed: an unknown source value is rejected', () => {
    expect(ContractSchema.safeParse({ ...baseContract, source: 'manual_entry' }).success).toBe(
      false,
    );
  });
});

describe('Admin: POST/GET /admin/price-sources, GET .../scrape-runs, POST .../acknowledge (§6.4)', () => {
  const priceSource = {
    id: 'ps-1',
    supplier: 'Eesti Energia (Enefit)',
    country: 'EE',
    sourceUrl: 'https://enefit.ee/pricing',
    addedByAdminId: 'admin-1',
    active: true,
    lastScrapedAt: '2026-08-13T05:00:00.000Z',
    complianceNote: 'robots.txt checked 2026-08-01, scraping permitted',
  };

  it('happy path: create price-source request', () => {
    const payload = { supplier: 'Elektrum', country: 'LV', sourceUrl: 'https://elektrum.lv/hinnad' };
    expect(PriceSourceCreateRequestSchema.safeParse(payload).success).toBe(true);
  });

  it('malformed: create price-source with an unsupported country is rejected', () => {
    const payload = { supplier: 'Elektrum', country: 'FI', sourceUrl: 'https://elektrum.fi' };
    expect(PriceSourceCreateRequestSchema.safeParse(payload).success).toBe(false);
  });

  it('happy path: price-source list', () => {
    expect(PriceSourceListResponseSchema.safeParse([priceSource]).success).toBe(true);
  });

  it('edge case: empty price-source list (no suppliers configured yet) is valid', () => {
    expect(PriceSourceListResponseSchema.safeParse([]).success).toBe(true);
  });

  it('edge case: a never-scraped, newly-added price source has null lastScrapedAt/complianceNote', () => {
    const fresh = { ...priceSource, lastScrapedAt: null, complianceNote: null, active: false };
    expect(PriceSourceSchema.safeParse(fresh).success).toBe(true);
  });

  it('missing API response: complianceNote key entirely absent is rejected (must be explicit null)', () => {
    const { complianceNote, ...withoutNote } = priceSource;
    expect(PriceSourceSchema.safeParse(withoutNote).success).toBe(false);
  });

  const scrapeRun = {
    id: 'run-1',
    priceSourceId: 'ps-1',
    scrapedAt: '2026-08-13T05:00:00.000Z',
    rawSnapshotUrl: 's3://scrapes/ps-1/2026-08-13.html',
    extractedRate: 15.9,
    previousRate: 15.6,
    aiDiffSummary: 'Rate increased from 15.6 to 15.9 c/kWh; no other changes detected.',
    changeDetected: true,
    status: 'pending_review',
    acknowledgedByAdminId: null,
    acknowledgedAt: null,
  };

  it('happy path: scrape-run history with a detected change awaiting review', () => {
    expect(ScrapeRunListResponseSchema.safeParse([scrapeRun]).success).toBe(true);
  });

  it('happy path: an acknowledged scrape-run carries admin attribution', () => {
    const acknowledged = {
      ...scrapeRun,
      status: 'acknowledged',
      acknowledgedByAdminId: 'admin-1',
      acknowledgedAt: '2026-08-13T08:00:00.000Z',
    };
    expect(ScrapeRunSchema.safeParse(acknowledged).success).toBe(true);
  });

  it('edge case: first-ever scrape for a source has no previousRate and no change detected', () => {
    const first = { ...scrapeRun, previousRate: null, changeDetected: false, aiDiffSummary: null };
    expect(ScrapeRunSchema.safeParse(first).success).toBe(true);
  });

  it('malformed: an unknown scrape-run status is rejected', () => {
    expect(ScrapeRunSchema.safeParse({ ...scrapeRun, status: 'ignored' }).success).toBe(false);
  });

  it('happy path: acknowledge with no correction (accept the extracted rate as-is)', () => {
    expect(ScrapeRunAcknowledgeRequestSchema.safeParse({}).success).toBe(true);
  });

  it('happy path: acknowledge with a manually corrected rate', () => {
    expect(
      ScrapeRunAcknowledgeRequestSchema.safeParse({ correctedRate: 15.75 }).success,
    ).toBe(true);
  });

  it('malformed: a negative corrected rate is rejected', () => {
    expect(
      ScrapeRunAcknowledgeRequestSchema.safeParse({ correctedRate: -1 }).success,
    ).toBe(false);
  });
});

describe('Admin: GET/PUT /admin/benchmark-profile (§6.3/§6.4)', () => {
  it('happy path: 12-value benchmark profile response', () => {
    const payload = {
      monthlyShares: [10, 9.3, 8.6, 7.8, 7.0, 6.4, 6.1, 6.4, 7.3, 8.3, 9.3, 10.2],
      updatedAt: '2026-08-01T00:00:00.000Z',
      updatedBy: 'admin-1',
    };
    expect(BenchmarkProfileResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('malformed: benchmark profile response with fewer than 12 months is rejected', () => {
    const payload = {
      monthlyShares: Array(11).fill(100 / 11),
      updatedAt: '2026-08-01T00:00:00.000Z',
      updatedBy: 'admin-1',
    };
    expect(BenchmarkProfileResponseSchema.safeParse(payload).success).toBe(false);
  });

  it('happy path: benchmark profile update request', () => {
    const payload = { monthlyShares: Array(12).fill(100 / 12) };
    expect(BenchmarkProfileUpdateRequestSchema.safeParse(payload).success).toBe(true);
  });

  it('missing API response: monthlyShares absent on update request is rejected', () => {
    expect(BenchmarkProfileUpdateRequestSchema.safeParse({}).success).toBe(false);
  });

  it('malformed: update request with 13 months is rejected', () => {
    const payload = { monthlyShares: Array(13).fill(100 / 13) };
    expect(BenchmarkProfileUpdateRequestSchema.safeParse(payload).success).toBe(false);
  });
});

describe('Admin: GET /admin/inbound-contract-emails, POST .../match (§7.2/§7.3)', () => {
  const matchedEmail = {
    id: 'ice-1',
    receivedAt: '2026-08-13T09:00:00.000Z',
    fromAddress: 'sales@elektrum.lv',
    subject: 'Contract attached — ref RFQ-4821',
    matchedCompanyId: 'c1',
    matchedRfqId: 'rfq-4821',
    matchConfidence: 0.97,
    matchMethod: 'subject-token',
    rawEmailUrl: 's3://inbound-email/ice-1/raw.eml',
    attachmentPdfUrl: 's3://inbound-email/ice-1/contract.pdf',
    status: 'matched',
  };

  it('happy path: an auto-matched inbound contract email', () => {
    expect(InboundContractEmailListResponseSchema.safeParse([matchedEmail]).success).toBe(true);
  });

  it('happy path: a low-confidence email routed to manual triage', () => {
    const needsTriage = {
      ...matchedEmail,
      matchedCompanyId: null,
      matchedRfqId: null,
      matchConfidence: null,
      matchMethod: null,
      status: 'needs_triage',
    };
    expect(InboundContractEmailSchema.safeParse(needsTriage).success).toBe(true);
  });

  it('edge case: a discarded email (spam/no attachment) is a valid terminal state', () => {
    const discarded = {
      ...matchedEmail,
      matchedCompanyId: null,
      matchedRfqId: null,
      matchConfidence: null,
      matchMethod: null,
      status: 'discarded',
    };
    expect(InboundContractEmailSchema.safeParse(discarded).success).toBe(true);
  });

  it('edge case: empty triage queue (nothing pending) is valid', () => {
    expect(InboundContractEmailListResponseSchema.safeParse([]).success).toBe(true);
  });

  it('malformed: an unknown status value is rejected', () => {
    expect(InboundContractEmailSchema.safeParse({ ...matchedEmail, status: 'archived' }).success).toBe(
      false,
    );
  });

  it('malformed: matchConfidence outside the 0-1 range is rejected', () => {
    expect(
      InboundContractEmailSchema.safeParse({ ...matchedEmail, matchConfidence: 1.5 }).success,
    ).toBe(false);
  });

  it('happy path: manual triage match request', () => {
    const payload = { companyId: 'c1', rfqId: 'rfq-4821' };
    expect(InboundContractEmailMatchRequestSchema.safeParse(payload).success).toBe(true);
  });

  it('missing API response: rfqId absent on the manual match request is rejected', () => {
    expect(InboundContractEmailMatchRequestSchema.safeParse({ companyId: 'c1' }).success).toBe(
      false,
    );
  });
});

describe('Sign endpoints', () => {
  it('happy path: sign start', () => {
    const payload = { signSessionId: 'sign-1', verificationCode: '9013' };
    expect(SignStartResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('happy path: sign poll pending has no signedContract yet', () => {
    expect(SignPollResponseSchema.safeParse({ status: 'pending' }).success).toBe(true);
  });

  it('happy path: sign poll signed includes the full signed contract', () => {
    const payload = {
      status: 'signed',
      signedContract: {
        supplier: 'Ignitis',
        planType: 'fixed',
        rate: 13.9,
        expiryDate: '2028-09-01',
        penalty: 0,
        terms: [],
        contractId: 'sc-1',
        signedAt: '2026-08-13',
        startDate: '2026-09-01',
      },
    };
    expect(SignPollResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('missing API response: signed status without a signedContract payload is rejected', () => {
    expect(SignPollResponseSchema.safeParse({ status: 'signed' }).success).toBe(false);
  });

  it('edge case: failed sign status is valid with no signedContract', () => {
    expect(SignPollResponseSchema.safeParse({ status: 'failed' }).success).toBe(true);
  });
});

describe('GET /companies/:id/win-vs-spot', () => {
  it('happy path: exactly 12 months of comparison data', () => {
    const months = Array.from({ length: 12 }, (_, i) => ({
      label: `M${i + 1}`,
      fixedCost: 1000,
      spotCost: 1100,
    }));
    const payload = { fixedAnnual: 12000, spotAnnual: 13200, savings: -1200, months };
    expect(WinVsSpotResponseSchema.safeParse(payload).success).toBe(true);
  });

  it('malformed: wrong month count (13) is rejected', () => {
    const months = Array.from({ length: 13 }, (_, i) => ({
      label: `M${i + 1}`,
      fixedCost: 1000,
      spotCost: 1100,
    }));
    const payload = { fixedAnnual: 12000, spotAnnual: 13200, savings: -1200, months };
    expect(WinVsSpotResponseSchema.safeParse(payload).success).toBe(false);
  });

  it('missing API response: savings field absent is rejected', () => {
    const months = Array.from({ length: 12 }, (_, i) => ({
      label: `M${i + 1}`,
      fixedCost: 1000,
      spotCost: 1100,
    }));
    const payload = { fixedAnnual: 12000, spotAnnual: 13200, months };
    expect(WinVsSpotResponseSchema.safeParse(payload).success).toBe(false);
  });
});
