# Handoff: Easy Break — Energy Contract Switcher

## Overview
"Easy Break" is a micro-business energy-comparison and contract-switching tool for Baltic SMEs (Estonia, Latvia, Lithuania). A business user logs in with Smart-ID, uploads their current electricity contract, the app extracts contract terms and pulls 12 months of consumption from the national datahub, requests quotes from suppliers, and lets the user sign a new contract electronically — then shows a comparison of the new contract's cost versus a spot-tracking plan. The tool also supports managing more than one company/contract in the same session.

## About the Design Files
The bundled file, `Energy Switcher.dc.html`, is a **design reference built in HTML** — a working interactive prototype showing intended look, copy, states, and flow. It is not production code to copy directly. All "backend" behavior (contract analysis, datahub fetch, RFQ, e-signature) is **simulated** with `setTimeout` calls and hardcoded demo data — there is no real API, auth, or persistence layer behind it.

The task is to recreate this design and its interaction flow in the target codebase's actual environment (React/Vue/native, whichever the product uses), backed by real integrations, using the codebase's existing component and design-system patterns rather than this file's inline styles.

## Fidelity
**High-fidelity.** Colors, typography, spacing, copy, and interaction states in the prototype are final/intended. Recreate pixel-close, but defer to the target codebase's existing design system/components where one exists.

## Screens / Views

### 1. Landing
- Top bar: small logo (30×30 icon) + "Easy Break" wordmark (16px/700), language switcher (EN/ET/LV/LT pills) right-aligned.
- Hero, centered, max-width 640px: large logo (64px icon + 30px wordmark), two-line tagline as an `<h1>` (44px/800, line-height 1.15) — line 1 "Dump your expensive contract.", line 2 "We'll handle the exit." (per-language variants in the file's `TRANSLATIONS.tagline_l1/_l2`).
- Below the tagline: one smaller paragraph (14px, muted gray) combining a one-line product description + explanation sentence.
- Country picker: 3 pill buttons (EE/LV/LT) with striped flag swatches (2–3 color bands), select sets both country and UI language.
- Primary CTA button: "Continue with Smart-ID" — opens the Smart-ID modal (PIN reveal simulation), then routes to the Dashboard.

### 2. Dashboard ("Current contract and consumption")
Step 1 of 3. Two-column-feeling single column, max-width 920px:
- **Contract card** (white, 12px radius, 1px border): drag/drop upload zone (dashed border) → uploading spinner → parsed-contract summary (supplier, plan, rate, expiry countdown, penalty, earliest-switch date, extra terms table).
- Sequential async states after upload: "Analyzing contract…" (spinner) → "Requesting 12 months of consumption data from `<datahub>`…" (spinner) → consumption ready.
- **Consumption chart**: 12 monthly bars (kWh), peak-month bar highlighted in accent color, legend dot for "Peak load".
- **Advanced view** (toggle in top bar): reveals 3 metric rows (consumption-pattern deviation, month-to-month volatility, shape-vs-flat-load cost) each with a tooltip (`?` icon) and an on/off toggle to attach to the RFQ.
- Primary CTA "See what else is out there" appears once upload + consumption are both ready → routes to RFQ page.
- **Post-sign state** (same screen, replaces the upload card): green success banner "You're switching — new contract on its way", new-contract summary + start date, then the old contract marked "Terminated" below a divider. If the signed plan wasn't itself a spot plan, a text link "See your win vs. spot price →" appears, routing to the Win page.

### 3. RFQ Matrix
Step 2 of 3. Three sequential stages driven by one `rfqStage` state (`published` → `sending` → `received`):
- **Published stage**: badge "Default pricing" + note that these are public list prices. Table of 3 supplier rows (radio-select, supplier name + optional badge [Recommended/High volatility], plan type, published rate, computed net benefit/yr vs. current contract). CTA banner prompting to request personalized quotes. Below (inside Advanced view): consumption metrics list with per-metric include-toggle, and a collapsible "view request text ▾" showing the literal formal RFQ letter that would be sent to suppliers. Two buttons: primary "Get personal quotes" (send RFQ) and secondary "Request contract" (skip straight to sign with default pricing).
- **Sending stage**: centered spinner card, "Sending your RFQ" / "Requesting personalized quotes from 3 suppliers…".
- **Received stage**: table now has 6 rows — the 3 original default-pricing rows AND 3 new "Personal offer" rows appended (not replacing), radio-select still available across all. "Automated monthly check" toggle row (keep monitoring market after switching). Primary CTA "Request contract" → Sign page.
- Table columns: radio · Supplier (+ badge) · Plan · Rate · Net benefit/yr. Best offer gets a "Recommended" badge and highlighted row background.

### 4. Switch & Sign
Step 3 of 3, max-width 680px:
- Summary card: switching-to supplier, new plan + rate, new start date, divider, termination notice sentence (which contract/supplier is being cancelled and the penalty fee already netted into the number below), and the headline "Estimated net benefit, year one" figure (large, colored green/red by sign).
- Not-yet-signed: two buttons — primary "Sign with Smart-ID" (opens PIN modal → success) and secondary "Download contract (unsigned)".
- Signed: green confirmation banner "Contract signed" + "Download PDF" button.
- Text link back to Dashboard.

### 5. Win vs. Spot (new page)
Only reachable via the link on the post-sign Dashboard card, and only when the signed contract is a fixed-price plan (hidden if the user chose the spot-tracking supplier plan).
- Header: title + one-sentence explanation.
- Three summary cards side by side: "Your fixed-price contract" (annual €), "A typical spot-tracking plan" (annual €), "Your estimated win, this year" (delta, colored green/red).
- Grouped bar chart: 12 months, two bars per month (dark = fixed cost, accent color = spot-plan cost), with a small legend above.
- Text link back to Dashboard.

### Global chrome (all screens after landing)
Top bar, always present once `screen > 0`: logo+wordmark (left) · 3-step nav (Contract & Consumption / RFQ Matrix / Switch & Sign) with numbered circles, current step highlighted (right center) · advanced-view toggle switch · language pills · **company switcher**: once at least one company's contract upload is complete, a horizontally-scrollable row of company "chips" appears (each showing company name + annual kWh, or "New company" if still mid-upload), clicking a chip switches the active company session; a dashed "+" button after the chips starts a brand-new company flow (fresh upload/consumption/RFQ/sign state, cycling through demo company templates) and switches to it.

## Interactions & Behavior
- **Smart-ID modal**: reused for both login and contract signing. Shows a 4-digit PIN with digits revealing one at a time (350ms apart) to simulate device confirmation, then either routes to Dashboard (login) or shows a success state with a "Done" button (signing) which then auto-navigates back to the Dashboard after ~900ms.
- **Upload → analyze → fetch** is a fixed scripted sequence (uploading 1100ms → analyzing 1300ms → fetching 1500ms) in the prototype; in production this is: parse PDF → verify company registry + representative rights → extract metering-point EIC → call national datahub for 12mo consumption.
- **Send RFQ** is a fixed 1400ms delay before "received" quotes appear in the real app this is: send formal request to 3 suppliers → await/parse their replies.
- Country selection on the landing page also sets the active UI language (EE→et, LV→lv, LT→lt); the language pills allow overriding independently afterward.
- Advanced view is a single global toggle that reveals/hides the "why" layer (consumption metrics, request-letter text) throughout the dashboard and RFQ screens — default off, so the primary path is deliberately terse.
- Button prominence swaps between stages to guide the flow: e.g. "Send RFQ" is primary before quotes exist, "Request contract" becomes sole primary once the user has quotes to act on.
- All copy is fully translated for en/et/lv/lt; language switch re-renders all visible text and number/date formatting immediately (no reload).

## State Management
The prototype's state (see `Component.state` in the file) is a useful map of what a real implementation needs to persist:
- `activeCompanyId` + `companies[]` — each company session holds: `company` (name, registry code, EIC code, annual kWh), `currentContract` (supplier, plan, rate, end date, penalty), `monthsRaw` (12 months consumption), `uploadStatus` (idle/uploading/done), `consumptionStatus` (pending/analyzing/fetching/ready), `rfqStage` (published/sending/received), `selectedQuoteId`, `includeMetrics` (3 booleans), `showRequestText`, `monthlyCheck` (bool), `signCompleted` (bool).
- Global (not per-company): `screen` (landing/dashboard/rfq/sign/win), `country`, `language`, `advancedView`, Smart-ID modal state.
- In production, company/contract/consumption/RFQ/sign records should be persisted server-side per user account, not just in client state — the multi-company switcher implies one user account can own several companies.

## Design Tokens
- **Color model**: everything is generated from a single accent hue via OKLCH (`oklch(58% 0.15 <hue>)` for the accent, with derived hover/bg/text/text-strong shades at different lightness/chroma). Default accent hue 70 (`#b16600`, warm amber-brown). Neutral palette is OKLCH gray (`oklch(X% 0.006-0.02 80)`), background uses two soft radial gradients (green-ish `oklch(95% 0.035 145 / 55%)` and teal `oklch(94% 0.035 200 / 45%)`) over a near-white base `oklch(98% 0.006 80)`.
- **Typography**: Inter (400/500/600/700/800) for UI text, JetBrains Mono (400/500/700) for all numeric/data values (rates, dates, kWh, step badges). Base body text ~13–14px, page titles 28px/700, hero tagline 44px/800, small caps labels 11px/700 uppercase with 0.04–0.06em letter-spacing.
- **Radius**: 6–8px small controls, 10px buttons, 12px cards.
- **Borders**: 1px `oklch(90% 0.006 80)` default card border.
- **Spacing**: page content padding 48px/64px; card padding 24px; row gaps 8–24px depending on density.
- **Status colors**: success/green `oklch(4x% 0.14 145)`, warning/red `oklch(4x% 0.15-0.16 35)`.

## Assets
- Logo: hand-drawn penguin mark as inline SVG (two overlapping circles for body/head, white belly + eyes, accent-colored beak/feet) — no external asset file, safe to recreate as SVG or replace with a real brand mark.
- Flags: rendered as 3 stacked color stripes per country (no flag image assets) — EE blue/black/white, LV maroon/white/maroon, LT yellow/green/red.
- No photography or icon-font assets are used; all icons are inline SVG/emoji-free glyphs (✓, ↑, ›, ▾/▴).

## Files
- `Energy Switcher (standalone).html` — the full interactive prototype (all 5 screens, all 4 languages, all state logic) referenced throughout this document. Self-contained — open directly in any browser, no server needed.
