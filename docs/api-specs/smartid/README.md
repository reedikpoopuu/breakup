# Smart-ID / Mobile-ID — awaiting spec from PM

Status: **awaiting spec from PM** (`PM_ANSWERS.txt` #2 — PM is applying for test/production
credentials; mock the flow until they arrive).

Referenced from `ARCH_SPEC.md` §5. Used for both login (`POST /auth/smartid/start` +
`GET /auth/smartid/poll/:sessionId`) and contract signing (`POST /companies/:id/sign/start` +
`GET /companies/:id/sign/poll/:signSessionId`) — same provider, different challenge type.

When the real spec arrives, document here: relying-party provider/SDK, environment base URLs
(demo vs prod), auth mechanism (relying-party UUID/name + certificate), the poll/callback shape,
and whether the signing flow uses a qualified/advanced eIDAS signature product from the same
vendor. Until then, adapters should implement against a mock that mirrors the prototype's
staged flow: session start → poll returns `pending` → poll returns `confirmed`/`signed`.
