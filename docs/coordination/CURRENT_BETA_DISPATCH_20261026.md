# FamilyPoints — Architecture coordination 2026-10-26, current BETA scope

**State:** ASSIGNED, not implemented by this planning commit. Baseline SHA `9af3562526ecf28875c1ac8d99d2532019f84d40`; verify current SHA/CI before code changes.

## User-approved decisions (authoritative)
- Economy **v0.7 preliminarily approved for DEV implementation**, adjust prices/difficulty after testing on BETA. No retroactive mutations of ledgers, balances or existing private templates; `null` price never means free.
- **Selective data reset (BKL-002) and first-run rebootstrap are REQUIRED before next BETA release**, to allow testing 142 curated suggestions, 6 packs, private-library selective import and explicit activation. Never run an actual family reset without a separate scoped confirmation.
- Frequent BETA publication and targeted smoke remain the objective; broad design QA does not block after minimum safety gates.

## Immediate critical path and track ownership
| Rank | Owner | Task | Exit gate |
|---|---|---|---|
| P0 | Backend + Media | Resolve MediaCatalogServiceTest sorted global media IDs contract. Previous run 40409863763: Java 226 tests, one failing sorted ID test; Web/Android/Compose green. Recent commit 9af3562 may have fixed ordering; **re-check latest exact CI, do not assume still broken.** | All 4 jobs green |
| P0 | Backend | Deliver BKL-002 family-scoped reset preview + guarded execution, category/dependency coverage, idempotent operation, audit and post-reset onboarding state. Preserve auth/member identity by default and other workspaces. | Transactional Postgres/tenant/security tests, documented API |
| P0 | Web | Admin reset checklist, counts/warnings, explicit protected-data confirmation, successful reset → first-run wizard → selective import of seed → optional explicit TASK/REWARD activation. | DEV authenticated flow in four locales |
| P0 | QA | Verify reset only on disposable family authorized for testing, repeat/import/restart, no dangling FK, no ledger mutations from import, cross-family isolation, 4 currencies, TIME 30-min. Currently DEV workspace 1 has 3 TASK private templates; no imported TIME template (TPL-08 pending). | Documented targeted tests, no unsupported PASS |
| P1 | Android | Verify fixed TemplateLibrarySection theme/description/duplicate import on emulator; distinguish one newly importable task vs already-imported-only preview, selected-first list, family switch. After reset clear/partition cached and queued offline operations; no stale replay. | Exact SHA Gradle success + emulator screenshot/test |
| P1 | Designer / Media | Integrate 18 approved category icons with safe Global Media API on Web/Android; review 142 catalog entries and approved v0.7 suggestions (difficulty independent of time, optional prices). Do not auto-write seed prices to user data. | Versioned catalog, asset mapping, i18n |
| P1 | Release | Build matched Backend+Web+Android BETA after reset / onboarding targeted QA. Pin schema/commit/APK SHA; smoke login, reset preview, approved reset on disposable fixture, 142/6 catalog, import one, currencies. Never reset existing family without direct authorization. | Published manifest and health evidence |
| P2 | Architecture | After BETA, BKL-003/005 admin history corrections, BKL-008 time reward 5/2/1 beep countdown, BKL-009 Android linked app reward, BKL-010 managed enforcement, BKL-011 calendar. | Separate approved packages |

## Explicit blocking vs nonblocking
**Block deployment:** broken CI, critical auth or tenant leak, dangerous reset scope, unexpected loss/corruption of ledgers, broken login/start, inability to test seed after reset.
**Do not block:** cosmetic icon styling, unapproved economy price precision, comprehensive exploratory regression, future timer/app-launch features.
**Only Release deploys**, after team signoff; coordination never performs SQL deletions, migrations or server changes.

## Reporting discipline
Each owner posts SHA, exact successful/failed tests, DEV evidence, remaining issue, next owner in its journal. The cross-chat dispatch is via this repository, not direct chat messages.