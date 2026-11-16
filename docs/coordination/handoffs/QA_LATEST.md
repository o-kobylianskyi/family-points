# FamilyPoints QA — rolling handoff (2026-11-16, P0)
## Accepted historical results
- BKL002 #01–03: 27/27 PASS, 0 fail/error/skip, exact tested SHA 419efb1fdcab849480e790449c791c86ce004695. QA evidence commit 2a32c9b.
- Web V49 lost-finalize synthetic 2/2 operator PASS, unpinned local checkout; Android P0 Linux 98/98 at 09da723; four-component full CI 43934619348 at 3c0cf014, historical.
## Confirmed current P0 blocker
- Actions [43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960) SHA 19adfe1b7: Java Maven PG Testcontainers 460 tests, 1 assertion failure, 1 error, 0 skipped; Backend/full-evidence/per-commit job FAILURE, Web/Android/Compose SUCCESS.
- WorkspaceRewardObligationArchiveClosurePostgresTest.unexportedPurchaseObligationsPreventAuthenticatedIncompleteArchive:189 expected 1 got 2.
- DisposableRewardCategoryCrossDatabasePostgresTest.independentlyMigratedDatabasesRebindCategoryLinksAndPreserveOtherFamily:105 NullPointerException (null Object.equals).
- QA→Backend defect [Issue #15 comment 6928302234](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6928302234); control doc update 2f770147. Backend now has source fixes 839e9f8, e1e4e2b, 9ccfe2e and formal QA packet #12 at d5b1e85; independent retest NOT_RUN / NOT ACCEPTED.
- BKL002 packet #11 docs/coordination/requests/BKL002_QA_PACKET_20261116_CATEGORY_NATIVE_REPLAY.md is test-only two-independent-PG reward-category FK remap; its new test is failing. #04–11 current SHA NOT_ACCEPTED; old 27/27 does not supersede failures.
## Deferred real PRE-BETA
- User wants QA to COLLECT real/manual/device test instructions from every track, execute one operator+QA campaign after Backend and Release gates only. [Shared inbox](../../qa/PRE_BETA_REAL_WORLD_TEST_INBOX.md) covers seven-step V49, 142 templates/6 packs, UUID replay, child/tenant ACL, Android device/offline, rewards+ledger, Release APK signature and safe operational backup.
- Never touch real family, Test/Test2, BETA/PROD or execute reset without fresh explicit approval for isolated disposable environment and independently supported owner-bound durable VERIFIED restoration. HTTP423 reset, BETA HOLD.
- Distinguish Release pg_dump atomic TOC-check from actual restore and BKL002 portable .fpbackup.
## QA workflow
- Continue independent synthetic automated regression and report to Backend Issue #15, Web Issue #18, Release as appropriate; pin exact SHA, test counts, failure/error/skip and env. One deduplicated next suite: previous 16 classes plus DisposableRewardCategoryCrossDatabasePostgresTest, focusing both red tests and FK/tenant/transaction neighbors.
- Latest source state changes continuously; re-read GitHub commits, handoffs, control, issue comments at each pickup.
- Chat capacity protocol docs/coordination/requests/CHAT_CAPACITY_ALL_TRACKS_20261116.md: true context token percent inaccessible; do not guess 50/75/85%. Create/update QA measurement log and this handoff every substantive batch, audit role handoffs P1 without delaying P0.
## Next task
Check Backend remediation of both confirmed P0 failures, independent pinned rerun and Issue #15 response. Collect new real test handoffs; NO manual live tests.


## Latest QA-012 pickup — update
- Backend acknowledged both historical red cases and delivered [CI_RED_REPAIR #12](../requests/BKL002_QA_PACKET_20261116_CI_RED_REPAIR.md): two fixture categories rather than one (839e9f8), no immutable-map null NPE + sorted archive category order (e1e4e2b), null-target-map negative regression (9ccfe2e). No newer same-SHA PASS observed.
- QA control recorded specific retest requirements commit `7b9eca5`: focus previously failing methods and scope/tenant/map/rollback safeguards; eight-class packet #12 then deduplicated 17-class neighboring suite, then actual four-component non-skipped full CI.
- User still wants all real/manual tests collected and deferred. New automation must not become real-data execution. Follow up with Backend on Issue #15 only once there is independent run evidence.


## QA-013 — latest checkpoint (2026-11-16 evening)
- Backend code-repair #12 remains `NEEDS_RETEST` despite source fixes `839e9f8/e1e4e2b/9ccfe2e`; no newer independently confirmed same-SHA CI run/Job PASS. Previous verified red `43947907960` at `19adfe1b7` stands.
- Exact Java source read for newest `DisposableRewardRequirementRestoreExecutor` + `DisposableRewardRequirementCrossDatabasePostgresTest` (commits `e825afe0`, `4088b5e5`): test-only two-PG AES restore of requirements with explicit owner remap, source/target tenant checks, null map, second replay, tampered archive negative. **Source only: new Testcontainers test not run.**
- Control [QA_BKL002_ASYNC_CONTROL_20261116.md](../QA_BKL002_ASYNC_CONTROL_20261116.md) updated at `ab8268a`; latest deduplicated target **18 distinct classes** (prior 17 plus requirements cross-DB). Await Backend fixes/tests and actual workflow run ID; do not accept by visual source audit alone.
- PRE-BETA operator manual campaign remains COLLECTION ONLY; final real test scripts tracked in [QA inbox](../../qa/PRE_BETA_REAL_WORLD_TEST_INBOX.md). Production HTTP423 reset / no VERIFIED / BETA HOLD.
- Next: verify newly reported GitHub Actions run on exact patched SHA (all four actual jobs, zero skipped), isolate two formerly red regression tests and requirement test. If failures, feedback to Issue #15 with exact repro; if pass, accept bounded packet only, not release gate.

## QA-014 latest — verified TWO new Actions failures and QA owner routing (2026-11-16)

- [run 43953661904](https://github.com/o-kobylianskyi/family-points/actions/runs/43953661904) `d28048c9`: Maven 460 tests/1 failure/0 errors/0 skips; Compose FAIL; Web and Android PASS; full exact SHA FAIL.
- [run 43953986968](https://github.com/o-kobylianskyi/family-points/actions/runs/43953986968) `4088b5e5`: Maven 461 tests/1 failure/0 errors/0 skips; Compose FAIL; Web and Android PASS; full exact SHA FAIL. Independently read latest Maven/Compose job logs (133603625640 / 133603625494).
- Root cause Maven: test `DisposableRewardCategoryCrossDatabasePostgresTest:107` expects SecurityException for null target map, instead gets `IllegalArgumentException CATEGORY_REPLAY_SCOPE_UNSUPPORTED` from `DisposableRewardCategoryRestoreExecutor:48`, exact ordered inspected category list mismatch before mapping guard. Full stack independently confirmed. Backend issue #15 comments 6930278438, 6930291716. Earlier 1-vs-2 and Map.of NPE no longer in new summaries, but no PASS.
- Root cause Compose: unterminated SHA regex/if expression in `scripts/android/build-release-apk.ps1:19`, PowerShell parser fails at 20. Release issue #20 comment 6930279375. No live APK publication.
- New Backend packet #13 requirement two-PG replay test-only source; independent PASS pending; no production VERFIED, reset Execute HTTP423, BETA HOLD.
- Web owner formal cross-tab marker ownership handoff `bed5098`; two-tab adversarial manual acceptance queued `REAL-V49-TABS-02`, Release provenance `REAL-REL-APK-02` in [PRE_BETA_REAL_WORLD_TEST_INBOX](../../qa/PRE_BETA_REAL_WORLD_TEST_INBOX.md) commit `69e8d428`. Never execute real tests now.
- [QA ASYNC CONTROL](../QA_BKL002_ASYNC_CONTROL_20261116.md) latest CI evidence commit `d067675`. Next: check Backend and Release fixes; new run on same patched SHA with all real component test steps and exact counts, no skips; send precise outcomes. Keep rolling handoff and fact-based chat-capacity log.

## QA-015 latest pickup — 2026-11-16
- Rechecked newest main source. Backend P0 `DisposableRewardCategoryRestoreExecutor:48` exact scope check is still causing wrong exception versus expected SecurityException for null map; historically verified run `43953986968` Maven 461 tests 1 failure/0 errors/skips, plus Compose failure. No later same-SHA all-four job PASS discovered. Release `scripts/android/build-release-apk.ps1:19` still has incomplete SHA regex/if PowerShell expression, so parser regression remains.
- Backend #14 formally READY_FOR_QA: `docs/coordination/requests/BKL002_QA_PACKET_20261116_OBLIGATION_NATIVE_REPLAY.md` commit `4a3fbe7`, test-only `DisposableRewardObligationRestoreExecutor` + two separate PG18 target test `DisposableRewardObligationCrossDatabasePostgresTest`; validates five explicit mapping types, purchase economic identity, 1/1/1 categories/rewards-blocked graph, and actual two-row SQL rollback after first insert. **NOT RUN independently**; queue now 19 distinct classes.
- Web two-tab synthetic Playwright `onboarding-v49-two-tab-synthetic.spec.js`, commit `fe64edb`, 2 new cases original UUID/lost response and competing marker receipt, **source only**. All real PRE-BETA tests still held.
- QA control and PRE-BETA intake updated `5376124` and `e52c18b`. Real-family PROD/BETA no operations, `reset/execute` HTTP423, VERIFIED unavailable. Next: inspect Backend/Release P0 source fixes and exact-SHA CI run, retest category scope and PowerShell parser first, then #14 Testcontainers/2-tab synthetic.

## QA-016 2026-11-16 late pickup
- Source fix `2539bf93515` now corrects Java alphabetical archive category preflight. Prior failure `CATEGORY_REPLAY_SCOPE_UNSUPPORTED` from completed CI 43953986968; source repaired, **no independently confirmed new pass**. QA→Backend #15 comment `6930583146`.
- Release script `scripts/android/build-release-apk.ps1:19` still syntactically incomplete on latest inspection, explicit owner Issue #20, full CI HOLD.
- Backend new test-only manual point ledger executor `1d9fb146`, two-PG test `39b699c3`: fixture-supplied source currency codes, not archive-verified. Add one unique class to synthetic suite, now **20 distinct classes** pending independent execution, with #14 obligation rollback. Cannot claim restored historical currency/source identity.
- QA BKL002 control `e17792270` and central PRE-BETA inbox `4e4d6fda` updated. Real candidate-only manual tests COLLECT_ONLY. `restorable=false`, reset Execute HTTP423, BETA HOLD.
- Next QA-017: inspect Release syntax fix, source category test outcome and new CI on same SHA; verify 2 historically red cases, new obligation/point ledger tests and exact PASS/FAIL/ERROR/SKIP; coordinate blockers by issues. Renew this handoff/capacity checkpoint each substantial batch.

## QA-017 latest checkpoint — 2026-11-16
- Formally read Backend packet #15 `docs/coordination/requests/BKL002_QA_PACKET_20261116_MANUAL_POINT_LEDGER_REPLAY.md` (`a07ce64`): manual-only point history native replay test source, two PG18, +12/-2=10 and neighbor +99, tenant/currency mismatch/TASK denial, but source currency codes not authenticated; 20 distinct Testcontainers classes pending. No latest-source Maven PASS.
- Category lex scope source fix `2539bf9` exists; last inspected Maven red run #43953986968 at `4088b5e5`, 461 tests/1 failure, older SHA. Release `scripts/android/build-release-apk.ps1:19` incomplete expression persists in inspected current source. No latest exact-SHA full CI PASS.
- QA async control commit `fd495a2`; full manual PRE-BETA campaign deferred. Never production restore/reset, HTTP423, BETA HOLD. Next QA-018: verify both P0 source fixes and exact-SHA real GitHub Actions execution, 20-class isolated target, user-facing results; keep handoff and chat-capacity updated.

## QA-018 latest checkpoint — authenticated point denomination source proof
- New source commits `e6f4bde`..`a567b4b`: encrypted scoped `POINT_TYPES` with manual ledger, authenticated source codes, exported family/FK closure; `DisposablePointLedgerRestoreExecutor` now verifies archived point type code against supplied expected source code and target active code. It remains src/test, MANUAL-only; polymorphic TASK/reward/refund source types denied. No new Maven/PG PASS evidenced. Prior QA statement 'no authenticated source currency metadata' is superseded for this bounded manual proof, not full production financial restore.
- QA→Backend request for formal #16/negative PG coverage [issue comment 6930866824](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6930866824). [QA control](../QA_BKL002_ASYNC_CONTROL_20261116.md) commit `49ae8a18`; [real-test inbox](../../qa/PRE_BETA_REAL_WORLD_TEST_INBOX.md) commit `73e47120` adds future denomination case.
- No newer verified all-four non-skipped exact SHA Actions run; known older 43953986968 red on Backend/Compose. Release PS1 syntax parser issue still not seen fixed on last direct source read. No actual tests executed by QA. BETA HOLD/HTTP423/no VERIFIED, PRE-BETA real cases COLLECT_ONLY.
- Next QA-019: check formal Backend #16 and Release script fix, fetch current push CI run ID/jobs/logs if supplied, inspect 20-class targeted Java source/dry-run evidence, report precise PASS/FAIL by candidate SHA, refresh this handoff and chat-capacity.

## QA-019 pickup — Backend packet #16 and exact-SHA gate
- Latest observed HEAD before QA write: `ff044623ab51` (Backend capacity documentation). Source formal QA #16 packet `b662d04`, negative regression `0657099` inspected. Authenticated POINT_TYPES archive + ledger FK; missing/dangling, duplicate code and foreign family negative case; source tests **NOT independently executed**, 20 unique classes queued. QA control commit `2b974b1`.
- Current Release `build-release-apk.ps1:19` incomplete SHA expression directly inspected; no newer release repair/Compose PASS. Last completed verified full-run 43953986968 @4088b5e5 was red, but newer HEAD CI is **LOOKUP_BLOCKED** without concrete run ID (not falsely FAIL). 
- User+QA unified manual PRE-BETA campaign remains postponed. No real-family writes, immutable ledger/owner binding/VERIFIED incomplete, reset HTTP423, BETA HOLD.
- Next QA-020: seek Release parser fix and actual GitHub Actions push-run IDs, inspect new financial packets and repo/QA logs; if CI repaired, validate exact-SHA Maven/Compose non-skipped results and report Backend #15/Release #20. Update handoff and measurable chat logs; do not estimate context percent.

## QA-020 — current QA continuation checkpoint (2026-11-16 late)
- Checked latest `main` at pickup `246efab2d3f7`: QA-only checkpoint; no Backend/Release fix after #16 source packet in observed commit list. Current `scripts/android/build-release-apk.ps1:19` still syntactically incomplete. Reopened `.github/workflows/ci.yml`: PowerShell parse step is mandatory inside Compose component job. This source cannot pass non-skipped Compose validation until fixed. Not a reason to disable the gate. Historically confirmed failed runs 43953661904 @d28048c9 and 43953986968 @4088b5e5; current HEAD actual CI run ID not found, status LOOKUP_BLOCKED/UNVERIFIED.
- Formal Backend #16 currency closure test source READY_FOR_QA, 20 deduped classes, independent current SHA Maven NOT_RUN. Latest QA control update `34e6de9279`. Production reset HTTP423, no VERIFIED or real tests. 
- Next QA-021: identify actual fixed Release script commit and exact-SHA Actions jobs, then verify BKL002 #12–16 targeted PG suite + four component gate; if run unavailable report LOOKUP_BLOCKED and continue source audit. Keep owner manual campaign collected only.

## QA-021 latest — BKL002 signed ledger source graph #17, current P0 gates (2026-11-16)
- Latest pickup source HEAD `50d014f3bdd3`, Backend new source-only authenticated financial ancestry packet #17 `947c19c`. Read exact `DisposableLedgerSourceGraphVerifier` `5928292` and `DisposableLedgerSourceGraphVerifierTest` `b6cf1c3`. MANUAL null source_id, TASK positive source_id found in archived TASK_EXECUTIONS and same member, POINT_TYPES and POINT_LEDGER sealed. Unsupported REWARD_PURCHASE/REFUND/EXCHANGE denied. Test-only **READ-ONLY** no target remap or restore; Maven NOT_RUN. 21 unique queued test classes (20 prior plus one new source verifier test). 
- QA control updated `3f7dfcd`, unified final manual inbox `2563331` with future ledger ancestry acceptance; no manual execution.
- Release `scripts/android/build-release-apk.ps1:19` still malformed after direct latest-source check. Last verified red CI `43953986968` @4088b5e5 is historical; newer HEAD exact-SHA run LOOKUP_BLOCKED, not an observed new FAIL or PASS.
- QA must continue checking full CI logs, P0 source repair, pinned 21-class targeted Testcontainers and two-tab V49 synthetic tests once executable. PROD/BETA/Test/Test2 untouched; no VERIFIED; Reset Execute HTTP423; BETA HOLD. Next QA-022: receive Backend #18/push CI and Release parser correction, source audit and follow-up.
