# Architecture: Easy Break — Energy Contract Switcher

Companion to `README.md` (UI/flow spec) and `Energy Switcher (standalone).html` (prototype). This document sketches the system behind the UI — for a developer (or Claude Code) to implement in a real stack.

## 1. Actors & core flow
- **User** — an owner/employee of one or more SMEs, authenticated via Smart-ID/Mobile-ID.
- **Company** — a legal entity the user represents (verified via business registry).
- **Supplier** — an electricity retailer who receives RFQs and returns quotes.
- **Datahub** — the national metering-data platform (Elering Estfeed in EE, AST Datahub in LV, ESO/Litgrid in LT).

Flow: authenticate → select/verify company → upload current contract → extract terms + representative rights → pull 12mo consumption from datahub → request quotes from suppliers → user picks a quote → e-sign new contract → old contract terminated, new one active → ongoing monthly market monitoring.

## 2. Suggested stack
- **Frontend**: React (TypeScript) — SPA or Next.js if SSR/SEO on the landing page matters. i18n via a library (react-intl / i18next) rather than the prototype's inline TRANSLATIONS object.
- **Backend**: Node.js (NestJS) or similar typed service framework — this domain is mostly orchestration of external integrations + a signing workflow, not heavy compute.
- **Database**: PostgreSQL (relational fits the entities below well; contract/quote history benefits from real joins and constraints).
- **File storage**: S3-compatible object storage for uploaded contract PDFs and signed output PDFs.
- **Async jobs**: a queue (BullMQ/SQS) for the multi-step, multi-minute-to-multi-day flows (document parsing, datahub fetch, RFQ round-trips, monthly monitoring) — these should not be modeled as synchronous request/response.
- **Document parsing**: a document-extraction pipeline (Claude via the Files/PDF API, or a dedicated OCR+LLM extraction service) to pull price, expiry, penalty, and supplier name from an uploaded PDF into structured fields.

## 3. Core services / domains

### 3.1 Identity & Auth
- Smart-ID / Mobile-ID (Estonia), Smart-ID also covers LV/LT; consider eIDAS-node fallback for other EU eIDs.
- Session issues a signed JWT; no passwords.
- Out of scope for v1 but note: multi-user companies (more than one employee representing the same company) will need role/permission handling later.

### 3.2 Company & representative rights
- On first mention of a company (via contract upload or manual entry), call the national business registry API (e.g. Estonian e-Business Register / Ariregister) with the extracted registry code to confirm:
  - the company exists and is active,
  - the authenticated person has a registered right of representation (board member, procurator, etc.).
- This replaces a manual power-of-attorney step. Store the verification result + timestamp + source reference (audit trail — this is a legal claim, not just UX).
- Represents the gate before any datahub call or RFQ can be sent "on the user's behalf."

### 3.3 Contract analysis
- Accepts a PDF upload, stores it in object storage, and runs extraction to produce: supplier, plan name, rate (c/kWh or fixed fee structure), contract type (fixed/spot+margin), start/end date, early-termination penalty formula, and the metering point identifier (EIC code) if present in the contract — otherwise resolved via the registry + datahub metering-point lookup.
- Extraction confidence should be surfaced — low-confidence fields should prompt the user to confirm/correct rather than silently trusting an LLM extraction for a legal figure like a penalty fee.

### 3.4 Datahub integration (per-country adapter)
- One adapter interface, three implementations (Elering Estfeed / AST / ESO+Litgrid) — each has different auth (API keys, mTLS, or OAuth) and data formats.
- Requires the metering point EIC + confirmed representative rights before querying.
- Pulls 12 months of hourly or monthly consumption; store as time-series (see data model) rather than just the 12 monthly totals the prototype uses, so later features (e.g. more granular spot-cost modeling) aren't blocked.

### 3.5 RFQ & supplier integration
- v1 can be semi-manual: generate the formal request-letter text (as the prototype does), and either email it to supplier contacts or push it into their submission portal/API where one exists.
- Track RFQ status per supplier (sent/awaiting/received/declined) with a deadline (the prototype's "reply within 5 business days").
- Supplier quotes come back with a rate + plan type; store them tied to the RFQ, not overwriting the "published" default-pricing snapshot — the UI shows both.
- Longer-term: formal API partnerships with suppliers remove the manual email loop.

### 3.6 Spot-price reference data
- The "win vs. spot" page needs a real market-price feed (Nord Pool day-ahead prices for EE/LV/LT) rather than the prototype's synthetic index, to compute a believable, defensible fixed-vs-spot comparison using the user's actual hourly consumption.

### 3.7 E-signature & contract execution
- Generate the final contract document (merge selected quote + company + consumption data into a supplier's contract template, or receive a supplier-issued contract to counter-sign).
- Route through Smart-ID/Mobile-ID digital signing (qualified electronic signature, eIDAS-compliant) — same identity provider as login, different flow (signing challenge).
- On completion: mark the old contract "terminated" (with penalty amount recorded), mark new contract "active" with start date, store the signed PDF.
- Automated monthly check (toggle in RFQ page) should schedule a recurring job that re-runs the RFQ comparison in the background and notifies the user if the market has moved.

### 3.8 Multi-company support
- A user account can own/represent N companies. Each company carries its own contract, consumption, RFQ, and sign state, entirely independent (mirrors the prototype's per-company session model).
- Company switcher in the UI should be backed by a `GET /companies` list scoped to the authenticated user, not client-only state.

## 4. Data model (core entities)
- **User** — id, auth identity (Smart-ID subject), name, contact email.
- **Company** — id, owner_user_id, name, registry_code, eic_code, country, annual_kwh (derived), representative_rights_verified_at, representative_rights_source.
- **Contract** — id, company_id, supplier, plan_name, plan_type (fixed/spot+margin), rate, start_date, end_date, penalty_terms (structured), status (active/terminated/pending), source_pdf_url.
- **ConsumptionRecord** — id, company_id, interval_start, interval_end, kwh, granularity (hourly/monthly), source (datahub name).
- **Rfq** — id, company_id, status (published/sending/received), sent_at, requested_supplier_ids, attached_metric_flags.
- **Quote** — id, rfq_id, supplier, plan_type, rate, is_personalized (bool), received_at.
- **SignedContract** — id, company_id, quote_id, signed_pdf_url, signed_at, terminates_contract_id.
- **AuditEvent** — id, company_id, actor_user_id, action, payload, created_at — for the "we acted on your behalf" trail (registry checks, RFQ sends, signing).

## 5. API surface (sketch)
```
POST   /auth/smartid/start | /auth/smartid/poll
GET    /companies
POST   /companies/:id/contracts/upload         → triggers async extraction job
GET    /companies/:id/contracts/current
POST   /companies/:id/representative-rights/verify
POST   /companies/:id/consumption/fetch         → triggers async datahub job
GET    /companies/:id/consumption
POST   /companies/:id/rfq                       → creates RFQ, sends to suppliers
GET    /companies/:id/rfq/:rfqId/quotes
POST   /companies/:id/contracts/:quoteId/sign/start | /sign/complete
GET    /companies/:id/win-vs-spot
```
Long-running steps (upload→extraction, RFQ send→receive, datahub fetch) should be async: return a job/status id immediately, client polls or receives a websocket/SSE push — matching the "analyzing… / fetching… / sending…" states already designed in the UI.

## 6. Security & compliance notes
- Representative-rights verification and RFQ/sign actions taken "on the user's behalf" are legally consequential — log them (see AuditEvent) and consider requiring re-confirmation for high-stakes actions (signing) even though rights were verified earlier in the session.
- Contract PDFs and signed contracts contain commercially sensitive data — encrypt at rest, restrict access to the owning company's users.
- GDPR: consumption data and company registry data are personal/business data under EU rules — define retention and deletion policy, especially after a company is removed from the platform.
- E-signatures must meet eIDAS qualified/advanced signature requirements to be legally binding for contract termination and formation.

## 7. Suggested build order
1. Auth (Smart-ID) + Company model + registry/representative-rights verification.
2. Datahub adapters (start with Estonia/Elering, the best-documented API) + consumption storage.
3. Contract upload + extraction pipeline.
4. RFQ flow (can ship semi-manual — generate the letter, send by email, manual quote entry — before full supplier API integration).
5. E-signature + contract lifecycle (terminate old, activate new).
6. Win-vs-spot using real Nord Pool price data.
7. Multi-company support, monthly auto-check job, notifications.
