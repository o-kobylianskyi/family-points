# FamilyPoints Designer — integrated onboarding, economy and reward-history parity checkpoint
Date: 2026-11-03. Base source observed main `def7f7b83ffa214f6e82630049bd3cccfb5532c4` before Designer commits. Follow Architect dispatch `DISPATCH_20261101_2241_CI_BKL002.md`, with newer approved `ADR-FAMILY-RESET-002.md` and `REWARD_HISTORY_IMPLEMENTATION_GATE_20261102.md`.

## Implemented in this batch (actual source)
- Web `frontend/src/components/TemplateDraftFieldsEditor.jsx` commit `d9b3605e`: four-locale per-field help for TASK reward/penalty and REWARD price showing **UNSET (requires decision; not free)** vs explicit **0** vs set value. Presentation-only; existing JSON edit and numeric handling untouched.
- Android `android/app/src/main/java/com/olehkobylianskyi/familypoints/android/ui/screens/TemplateFieldsEditor.kt` commit `16450656`: same four-locale supporting text on rewardAmount, penaltyAmount and priceAmount. Existing integer parser and omission on blank remain unchanged; no new account/family mutation.
- This is a **copy and semantic-parity update, not an activation behavior change**. Do not claim actual UI acceptance or exact-HEAD CI without completed evidence.

## Source audit: actual outstanding end-to-end gates
1. Web `FamilySetupPage.jsx` supports explicit member-group creation and separately verified import; the import confirmation does not activate TASK/REWARD definitions. Web `InitialFamilySetupReview.jsx` has setup finalize/UUID receipt; this is a distinct stateful flow, not a unified multi-entity transaction.
2. Android `WorkspaceOnboardingScreen.kt` handles family selection/creation. Android setup and template-import/activation coordinators are separately exposed. No full cross-client first-family→member group→task-folder→catalog import→explicit task+reward activation interaction has been accepted at pinned current SHA.
3. Web template creation editor exposes creation DTO keys such as `defaultDurationMinutes` and `rewardKind TIME_BASED`; Android **portable** template editor exposes `durationMinutes`, `rewardKind TIME/ITEM`. These represent potentially distinct DTO/schema contexts. Do **not** blindly rename either: Backend needs mapping proof at draft→activation boundary, including TIME 30min and all chosen reward kinds.
4. Mobile editor currently supports a narrower field list than Web (e.g. no member/point type selector in shown portable form). Compare actual editable contract by endpoint and task/reward activation action before marking parity, avoiding mistaken 1:1 UI parity assertions.
5. Nullable reward/task amount `null/absent` means UNSET, never 0/free. Newly added descriptions make this visible in both client editors, but Backend remains authoritative for price selection and valid creation.
6. Reward History purchase-only v1 has separate Backend/Web/Android implementation work. Correct labels are `PENDING_APPROVAL`, `READY`, `REDEEMED`, `CANCELLED`. Do **not** turn READY/REDEEMED into DELIVERED/RECEIVED or claim request/obligation lifecycle from purchase history.
7. `ADR-FAMILY-RESET-002` now settles key architectural policy: destructive operations only for verified family owner; FULL unsupported category blocks; SELECTIVE REPLACE_SELECTED rather than implicit MERGE, and 30-day local retention. This is not implementation or reset authorization. Source review should supersede earlier Designer docs still labeling those decisions open.

## Requested owner batch
**Web P0:** finish complete first-family receipt-driven flow with typed MEMBER_GROUP vs WorkNode GROUP, separate import vs activation and actual creation IDs; honor nullable points and reward TIME duration. Assert 4 locales, dark/light, mobile; no hidden writes on navigation.
**Backend P0:** publish exact versioned end-to-end activation DTO/value mappings, idempotent original UUID response/receipt, real BKL002 archive HTTP & FULL/SELECTIVE committed apply and owner proof; test in disposable PostgreSQL.
**Android P1:** source-verify DTO mapping for TIME/ITEM and reward editor fields, common terminology with Web, identity/epoch fencing and no offline queued setup writes; run pinned Gradle and device instrumentation separately.
**QA P0:** add/execute source-aware parity assertions: blank vs zero vs positive/negative as permitted, TIME 30min, 142 templates/6 packs and duplicate selections, owner/child/account-family switch, import not activation, lost POST receipt, and purchase-only history status/filters. Record exact SHA, counts, skips and version. Testonly and CI status currently not accepted as all green.
**Designer P2:** accept copy+visual semantics when build and browser/Compose test evidence is received. No autonomous destructive actions or new cosmetic sprint.

## Release gate
As of source checkpoint, latest Actions run was in progress; earlier run had a failed job. No new safe backup/restore executable claim, no BETA deploy. All production and persistent Test/Test2 reset/restore operations remain HOLD.
