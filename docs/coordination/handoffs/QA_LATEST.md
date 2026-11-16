# FamilyPoints QA — rolling handoff (2026-11-16, P0)
## Accepted historical results
- BKL002 #01–03: 27/27 PASS, 0 fail/error/skip, exact tested SHA 419efb1fdcab849480e790449c791c86ce004695. QA evidence commit 2a32c9b.
- Web V49 lost-finalize synthetic 2/2 operator PASS, unpinned local checkout; Android P0 Linux 98/98 at 09da723; four-component full CI 43934619348 at 3c0cf014, historical.
## Confirmed current P0 blocker
- Actions [43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960) SHA 19adfe1b7: Java Maven PG Testcontainers 460 tests, 1 assertion failure, 1 error, 0 skipped; Backend/full-evidence/per-commit job FAILURE, Web/Android/Compose SUCCESS.
- WorkspaceRewardObligationArchiveClosurePostgresTest.unexportedPurchaseObligationsPreventAuthenticatedIncompleteArchive:189 expected 1 got 2.
- DisposableRewardCategoryCrossDatabasePostgresTest.independentlyMigratedDatabasesRebindCategoryLinksAndPreserveOtherFamily:105 NullPointerException (null Object.equals).
- QA→Backend defect [Issue #15 comment 6928302234](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6928302234); control doc update 2f770147. No source fix verified yet.
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
