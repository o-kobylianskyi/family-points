# FamilyPoints — Testing / QA journal

## Scope
Sixth independent track: QA owns risk-based test planning, test cases, defect reproduction, regression verification, cross-client integration checks and independent release acceptance evidence. Backend/Web/Android fix their own source; Release owns deployment. QA does not claim a commit, CI job or BETA test proves another stage.

## 2026-10-24 — QA track initialized (DOCUMENTATION ONLY)
- Repository `o-kobylianskyi/family-points`; active integration branch `feature/task-soft-delete`. Reviewed `AGENTS.md`, coordination README, shared PROJECT_STATE and IMPACT_REGISTER, Backend and Release journals and XTR-W-01.
- This QA charter lives on topic branch `docs/qa-track-init` pending incorporation into integration. No project build, automated suite, browser/device run, BETA access or VPS deployment performed.
- Critical risk queue:
  - **QA-001 P0:** Two-family tenant separation: API resource IDs, member/group/task/reward/media visibility, workspace switching and cached/offline data; forbidden writes/reads and token scope.
  - **QA-002 P0:** Reward life cycle: purchase request -> parental approval -> deliver/receive confirmation -> ledger and usage status; verify authorized manual correction of delivery timestamp, price and status when implemented. Test duplicate approve/receive and insufficient funds.
  - **QA-003 P0:** Signed Android release installation/upgrade and startup/login on actual phone; previously observed IllegalAccessError; keep release publication blocked until device evidence.
  - **QA-004 P0:** Offline point queue/idempotency: repeated replay, network flap, restart, multiple-family switching, reconciliation without duplicate ledger postings; offline task mutation not assumed complete.
  - **QA-005 P1:** Registration/password + Google + email invitation acceptance; new/existing identities, expiry and family choice.
  - **QA-006 P1:** Task templates, assign/claim, child task completion with adult approval, recurring schedule, group/shared eligibility and historical correctness.
  - **QA-007 P1:** Calendar/reward availability: both direct/approved purchase gates, DST/week boundaries, old DTO compatibility, two-family negative cases, Flyway V35 and V36 order after draft PRs #4/#5 merge.
  - **QA-008 P1:** Media uploads/catalog: authorization, file limits/MIME/EXIF, inherited category images, private cache, migrations/backups and legacy client behavior.
  - **QA-009 P1:** Web/Android visual consistency, dropdown/input controls, light/dark theme, translations, dynamic rather than rasterized controls, accessibility.
  - **QA-010 P0:** Beta release acceptance: exact source SHA, green CI, backup/restore evidence, safe migrations, signed APK verification, HTTP smoke/auth, rollback.
- Every test case must record branch/SHA or deployed version, environment, preconditions, steps, expected vs actual, evidence, severity, owner, retest status.
- Status labels: NOT_RUN, PASS, FAIL, BLOCKED, NEEDS_RETEST. Defect priority: P0 blocking release; P1 high; P2 medium; P3 low.
- Open gate: QA cannot certify live BETA without independent runtime/device evidence. Cross-track requests must go into `docs/coordination/requests/` and owning track journals.

## Default reporting template
| ID | Priority | Scenario | Target | Status | Evidence | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| QA-001 | P0 | Two-family access isolation | Backend/Web/Android | NOT_RUN | No executed negative E2E | Backend |
| QA-002 | P0 | Reward approval and fulfillment | Backend/Web/Android | NOT_RUN | Reported user failure requires reproduction | Backend/Android |
| QA-003 | P0 | Signed APK start/upgrade | Android/Release | BLOCKED | Device retest required | Android/Release |
| QA-004 | P0 | Offline replay | Android/Backend | NOT_RUN | Device/E2E proof absent | Android/Backend |
| QA-010 | P0 | Beta release gate | Release | BLOCKED | Combined gate evidence absent | Release |

Update outcomes only after running or reviewing exact evidence. 
