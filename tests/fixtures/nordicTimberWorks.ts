// Golden fixture per ARCH_SPEC.md §4: "the calculation functions in §2 are pure and should get
// unit tests with the prototype's own hardcoded demo values (Nordic Timber Works OÜ, 131,000
// kWh/yr, rate 15.6, penalty €410) as fixtures/golden values". The monthly breakdown, quote
// rates, and spot-index curve below are this suite's own choices (not stated in ARCH_SPEC),
// chosen to sum exactly to the spec's 131,000 kWh/yr annual figure; expected outputs were
// derived independently from the §2 formulas, not from the implementation under test.

export const ANNUAL_KWH = 131_000;
export const CURRENT_RATE = 15.6; // c/kWh
export const PENALTY = 410; // EUR

// Winter-heavy monthly profile, sums to exactly 131,000 kWh.
export const MONTHLY_KWH = [
  15700, 14400, 12700, 10200, 7900, 6400, 6000, 6300, 8500, 11000, 13800, 18100,
];

export const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

// ARCH_SPEC.md §2 — prototype's hardcoded "typical Baltic SME load profile" benchmark (placeholder).
export const BENCHMARK_SHARES = [10, 9.3, 8.6, 7.8, 7.0, 6.4, 6.1, 6.4, 7.3, 8.3, 9.3, 10.2];

// ARCH_SPEC.md §2 — prototype's hardcoded synthetic seasonal spot-index curve (placeholder).
export const SPOT_INDEX = [1.25, 1.20, 1.05, 0.95, 0.85, 0.80, 0.78, 0.82, 0.90, 1.00, 1.15, 1.28];

export const QUOTE_RATE_BETTER = 13.9; // c/kWh — a candidate cheaper than the current contract
export const QUOTE_RATE_WORSE = 16.8; // c/kWh — a candidate more expensive than the current contract

// Expected values below were computed by hand from the ARCH_SPEC.md §2 formulas against the
// fixture data above (see PR description / commit for the derivation), independent of any
// implementation, so they serve as this suite's ground truth once the calculations are built.
export const EXPECTED = {
  netBenefitBetter: 1817,
  netBenefitWorse: -1982,
  netBenefitNoPenalty: 2227,
  profileDeviationPct: 8.0698,
  volatilityPct: 35.4241,
  shapeMarginPct: 6.0699,
  winVsSpot: {
    fixedAnnual: 18209.0,
    spotAnnual: 21121.87,
    savings: -2912.87,
    months: [
      { fixedCost: 2182.3, spotCost: 2912.35 },
      { fixedCost: 2001.6, spotCost: 2577.6 },
      { fixedCost: 1765.3, spotCost: 2025.65 },
      { fixedCost: 1417.8, spotCost: 1494.3 },
      { fixedCost: 1098.1, spotCost: 1054.65 },
      { fixedCost: 889.6, spotCost: 812.8 },
      { fixedCost: 834.0, spotCost: 746.4 },
      { fixedCost: 875.7, spotCost: 816.48 },
      { fixedCost: 1181.5, spotCost: 1190.0 },
      { fixedCost: 1529.0, spotCost: 1683.0 },
      { fixedCost: 1918.2, spotCost: 2380.5 },
      { fixedCost: 2515.9, spotCost: 3428.14 },
    ],
  },
} as const;
