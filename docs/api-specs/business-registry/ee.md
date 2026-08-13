# Estonia — e-Business Register / Ariregister — awaiting spec from PM

Status: **awaiting spec from PM** (`PM_ANSWERS.txt` #4 — local business registry has an API;
mock until credentials/spec are supplied).

Referenced from `ARCH_SPEC.md` §5, consumed by the representative-rights verification service in
`ARCHITECTURE.md` §3.2 (`POST /companies/:id/representative-rights/verify`). Needs: base URL,
auth, company-lookup-by-registry-code call, and how representative rights (board member,
procurator, etc.) are expressed in the response.
