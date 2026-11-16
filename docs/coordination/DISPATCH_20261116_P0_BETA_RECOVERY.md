# FamilyPoints — Coordinated P0 delivery dispatch (2026-11-16)

**Reference GitHub HEAD:** `e3bf7b975` (2026-11-04). **CI:** [41813780037](https://github.com/o-kobylianskyi/family-points/actions/runs/41813780037): Maven/PostgreSQL PASS; Web/Vite PASS; Compose PASS; Android FAIL (98 tests, 3 failures). **No newer main commits as checked November 16.**
**User's direction:** coordinate maximal cohesive batches; synthetic/disposable PostgreSQL and GitHub CI first, **live/device smoke deferred**. Never operate on existing family data; no production BETA deployment without separate authorization.

## Release-critical sequence, parallelism and accountability

### Track A — Android P0 / issue #19, owner Android; QA consumer
**Deliverable** one coherent bug-fix + regression evidence packet for 3 CI red cases:
- TemplateReceiptRepositoryHttpTest.lostActivationResponseRecoversWithOriginalIdAndNoRepost
- OfflinePointsReplayHttpTest.lostResponseKeepsOriginalIdForExplicitIdempotentReconciliation
- TemplateReceiptHttpFaultTest.lostActivationPostResponseRecoveredOnlyByGetWithSameUuid.
- Collect exact test XML/cause; deterministic fixture, stable UUID and GET receipt reconciliation, forbid hidden duplicate POST or relaxation of scopes. Rerun JVM/assemble and new GitHub Actions on exact SHA; document failures honestly.
- Android reward-history UI already committed, purchase-only v1; not reason to block CI fix for new event features.

### Track B — Backend BKL-002 P0 / issue #15, QA/Release/Web consumers
**Deliverable** smallest complete executable FULL/SELECTIVE family backup/reset/restore slice compliant with approved [ADR-FAMILY-RESET-002](../../architecture/ADR-FAMILY-RESET-002.md), instead of further isolated test-only projections.
- Publish actual unsupported category matrix / FK closure; target disposable family only, fail FULL if unsupported; no silent co-clears or account/audit changes.
- Ship secure explicit family owner binding and legacy fail-closed, fresh reauth including Google-only strategy and user-confirmed family name; idempotent operation UUID/durable receipt; transaction locks, epoch and error codes.
- Real encrypted **family-scoped** export/download, upload/verify/preview, FULL/SELECTIVE replace-selected, automatic verified recoverable protective snapshot before destructive operation, server-local 30-day physical TTL, quota/key strategy. No off-host requirement; no whole-DB pg_dump via family UI.
- One-transaction native restore currently confirmed only in disposable test for member groups, media assets/bindings, private templates, work folders, and newer definition remap source. This is **not** production HTTP execute. Record exact PostgreSQL/Flyway schema path for task/reward history, ledger, purchases, obligations and settings. If not supported, return blocked reason; do not claim FULL.
- Backend result must include exact API DTO, response status, tested migration/source SHA, Testcontainers test counts and rollback/neighbor-tenant proof; do not enable existing `reset/execute` 423 until safety gates pass.

### Track C — Onboarding Web P0 / issue #18; Backend contracts, Designer, QA
- Validate 7-step first family wizard: explicit groups (member groups vs WorkNode folders), curated 142-entry TASK/REWARD starter catalog and six packs, preview/import/activation, resume, zero selections, duplicate/retry receipts V47/V48/V49, localized UI (UA/RU/DE/EN).
- Run exact SHA frontend build/unit tests, Playwright with authenticated disposable family and permissions; distinguish missing QA credentials SKIPPED from PASS. Do not auto-create workspace or activate templates by navigating.
- BKL-002 Web dangerous actions remain disabled pending Backend execute API. Reward history v1 Web/Android done in source, leave full lifecycle as independent P1; do not distract P0.

### Track D — QA P0, then Release
- QA stage 1 **now**, parallel: CI triage, Android 3 test proofs, read-only Web onboarding/reward-history, cross-tenant/epoch, current-head auth-gated Playwright when credentials available, isolated PostgreSQL negative suite. No real-family writes.
- QA stage 2 **only after Backend safe endpoints**: fresh disposable family automatic backup → selective/full reset → file restore → onboarding import/activate → ledger reconciliation, multiple members, currency, media, auth owner proof, expired/wrong key, forced transaction rollback, missing backup, idempotency, user-visible errors, original UUID and retained audit; report exact SHA, runs and outcomes.
- Release must pin **single all-green** GitHub CI SHA and verified restoration evidence. Local red/green reports at other SHAs do not count. No scheduling claims; release stays HOLD.

## Gate scorecard — update on evidence not estimated percentages
| Gate | Present state | Accountable |
|---|---|---|
| Architecture policy (bounded) | ACCEPTED; ADR-FAMILY-RESET-002 | Architecture |
| Full BKL-002 owner/reauth/verified snapshot/HTTP execute | NOT ACCEPTED, 423 | Backend |
| Issue #18 wizard acceptance | OPEN, full authenticated E2E outstanding | Web/QA |
| Android CI lost-response 3 cases | FAIL on 41813780037 | Android |
| Four-job same-SHA CI | FAIL | All/Release |
| Real data wipe / device smoke | NOT AUTHORIZED / DEFERRED | Release/User |
| BETA | HOLD | Release |

## Reporting contract for every track
In existing owner logs or issue comments provide **HEAD SHA / files changed / tests actually run / pass-fail-skip counts / newly found blockers / next concrete deliverable**. Do not report source work as verified execution. Avoid meaningless near-100% progress estimates and avoid repeated cosmetic micro-commits; prefer one integrated package per owner.

## Deferred backlog guard
- Reward history: purchase-only v1 in Backend/Web/Android; full REQUEST/PURCHASE/OBLIGATION/delivery event model P1 after P0.
- Issue #16: configurable punishment/restriction templates, time/task unlock and app/device policy P2 after BETA; managed Android OS app blocking requires separate BKL-010 capability.
- All cross-track updates happen via GitHub docs/issues; other ChatGPT role chats do not automatically receive a notification from this file. Owners must pick up and acknowledge.

**Acceptance:** only verified source + independent tests unlock dependent gate. This dispatch does not authorize destructive commands or real family changes.
