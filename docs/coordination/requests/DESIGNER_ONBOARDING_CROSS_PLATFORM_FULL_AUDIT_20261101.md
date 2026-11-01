# FamilyPoints Designer — complete onboarding UX/API parity audit and large-package handoff
Date: 2026-11-01. Source inspected: current GitHub main `601428f5191073e17413a05f6e4e2aee03043b4f` (moving shared main). Architect source: `DISPATCH_20261101_EVENING_RESTORE_ONBOARDING.md`, Issue #18, `XTR-20261101-SETUP-01.md`.

## Decision / priority
Onboarding is **Web/Backend P0**, Android P1 and Designer **supporting P2** according to current Architect dispatch. BKL-002 remains a separate security gate; onboarding never grants permission to reset existing family data. **One large implementation package** should deliver a complete fresh-family flow rather than a series of presentation-only patches.

## Verified implementation inventory
- Web `FamilySetupPage.jsx`: existing-family 4-step Groups → Choose templates → Review → Import, 4-language catalogue, draft sessionStorage scoped account+family, parent explicit member-group creation, independent group GET failure handling, server import preview repeated just before POST, authoritative private template re-read and skip option. Import **does not activate task/reward definitions** and does not write balances/purchases.
- Web `InitialFamilySetupReview.jsx`: *separate* V46/V48/V49 setup-state workflow with seven typed metadata steps, catalog dry-run, checked confirmation and atomic private-template import + setup finalize/receipt, UUID retained for ambiguous outcomes. This component is explicitly **not a complete activation wizard**.
- Web `setupApi.js`: GET setup, revision-fenced PATCH steps, POST review, POST finalize-import with Idempotency-Key, receipt GET. Do not invent a missing setup+group+activation atomic POST.
- Android `WorkspaceSetupRepository.kt`: setup state + read-only catalog review with workspace/revision/epoch assertions.
- Android `WorkspaceSetupOverviewRepository.kt`: separate member-group versus task-folder counts, explicit revision-fenced step acknowledgments; COMPLETE/SKIPPED are metadata, **not actual created group or activated record**. Other dedicated Android import/finalization/activation coordinators exist; their mere existence is not proof of full device onboarding UI parity.
- Latest dispatch: group creator and duplicate reconciliation Web source exist, but the end-to-end new family → group → catalog selection → import → **explicit activation** remains unverified. QA browser/device live acceptance is deferred.

## Non-negotiable semantic model across Web/Android/Backend
| Concept | UI labels (UA / RU / DE / EN) | Actual success evidence |
|---|---|---|
| FAMILY | Сім’я / Семья / Familie / Family | Created selected workspace identity from authenticated server response |
| MEMBER_GROUP | Група учасників / Группа участников / Mitgliedergruppe / Member group | Real server group ID from scoped group endpoint |
| WORK_NODE GROUP | Папка завдань / Папка задач / Aufgabenordner / Task folder | Real WorkNode GROUP ID, not MemberGroup |
| REWARD_CATEGORY | Категорія нагород / Категория наград / Belohnungskategorie / Reward category | Real category ID; NOT task folder |
| CATALOG PACK | Набір шаблонів / Набор шаблонов / Vorlagenpaket / Template pack | User explicitly chose concrete code list; pack is not an installed object |
| PRIVATE IMPORT | Імпортовано до бібліотеки / Импортировано в библиотеку / In Bibliothek importiert / Imported to library | Verified private template IDs/codes or durable import receipt |
| ACTIVATE | Створити активне завдання чи нагороду / Создать активное задание или награду / Aktive Aufgabe oder Belohnung erstellen / Create active task or reward | Explicit definition/activation response and original UUID receipt |
| SETUP COMPLETE | Початкове налаштування завершено / Начальная настройка завершена / Ersteinrichtung abgeschlossen / Initial setup completed | Authoritative server setup status COMPLETED; may contain only private imports, not active definitions |

**Crucial:** a UI step marked COMPLETE is user acknowledgment, not evidence of creating a group, adding a task, awarding points, spending points, or activating a reward. A skipped step never claims side effects. Imported private template is not an active task/reward. Nullable price UNSET must never be displayed as 0 or “free”; TIME reward duration and difficulty/effort are separate.

## Proposed complete parent wizard (realization after capabilities are verified)
1. Select newly created family / ensure correct parent permission. If V46 setup feature flag disabled or missing state, show non-destructive safe fallback and preserve existing-family setup entry; never silently enroll an old family.
2. Members (can skip) → **people groups** (explicit create/choose) → **task folders** (explicit create/choose with separate endpoint). No combined “Group” control that points at the wrong entity.
3. Choose six packs and/or individual items from 142 curated templates (110 task, 32 reward). Show de-duplicated count and previously imported count; selection persists only as family+account draft, *not* approval.
4. Review selected task/reward drafts separately: assignments, group/WorkNode target, schedule, TIME 30 min, currency/points nullable UNSET and category icon. Show “needs parent decision” when missing required points or a dependency.
5. Read-only server review of actual codes, conflicts, toImport/alreadyImported, currency/ownership/dependencies, revision+epoch. If selection or family changes, invalidate preview.
6. Explicit confirmation for **private import**. Single immutable operation UUID, revision, same body across retries; first examine receipt after timeout. Family/account/epoch switch quarantines stale pending operation.
7. After verified import, **separate explicit activation** of chosen TASK/REWARD definitions with editable values; never activate every imported item by default or infer activation from COMPLETE/SKIPPED. Return actual IDs and receipt, or an actionable recovery state.
8. Show final summary with separate counters: imported private templates, active tasks, active rewards, skipped setup steps, unresolved items. “Setup finished” refers to server setup completion; “ready to use” only when explicitly activated. Allow navigate to library without implicit writes.

## Critical gaps to be handled in a single cohesive Web implementation package
**P0:**
- Two partially overlapping onboarding views carry different expectations: `FamilySetupPage` manual four-step library import and `InitialFamilySetupReview` seven-step server metadata/finalize flow. Define navigation/authority for fresh vs existing families, avoid duplicated or conflicting completion claims, and use one shared step semantics/copy matrix.
- User-visible Group step currently focuses on member groups with a link for others: do not claim this creates WorkNode GROUP folders. Explicit optional task-folder action with correct permissions/endpoint, or mark “configure separately.”
- Current import result is not activation. Make a visually explicit **next action** / status in completion screen and setup receipt, using the actual versioned activation APIs and only after fresh explicit user choice.
- Server V49 finalize-import has exact receipt/revision semantics but is not a full saga transaction including group/member creation and task/reward activation. Avoid one-word “Everything completed” success.
- Clearly show nullable/unset points vs 0 and TIME duration for each reward/task review; never invent fallback prices. Map exceptions to category-specific actionable review.
- For any network ambiguity, show pending original UUID and receipt lookup; no duplicate implicit writes on reload/multi-tab.
**P1:** Compare Android setup overview and coordinators with exact Web receipt, skip, groups, pack selection, draft review, and post-activation summary; no offline queued creation/import/activation. Four languages and screen reader/mobile/dark-light acceptance.

## Stable QA criteria and handoff
- Fresh synthetic family A and independent B; parent, child and same-account multi-family checks. Never run destructive tests in real Test/Test2/BETA/Prod.
- Group name duplicate, groups GET failure while import still available, separate people vs WorkNode folder IDs, cross-workspace role denial, stale epoch.
- 142 catalog entries and six packs, TASK/REWARD separation, deduped pack overlap, existing private templates and preview changed before POST.
- Null vs explicit zero price, TIME minutes, difficulty and default descriptions, never auto-ledger/purchases.
- Skip versus COMPLETE, setup disabled, existing-family fallback, double click, lost response after committed import and activation, reload reuses original UUID, no activation on import.
- Actual server receipt, source SHA and accepted API payloads; UI tests alone do not prove domain changes. Android device/instrumentation status separately identified.
- Owner deliverables: **Web** source+Playwright for complete wizard, **Backend** proven create/activation contracts & receipts, **Android** parity and offline isolation, **QA** disposable cross-platform scenario, **Architecture** typed entity/authorization and null/price policy. Designer accepts UX and four-language terminology only.

## Release boundary
No real data wipe, remote server storage change, BETA deploy or reset execute authorized. BKL-002 must independently pass the previously approved recovery gate before release. CI “pending”/“cancelled” is not PASS; pin SHA and actual full test counters.
