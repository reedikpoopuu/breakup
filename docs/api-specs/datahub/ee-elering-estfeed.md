# Estonia — Elering Estfeed — awaiting spec from PM

Status: **awaiting spec from PM** (`PM_ANSWERS.txt` #3 — local datahub has an API; mock until
credentials/spec are supplied).

Referenced from `ARCH_SPEC.md` §5, consumed by the datahub adapter in `ARCHITECTURE.md` §3.4.
Needs: base URL, auth (API key / mTLS / OAuth — unconfirmed), the metering-point (EIC) lookup
call, and the 12-month consumption pull call/response shape (target: monthly kWh per metering
point, per `ARCH_SPEC.md` §2). Mock adapters should return `ConsumptionMonth[12]` matching the
shape in `src/types.ts`.
