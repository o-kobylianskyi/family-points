# QA P0 integrated gate — Issue18 activation loss-response + BKL-002 restore
Date 2026-11-01. Owner QA. Baseline local browser acceptance **41 PASS / 1 SKIPPED / 0 FAIL** (operator reported), exact SHA not supplied. This is a test design and source audit, not new PASS evidence.

## Critical findings from latest source
Web `CreateTaskModal.jsx` has `unknownWrite`, submission lock, and distinguishes POST success from subsequent list refresh errors. Web `RewardsPage.jsx` has `rewardWriteUnknown`; both seek to prevent blind second POST after an uncertain network outcome (source commits 4abe976,6ead045,9ee058,eee23eb). **New changes were committed after user-reported green browser batch** and still require their own synthetic acceptance. Do not infer durable server receipt or idempotent create from client-side locks.
Backend now validates encrypted MEDIA_BINDINGS identity and tenant in `ProtectiveBackupImportInspector`; Testcontainers negative matrix `WorkspacePortableMediaBindingPreflightPostgresTest` exists and Backend reports user local targeted Maven BUILD SUCCESS, but exact HEAD+CI not certified.
Test-only native committed restore and rollback-only production rehearsal **are not production FULL/SELECTIVE Restore Execute**.

## Synthetic loss-response suite matrix (no persistent writes)
| ID | Flow | Fault injection | Acceptance |
|---|---|---|---|
| A01 | Task create | First POST reaches mocked API, then response disconnects | Exactly one POST, Save disabled until safe reconciliation |
| A02 | Task create | POST 201, follow-up GET 503 | Do not send second POST; committed operation must not be labeled unequivocal failure |
| A03 | Task create | Double click / Enter while pending | Exactly one request; no second submit |
| A04 | Task create | Template request pending | Save unavailable until verified template selection |
| A05 | Task create | Workspace A→B while POST pending | A response never writes B view; no implicit B POST |
| A06 | Task create | Auth/account switch while pending | No stale reconciliation into new account or automatic retry |
| A07 | Reward create | POST response interrupted | Exactly one POST, disabled uncertain form, explicit reconcile |
| A08 | Reward create | POST 201 then list GET 503 | No duplicate reward create; report confirmed commit distinctly |
| A09 | Reward create | Double click during pending | Exactly one POST |
| A10 | Reward create | Workspace/account switch pending | No stale response side effects or cross-family mutation |
| A11 | Task+reward | Duplicate server POST on same idempotency key | Server must produce same durable receipt or explicit 409, not duplicate domain rows |
| A12 | Task+reward | Offline/reload after unknown POST | User can recover status by authoritative server GET/receipt, not blind resubmit |
| A13 | Onboarding | Import preview stale from second tab | No import POST until fresh matching preview |
| A14 | Onboarding | Import commits but response lost | Read private templates, reconcile exact catalog codes; manual retry cannot duplicate |
| A15 | Onboarding | Group creation response lost | GET duplicate reconciliation before another create |
| A16 | Onboarding | Template activation | Import alone never creates active TaskDefinition/RewardDefinition, ledger or purchase; activation is explicit and correctly mapped |
| A17 | ACL | Child/import/group/activation across family | API denies writes; UI not sole security barrier |
| A18 | Reset safety | Browsing or opening modal | Never issue `data-reset/prepare|challenge|confirm|execute`, restore apply, or file upload |

**Fixture rule:** A01–A10 require synthetic `page.route` HTTP stubs or a truly disposable isolated workspace with rollback; do not exercise create POST on Test 14 / Test2 11 or Beta. Never embed real credentials in test or logs. Do not count A11/A12 PASS without Backend receipts and server idempotency.

## Restore integration (disposable Testcontainers only)
B01 complete exported encrypted archive loaded from file and authenticated; B02 FULL domain reconstruction with FK remapping and media bytes; B03 SELECTIVE restore with unselected-category row digests unchanged; B04 financial ledger/balance invariants and currency mapping; B05 cross-tenant owner/global/shared account preserved; B06 malformed/tampered media binding/preflight rejected prior to any transaction; B07 injected mid-transaction failure all-or-nothing; B08 retries UUID/fingerprint receipt; B09 stale epoch; B10 protective snapshot empirically restore-tested before any reset; B11 approved 30-day local TTL and exported user file unaffected; B12 401/403/fresh reauth/locked 423 enforcement. Test-only TEMP staging and slice committed SQL are partial evidence, **not B02/B03**.

## Release evidence packet and decision
- Pin `git rev-parse HEAD`, run the exact seven-class PostgreSQL suite plus `WorkspacePortableMediaBindingPreflightPostgresTest` and `DisposableComposedNativeRestorePostgresTest`; capture Surefire counts and failures.
- `cd qa/e2e && npx playwright test`; existing 41 PASS/1 SKIP belongs to earlier local snapshot and does not certify newest task/reward create changes.
- For A01–A10 implement selector/route tests only after API payload/locator inspection; mock at network boundary, prohibit any outbound unmocked POST. Do not manufacture a green suite by skipping missing selectors.
- Four same-SHA cloud jobs must finish PASS; prior cancelled/queued is not green.
- Block release if durable creation receipt, full restore apply, architect policy approval, snapshot recoverability, or exact-head CI is missing. Reset Execute remains HTTP 423.
