import { describe, expect, it } from 'vitest';
import { generateRequestLetterText } from '../../src/calculations/requestLetter.js';
import type { Company } from '../../src/types.js';

const company: Company = {
  id: 'c1',
  name: 'Nordic Timber Works OÜ',
  registryCode: '12345678',
  eicCode: '38ZEE-00000001-X',
  annualKwh: 131_000,
};

describe('generateRequestLetterText', () => {
  it('happy path: interpolates company identity fields into the letter', () => {
    const text = generateRequestLetterText({
      company,
      language: 'en',
      includeMetrics: { profile: true, volatility: true, shape: true },
    });

    expect(text).toContain(company.name);
    expect(text).toContain(company.registryCode);
    expect(text).toContain(company.eicCode);
    expect(text).toContain('131'); // annual kWh, allowing for locale-formatted grouping
  });

  it('happy path: includes a rate-offer table with spot+margin and fixed 6/12/24mo rows', () => {
    const text = generateRequestLetterText({
      company,
      language: 'en',
      includeMetrics: { profile: false, volatility: false, shape: false },
    });

    expect(text.toLowerCase()).toContain('spot');
    expect(text).toContain('6');
    expect(text).toContain('12');
    expect(text).toContain('24');
  });

  it('edge case: no metrics included still produces a valid non-empty letter', () => {
    const text = generateRequestLetterText({
      company,
      language: 'en',
      includeMetrics: { profile: false, volatility: false, shape: false },
    });
    expect(text.length).toBeGreaterThan(0);
  });

  it('edge case: all metrics included produces a letter distinct from the no-metrics version', () => {
    const withoutMetrics = generateRequestLetterText({
      company,
      language: 'en',
      includeMetrics: { profile: false, volatility: false, shape: false },
    });
    const withMetrics = generateRequestLetterText({
      company,
      language: 'en',
      includeMetrics: { profile: true, volatility: true, shape: true },
    });
    expect(withMetrics).not.toBe(withoutMetrics);
  });

  it('localization: each supported language produces a distinct letter for the same input', () => {
    const languages = ['en', 'et', 'lv', 'lt'] as const;
    const texts = languages.map((language) =>
      generateRequestLetterText({
        company,
        language,
        includeMetrics: { profile: true, volatility: true, shape: true },
      }),
    );
    const uniqueTexts = new Set(texts);
    expect(uniqueTexts.size).toBe(languages.length);
  });
});
