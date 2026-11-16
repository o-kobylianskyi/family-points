# BKL-002 — independent Backend/QA queue and control snapshot, 2026-11-16 20:40 CET

**Owner decision:** Backend develops independent non-destructive P0 packages continuously; QA validates asynchronously in isolated PostgreSQL and returns consolidated failures; Release remains a separate gate. Root AGENTS.md and [operating protocol](requests/BKL002_BACKEND_QA_ASYNC_TEST_PIPELINE_20261116.md). **No automatic role-chat notifications**.

## Evidence status (do not imply QA independent PASS from a committed test)
| Packet | Backend deliverable | Source status | Independent QA acceptance |
|---|---|---|---|
| [BKL002-QA-01](requests/BKL002_QA_PACKET_20261116_BACKEND_RECOVERY_LEDGER.md) | V51/V52 staging, recoverability rehearsal, two-DB group/media/template, financial closure negative | READY_FOR_QA; historical operator local suites reported 20/20 and 10/10 on unpinned checkout | **PENDING exact-SHA QA record** |
| [BKL002-QA-02](requests/BKL002_QA_PACKET_20261116_GROUP_LEDGER_ARCHIVE.md) | Authenticated scoped group ledger source archive, non-replayable stop | READY_FOR_QA; producer NOT_RUN | **PENDING** |
| [BKL002-QA-03](requests/BKL002_QA_PACKET_20261116_GROUP_LEDGER_NATIVE_REPLAY.md) | Test-only independent PostgreSQL native +15/-4 ledger remap; tenant preservation | READY_FOR_QA; producer NOT_RUN | **PENDING** |
| [BKL002-QA-04](requests/BKL002_QA_PACKET_20261116_POLYMORPHIC_LEDGER_SOURCES.md) | Reject unmapped source_id and foreign currency | READY_FOR_QA; producer NOT_RUN | **PENDING** |
| Next Backend packet | Foreign-family task/purchase/request relation denial, reward-request source/member/currency blockers in commits fac5cb250/94fdb65b/c79658093 | Source committed; tests/QA coverage being developed | **NOT HANDED OFF / NOT ACCEPTED** |

## QA P0 intake instruction
- Read all four packets and issue #15; do **not** ask user to rerun each small Backend source patch. Consolidate overlapping suites into one pinned-SHA Maven/Testcontainers batch on explicitly isolated synthetic PostgreSQL. Deduplicate test classes and record real totals per packet. Do not test BETA/PROD/Test/Test2 destructively.
- For each packet update READY_FOR_QA → IN_QA → PASS / FAIL / BLOCKED_ENV / STALE_SHA with exact checkout SHA, started/finished date, test command, counted tests, evidence link, root cause and Backend remediation commit. Do not mark PASS if unexecuted or if CI merely skips a heavy job.
- On error, comment to #15 with severity, owning file/test and reproducible isolated fixture; return one consolidated batch, and retest safety neighbors. Feedback may be posted while Backend proceeds with unaffected work.
- The 4 packet files are unchanged historical scopes: if newest Backend contracts supersede them, label STALE_SHA and request a QA delta rather than silently applying obsolete assumptions.

## Backend P0 control
- Continue source remapping for TASK/REWARD, purchases, requests, settlements, currency/ledger and independent replay; keep current unsafe financial/native restore **disabled** until supported categories and verified evidence are real.
- Before next handoff provide at least compile + affected fast tests wherever tooling is actually available; otherwise mark NOT_RUN and have QA prioritize compile gate. The producer must not rely solely on source diff for acceptance.
- Read incoming QA FAILs at next pickup. Security/tenant isolation/financial corruption/blockers to unsafe operation take immediate precedence. Preserve 423 Reset Execute and no VERIFIED promotion.
- Publish fifth packet **only when bounded** with current SHA/negative cases and without duplicating existing QA commands.

## CI / release evidence
- Newly modified CI now emits per-commit status report, and full exact-SHA gate only if all four components actually execute. A SUCCESS run with selected jobs skipped **is not a complete validation**.
- At inspection run [43941729286](https://github.com/o-kobylianskyi/family-points/actions/runs/43941729286) for c79658093 was **in progress**, Backend and Android unfinished, Web and Compose PASS. Do not report it green.
- Onboarding #18 still needs **real enabled-server seven-step** E2E, not synthetic-only browser test.
- **Release HOLD.** FULL/SELECTIVE destructive Reset/Restore and archive VERIFIED gate not approved. No production or user-family data mutations.

## Next control checkpoint
QA posts receipt of four packets and one consolidated run matrix; Backend posts next source remapping packet and fixes any P0 FAIL; release coordinator records one exact-SHA full CI (no skipped actual tests). Update this control file with evidence, not solely chat messages.


## QA evidence update — 2026-11-16 after 20:38 CET

- Operator independent consolidated 13-class Java21/Testcontainers run **27 tests / 0 failures / 0 errors / 0 skips / BUILD SUCCESS** at **SHA `419efb1fdcab849480e790449c791c86ce004695`**, pre- and post-run HEAD equality checked. The local run covers source/test scopes of packets **#01 recovery, #02 encrypted group ledger source, #03 test-only manual group ledger cross-DB replay**, not full financial recovery/production release. QA log commit `2a32c9b` captures details. These three packets **PASS for bounded test acceptance on 419efb1**; any later source deltas remain unverified.
- Packet **#04** `BKL002_QA_PACKET_20261116_POLYMORPHIC_LEDGER_SOURCES.md` and packet **#05** `BKL002_QA_PACKET_20261116_REWARD_TASK_FK_CLOSURE.md` are now READY_FOR_QA, **NOT_RUN by QA at latest HEAD**. These introduce source_type/source_id historical remap blockers, foreign family point currency denial, task execution/reward purchase/request foreign FK validation and nullable reward-definition semantics. They postdate accepted SHA `419efb1`.
- Queue: #01-03 `PASS@419efb1 (bounded; latest-source delta STALE_SHA)`; #04-05 `IN_QA (await consolidated isolated PG run)`. Never rewrite those as feature-complete financial restore.
- Next exact-SHA QA single consolidated command should deduplicate 15 classes: `WorkspacePortableArchiveForeignRelationTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePortableRestorePreviewPostgresTest,DisposableCrossDatabasePortableRestorePostgresTest,DisposableComposedNativeRestorePostgresTest,WorkspaceBackupRehearsalAssessmentServiceTest,WorkspaceDisposableRestoreRehearsalPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupArtifactRegistryPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest`; preserve zero skipped/negative-case assertions and compare pre-/post-test SHA. Add staging/local storage if release-candidate gate requires full regression.
- CI [43941729286](https://github.com/o-kobylianskyi/family-points/actions/runs/43941729286) inspected: changes/Web/Compose SUCCESS, Java/Android IN_PROGRESS. No all-component-green claim. BETA HOLD, production reset HTTP 423, durable VERIFIED and financial native restore still unavailable.


## QA checkpoint — 2026-11-16, Backend packets #04–#09 consolidated

- Formal #09 [reward category/source link graph](requests/BKL002_QA_PACKET_20261116_REWARD_CATEGORY_PORTABLE.md) READY_FOR_QA; exporter + inspector composite association FK, cross-family and duplicate pair tests source committed. Prior #08 portable requirements, #06 obligations and #07 original unexported category links have evolved since early handoffs: current source permits bounded export but **does not** permit native financial restore. Evaluate superseded negative assertions against #08/#09 contracts, do not mechanically expect the old unconditional unexported reason.
- **Unchanged prior independent acceptance:** #01–03 27/27 PASS on pinned `419efb1fdcab849480e790449c791c86ce004695`; current #04–09 have no operator Maven counts at latest SHA. Status `IN_QA (await consolidated pinned automated run)`; no false PASS.
- **Deduplicated next Maven Testcontainers regression inventory** (synthetic/disposable only): `WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePortableArchiveForeignRelationTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest,WorkspacePortableRestorePreviewPostgresTest,DisposableCrossDatabasePortableRestorePostgresTest,DisposableComposedNativeRestorePostgresTest,WorkspaceBackupRehearsalAssessmentServiceTest,WorkspaceDisposableRestoreRehearsalPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupArtifactRegistryPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest`. These 16 classes preserve latest #09 target eight classes and older isolated restore/provenance neighbors. Require actual tests run / failures / errors / skips, exact checkout HEAD pre/post and branch movement status; only executed cases accepted.
- User explicitly defers *manual/real tests* until Backend stabilizes and QA has one cross-track PRE-BETA plan. This does **not** prevent isolated automated regression. Real backlog [PRE_BETA_REAL_WORLD_TEST_INBOX.md](../qa/PRE_BETA_REAL_WORLD_TEST_INBOX.md) includes #09 and latest Web epoch/filter/retry cases. No real-family writes.
- Release HOLD, `reset/execute` HTTP 423, no financial `VERIFIED`.


## QA intake checkpoint — BKL002 packet #10 (2026-11-16)

- [BKL002-QA-20261116-10](requests/BKL002_QA_PACKET_20261116_REWARD_OBLIGATIONS_PORTABLE.md) READY_FOR_QA, commits `ab5e29d`.. `266b879`: authenticated reward obligation source archive now includes blocked category/reward link graph with composite keys and parent FK closure, source family isolation and missing-graph denial. New PostgreSQL/inspector negative regressions committed, developer Maven NOT_RUN. **QA state IN_QA/NOT_RUN**, distinct from bounded `27/27 PASS @419efb1` (#01–03).
- #04–#10 are best handled as **one updated pinned-SHA synthetic regression**. Reuse the 16-class inventory from the preceding control entry; it already contains all eight classes requested in #10, so no extra duplicate full execution is required. Current code changed the expected status of OBLIGATIONS from unexported to exportable-but-not-replayable: validate revised archive category graph rather than stale old blocker assumptions.
- Preserve actual test counts, zero skipped if targeting full acceptance, SHA before/after, and a single QA→Backend #15 consolidated outcome. No user-operated *real* tests until Backend readiness and the pre-BETA unified campaign; synthetic tests are permitted independently.
- Future real obligation/blocked target and Web bootstrap GET recovery instructions were collected in [PRE_BETA_REAL_WORLD_TEST_INBOX.md](../qa/PRE_BETA_REAL_WORLD_TEST_INBOX.md), commit `d6a3f93`. Production reset HTTP423 / no VERIFIED / BETA HOLD.


## P0 QA regression observed — run 43947907960 / commit 19adfe1b7 (2026-11-16)

- QA independently checked Actions jobs: Maven/PostgreSQL **FAILURE**, full exact-SHA proof **FAILURE**, per-commit result **FAILURE**; Android/Web/Compose SUCCESS. Architect read JUnit count **460 tests, 1 failure, 1 error, 0 skipped**.
- Fail 1: `WorkspaceRewardObligationArchiveClosurePostgresTest.unexportedPurchaseObligationsPreventAuthenticatedIncompleteArchive:189`, expected 1 / actual 2. Inspect actual obligations/category graph inventory before fixture change.
- Error 2: `DisposableRewardCategoryCrossDatabasePostgresTest.independentlyMigratedDatabasesRebindCategoryLinksAndPreserveOtherFamily:105`, null receiver for Object.equals; inspect target mapping/PK and transactional replay before test change.
- **QA → Backend P0 notification:** [Issue #15 comment 6928302234](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6928302234). Backend source fix and independent retest required. #04–#11 now NEEDS_RETEST/FAILED_CI at 19adfe1b7; no current-source PASS. Historical #01–#03 27/27 PASS @419efb1 remains bounded, not same-SHA.
- New [packet #11](requests/BKL002_QA_PACKET_20261116_CATEGORY_NATIVE_REPLAY.md) test-only two-independent-PostgreSQL native reward-category/link replay, mapped definition IDs, duplicate/foreign/tamper negatives; source committed, **not accepted**, current CI failure explicitly overlaps this case.
- Do not execute real/manual tests until Backend and approved isolated environment; hold production Reset Execute HTTP423 / durable VERIFIED / BETA.
