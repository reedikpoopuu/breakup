# Estonia — e-Business Register / Ariregister — spec read, endpoint host still unconfirmed

Status: **implemented against the published query spec, credentials/base URL still pending**.
`AriregisterEsindusClient` (package `com.example.demo.registry`) calls the `arireg.esindus_v1`
"rights of representation - all persons related to a company" open-data query, built 2026-08
against https://avaandmed.ariregister.rik.ee/en/open-data-api/rights-representation-all-persons-related-company
and its linked XSD (http://www2.rik.ee/schemas/xtee6/arireg/live/xroad6_esindus_v1.xsd).

Referenced from `ARCH_SPEC.md` §5, consumed by `RepresentativeRightsVerificationService` /
`RepresentationRightsController` (`POST /companies/{registryCode}/representative-rights/verify`),
implementing `ARCHITECTURE.md` §3.2.

**Confirmed from the XSD:**
- SOAP/X-Road service, target namespace `http://arireg.x-road.eu/producer/`, request root
  `esindus_v1`, response root `esindus_v1Response`.
- Auth is a plain open-data account username/password (`ariregister_kasutajanimi` /
  `ariregister_parool`) carried as SOAP body parameters, not an API key or X-Road member
  certificate.
- Request takes `ariregistri_kood` (company registry code); response nests
  `keha.ettevotjad.item[].isikud.item[]`, each person carrying `fyysilise_isiku_kood` +
  `isikukood_riik` (personal ID code + ISO 3166-1 alpha-3 country - this is the field the
  "is the logged-in personal ID visible in the list" check matches against), name, role
  (`fyysilise_isiku_roll[_tekstina]`), and `ainuesindusoigus_olemas` (exclusive right of
  representation).

**Still needed:**
- The SOAP endpoint hostname/path — RIK's documentation page describes the request/response
  shape but does not publish it; only arrives once open-data API credentials are requested from
  RIK. Set via `ARIREGISTER_BASE_URL` once known (see `AriregisterProperties`).
- Whether a `SOAPAction` header is required, and any rate limits — unstated in the docs.
- The exact name of the optional output-format field — two passes over the documentation
  disagreed (`ariregistri_valjundi_formaat` vs. `ariregister_valjundi_formaat`); left unset
  since the query defaults to XML anyway, which is what the client parses.

## Getting real credentials

RIK's abiinfo.rik.ee FAQ states plainly that **"API teenuste kasutamine eeldab lepingu
sõlmimist"** — using API services requires signing a contract. There is no self-service
signup for `esindus_v1` specifically; despite living under the "open data" heading, it is
not a no-contract public endpoint (an earlier version of this doc/the client javadoc said
otherwise — that was wrong).

To get real access:
1. Start the contract flow at `https://ariregister.rik.ee/eng/contract` ("Start creating a
   contract" → `/eng/contract/step1`), or contact support directly at `klienditugi@rik.ee`
   / +372 680 3160 (Mon–Thu 9:00–16:00, Fri 9:00–14:00) and ask specifically for open-data
   XML/SOAP API access to `esindus_v1`.
2. Check `https://ariregister.rik.ee/eng/pricelist` and/or confirm with support whether
   this specific query is free-with-contract or metered - unconfirmed as of this writing.
3. Once signed, RIK should provide the real SOAP endpoint host and the
   `ariregister_kasutajanimi`/`ariregister_parool` credentials - set these via
   `ARIREGISTER_BASE_URL` / `ARIREGISTER_USERNAME` / `ARIREGISTER_PASSWORD`.
