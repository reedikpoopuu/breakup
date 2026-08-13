# Lithuania — ESO / Litgrid — awaiting spec from PM

Status: **awaiting spec from PM** (`PM_ANSWERS.txt` #3 — local datahub has an API; mock until
credentials/spec are supplied).

Referenced from `ARCH_SPEC.md` §5, consumed by the datahub adapter in `ARCHITECTURE.md` §3.4.
Needs: base URL, auth mechanism, metering-point lookup, and 12-month consumption pull
call/response shape. Mock adapters should return `ConsumptionMonth[12]` matching the shape in
`src/types.ts`.
