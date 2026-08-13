# Nord Pool day-ahead price feed — awaiting spec from PM

Status: **awaiting spec from PM** (`PM_ANSWERS.txt` #6 — PM will get the specifics; monthly
granularity for v1).

Referenced from `ARCH_SPEC.md` §2/§5, consumed by the spot-index calculations
(`shapeMarginPct`, `winVsSpot` in `src/calculations/`) and `ARCHITECTURE.md` §3.6. Needs: feed
provider (Nord Pool directly or a reseller), base URL/auth, and response shape for day-ahead
price per country. v1 needs monthly average €/MWh per country (per PM); the adapter should
still be shaped so hourly granularity can be added later without changing the calculation
function signatures (they already take a `spotIndex: number[]` array — a monthly feed just
means 12 entries instead of 8760).
