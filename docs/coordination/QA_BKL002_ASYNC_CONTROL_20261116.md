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
