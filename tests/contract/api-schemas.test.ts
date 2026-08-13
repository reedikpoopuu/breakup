// Contract tests validating example API payloads against the Zod schemas derived from
// ARCH_SPEC.md §3. Since no backend exists yet (greenfield build, per PM_ANSWERS.txt), these
// tests exercise the schemas directly against fixture JSON rather than a live/mocked HTTP layer
// — they are the executable version of the API contract the backend must satisfy, and they
// double as documentation of which fields are optional/nullable vs required.
import { describe, expect, it } from 'vitest';
import {
  CompaniesListResponseSchema,
  CompanyDetailSchema,
  ConsumptionFetchPollResponseSchema,
  ConsumptionFetchStartResponseSchema,
  ConsumptionResponseSchema,
  CurrentContractResponseSchema,
  RepresentativeRightsResponseSchema,
  RfqCreateResponseSchema,
  RfqGetResponseSchema,
  RfqSendResponseSchema,
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
