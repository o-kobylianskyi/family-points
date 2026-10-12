# FamilyPoints — Development Log

> Branch: `feature/task-soft-delete`  
> Repository: `o-kobylianskyi/family-points`  
> Scope: technical development history, build verification, commits, current blockers and next steps.  
> Product/domain decisions stay in `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`; feature checklist stays in `FAMILYPOINTS_CURRENT_WORK_CHECKLIST.md`.

## Log conventions

- Add a dated entry for each substantial implementation or debugging batch.
- Record exact Git commit SHA and affected modules/files when known.
- Distinguish **implemented**, **reviewed**, **compiled**, **device-tested**, and **not tested**. Never infer runtime success from source inspection.
- Record test commands and outcomes, unresolved errors, and next action.
- Always inspect the current remote branch HEAD before editing; treat GitHub as source of truth. Do not overwrite concurrent changes.
- Port Android features by inspecting the corresponding React component, REST calls, and backend DTOs. Prefer parity with web behavior, adapting layout to mobile.
- Update the feature checklist when the implementation status changes.

---

## 2026-10-10 — Android compile diagnostic and logging setup

**Context**

- Android project: `android/` (Kotlin + Jetpack Compose; direct REST API, no WebView).
- Previous known commit: `f39c082` (Android Points progress).
- Remote branch HEAD observed during diagnostic: `f1c85577700d5d8fd5837e0d7d273691fcb4d325`.
- User synchronized locally with `git pull` from `f39c082` to `f1c8557`.

**Diagnosis / verification**

- Android Studio visually highlighted code in `CreateTaskScreen.kt` around `retrofit2.HttpException` and had earlier reported red markings in `LoginScreen.kt`.
- Inspected the current GitHub `CreateTaskScreen.kt`, `LoginScreen.kt`, `android/app/build.gradle.kts`, and `android/gradle/libs.versions.toml`.
- The `retrofit2.HttpException` import is present and Retrofit 2.11.0 is declared as a dependency; no source correction was justified solely by the red editor highlighting.
- **COMPILED (user-verified locally):** Ran `./gradlew.bat :app:compileDebugKotlin --console=plain` from the Android project in PowerShell; screenshot shows **BUILD SUCCESSFUL in 6s**, six tasks up-to-date. This establishes that the local debug Kotlin compilation succeeded at that checkpoint.
- **NOT VERIFIED:** APK installation, emulator/phone runtime, authenticated API flow, instrumentation tests, full clean rebuild.
- No Android source modification was needed in this diagnostic session.

**Android screens reported implemented in the project**

- Login / language selection / JWT session handling
- Dashboard, Tasks, Task Details, Create Task
- Task Reward Negotiation, Rewards, Points
- Shared authenticated AppHeader and localized calendar

**Next action**

1. Port **Members / Учасники** to native Android after reading the current React page, REST API contracts, backend DTOs, and existing Android navigation shell.
2. Preserve current web behavior wherever possible; use compact mobile controls when necessary.
3. Check remote HEAD immediately before each edit; commit changes to `feature/task-soft-delete`.
4. Have the user validate locally after `git pull`; record the actual build/run results here.

**Logging decision:** Maintain this development log continuously alongside the existing concept log and work checklist.

---

## 2026-10-10 — Explicit Retrofit exception type in Create Task

- **CHANGED:** `android/app/src/main/java/com/olehkobylianskyi/familypoints/android/ui/screens/CreateTaskScreen.kt`.
- Replaced both ambiguous `catch (exception: HttpException)` declarations with `catch (exception: retrofit2.HttpException)` and removed the unused short-name import.
- Purpose: disambiguate from Android SDK `android.net.http.HttpException` and avoid its API-extension inspection warning.
- Source commit: `d8b067dd26434acbf4af6ef9717c0654bde25b8f`.
- **NOT VERIFIED AFTER PATCH:** Gradle compilation / IDE inspection; user needs to run `git pull` and recheck. Previous compilation was successful before this change.

## 2026-10-10 — Retrofit exception naming follow-up

- Android Studio screenshot showed a local stray `import android.net.http.HttpException` restored through `git stash pop`, while Kotlin Gradle compilation completed successfully.
- On the remote branch, `CreateTaskScreen.kt` did **not** contain that Android SDK import; both catches already used the fully qualified Retrofit class.
- Updated imports to `import retrofit2.HttpException as RetrofitHttpException` and both handlers to `catch (exception: RetrofitHttpException)` to make the class intent explicit and avoid name ambiguity. Commit: `26216d65b03771494b0c49c93b770cf345f49069`.
- **NOT BUILD VERIFIED after latest patch.** User should remove the stale local-only change to this file before `git pull` and run Gradle compile locally.

## 2026-10-10 — Local compile verification after Retrofit alias fix

- User confirmed `git status`: branch up to date with origin; working tree clean.
- **COMPILED (user-verified):** `./gradlew.bat :app:compileDebugKotlin --console=plain` -> `BUILD SUCCESSFUL in 2s`, 6 actionable tasks (1 executed, 5 up-to-date).
- One compiler **warning** remains: `CreateTaskScreen.kt:416:13 Expression under 'when' is never equal to null`. Not a compile failure.
- **NOT VERIFIED:** full APK install or post-change runtime/API flows. Android Studio editor annotations may require IDE cache refresh if still displayed.
- Next native page planned: Members, after inspection of matching React/API/backend source.

## 2026-10-10 — Remove remaining Kotlin compiler warning

- Removed unreachable `null` branch from the calendar `when (dateField)` in `CreateTaskScreen.kt`; selection is guarded by `if (dateField != null)`.
- Commit: `6dec0ac1868d27600bb452aac0a482cee266b9c2`.
- **Not build-verified after this change.** User should run Kotlin compilation locally.
- Editor-only `RetrofitHttpException` unresolved highlighting remains unconfirmed as a real compiler error; preceding user Gradle compilation succeeded. Re-sync Gradle and invalidate IDE caches if necessary; do not replace Retrofit's `code()` with SDK property access.

## 2026-10-10 — Fix nullable `when` compile failure

- User reported actual Kotlin compiler error at `CreateTaskScreen.kt:413:24`: `when expression must be exhaustive. Add the null branch or an else branch`.
- The previous removal of the `null` branch introduced a compile failure even though Kotlin had previously warned the branch was unreachable in that context.
- Fixed by adding `else -> startDate` to the calendar selection `when (dateField)`; this preserves the previous fallback and makes the expression exhaustive.
- Source commit: `9bd7879a03d7796854887f3966dc8dbf396be74a`.
- **Not yet recompiled after this fix**; user must `git pull` and run `:app:compileDebugKotlin` locally.

## 2026-10-10 — Android Members initial native page (PARTIAL)

- Verified remote HEAD before implementation: `77c04392798b728f22b5b315de2b48782c16da40`.
- Examined `frontend/src/pages/MembersPage.jsx`, `frontend/src/components/MemberEditor.jsx`, `frontend/src/api/workspaceApi.js`, `frontend/src/api/memberGroupApi.js`, existing Android DTOs, API client, and navigation.
- Added `MembersModels.kt`, `MembersApi.kt`, `MembersRepository.kt`, `MembersScreen.kt`; registered Retrofit API and routed MEMBERS in `MainActivity.kt`.
- Native screen: tabs Members/Groups, member cards, permission-gated create/edit/delete, workspace role selector, basic group creation and recursive group display with cycle protection.
- Source commits: `85a5ba1`, `05a912f`, `762d556`, `7354173`, `a7fdc40`, `e83a06b`.
- **PARTIAL:** Web GroupEditor (group membership, nested group edit, permission/role administration and point management) not yet ported.
- **NOT COMPILE- OR RUNTIME-TESTED** after these changes. User should `git pull` and run `./gradlew.bat :app:compileDebugKotlin --console=plain`; send compiler errors and then test Members on emulator.

## 2026-10-10 — Android Members GroupEditor follow-up (PARTIAL)

- Compared React GroupEditor with the existing MemberGroups REST API before implementation.
- Added group-update request model, group role/member DTO fields, Retrofit endpoints and repository operations for group basic fields, membership, subgroup links and role assignments.
- Added compact Compose group editor dialog with scrollable content, selection dropdowns, add/remove actions and existing role assignment.
- Main source commit: `3f30eb07693fdad35737690bad8b836f3255a875`; preceding API/repository commits: `76f9c88`, `d912b62`, `bdfe6a3`.
- **Not yet complete:** local custom role CRUD/permissions and group economy flows from React editor.
- **NOT BUILT OR RUNTIME-TESTED**: user should run `git pull`, then `./gradlew.bat :app:compileDebugKotlin --console=plain` and test member/group operations in emulator.

## 2026-10-10 — Android local group roles (PARTIAL)

- GitHub HEAD inspected before coding: `b0729269d52cc4a5e8461a1270114c181999e294`.
- Inspected React `GroupEditor.jsx`, group API, native Members screen and DTOs.
- Implemented group-local PRIVATE role create/edit/delete dialogs, name duplicate validation, and protected system/shared roles from edit/delete. Added role DTO/request and matching REST/repository calls.
- Commits: `d16f54f`, `6363497`, `f325c3e`, `3681ec4`.
- **Not included:** permission profiles, scoped permission grant editor, automatic local role naming, group points operations.
- **Build and runtime NOT VERIFIED.** Run local Android Kotlin compile and exercise flows after `git pull`.

## 2026-10-10 — Android GroupEditor role permission profiles (PARTIAL)

- Remote HEAD inspected before changes: `3b315ff36e2a7d04060e65ffc6e6341afe0d9704`.
- Ported existing React permission groups and presets NONE, EXECUTOR, SENIOR, LEADER, CONTROL; included advanced permission toggles and GROUP/GROUP_SUBTREE scope selection.
- Added existing GET/POST/DELETE group permission REST calls and repository reconciliation of grants, plus a role-specific Compose dialog.
- Code commits: `5f1907a`, `4f4792c`, `1940da5`, `1a3f19d`.
- **Not compiled or runtime-tested.** User must verify with `git pull` and `./gradlew.bat :app:compileDebugKotlin --console=plain`; authorization and server-side grant rules remain authoritative.
- Remaining: synchronize grants after role creation in one flow, automatic naming and duplicate permission warning, group balance controls, compact UI testing.

## 2026-10-10 — Android local role naming and preset save (PARTIAL)

- Verified remote HEAD `2147cdfc457e69fdd5bfcc8ecd3c5fbd4a2a934f` before implementation.
- Local role names now default to a localized, collision-free suggestion; selecting a predefined permission profile updates the proposed name only until manually edited.
- Role editor warns when another role has the same predefined permission set. Name duplicates remain blocked.
- Saving local roles now also applies selected NONE/EXECUTOR/SENIOR/LEADER/CONTROL grant presets via the existing REST permission endpoints. CUSTOM preserves existing grants on edit; new CUSTOM applies no preset.
- Source commits: `70877d1`, `6e16f80`, `1ea8d1d`.
- **NOT COMPILED OR DEVICE TESTED.** API role ID resolution after create and partial failure of multi-call updates need smoke testing. Next: verify compilation, then group balances and richer advanced-edit UX.

## 2026-10-10 — Android group balances display

- Inspected current remote HEAD `396188ece7d9e3adc1ab58426870664beadde859` before editing.
- Checked React `GroupPage.jsx` and `memberGroupApi.js`. React currently displays balances by point type; the `addGroupPoints` method exists but no request body example was found in the inspected pages.
- Added a detailed group balance section to the native GroupEditor using server-provided `balances`, `pointTypeId`, `code`, `name` and `amount`; displays empty state.
- Source commit `4e4c15254b195213f86cec998a1060c3990d4424`.
- **No mutation of group balances** added without verifying backend request DTO/permission rules. Kotlin build and runtime not verified after change. Last several Android batches still need local compile verification.

## 2026-10-10 — Group credit/debit operations (UNVERIFIED)

- User confirmed `BUILD SUCCESSFUL` for the preceding Android GroupEditor changes.
- Verified active branch HEAD `81dff7aa4426944dbce60661d7dda07d82404ff7` before editing.
- Located actual Java DTO `GroupPointOperationRequest`: `pointTypeId: Long`, `amount: Integer`, `type: PointTransactionType`, `description: String (max 255)`; types include EARN and SPEND. Confirmed backend endpoint POST `/workspaces/{workspaceId}/member-groups/{groupId}/points`.
- Found missing endpoint-specific backend authorization: global SecurityConfig only requires authentication. Added `WorkspaceSecurity.canManagePoints` enforcing current workspace and MANAGE_POINTS or ADMIN_OVERRIDE, then `@PreAuthorize` on group point mutation endpoint.
- Added native Retrofit DTO/method/repository with positive amounts and EARN/SPEND only; added permission-gated Compose credit/debit dialog with point-type picker, amount, reason, and client-side overspend warning. Backend remains authoritative and currently allows negative group balances unless separately constrained.
- Commits: `4922c7e`, `6e33ed1`, `932e1b6`, `1b57bc5`, `0c4a5e8`, `4ecfe11`.
- **NOT COMPILED OR RUNTIME-TESTED after latest changes** (Android and backend). Needs Kotlin and Maven compile, server deployment before testing group point mutation in emulator. Any negative-balance business rule requires dedicated backend enforcement rather than relying on mobile preview.

## 2026-10-11 — Group ledger regression tests

- Previous backend changes added pessimistic group-row lock and insufficient-balance rejection for manual credit/debit.
- Added JUnit/Mockito unit tests in `MemberGroupPointsTest.java` for insufficient balance (no ledger write) and zero/negative manual amounts. Commit `e02eb17`.
- Inspected the member group service and ledger repository. A repository-wide exhaustive inspection of every Java writer was not completed; other write paths require separate audit.
- **Tests not executed yet**; local Maven run required. Integration concurrency tests also remain open.

## 2026-10-11 — Compact Android Groups tree and translations
- Based on emulator screenshot: replaced tall nested group cards with compact rows, labeled member/subgroup counts, toggle for expanding group contents, indented subgroups, and hid zero balances in overview (full balances remain available in editor).
- Localized GroupEditor permission categories, individual permission names, profiles, scope labels and member-type selector in UK/DE/EN/RU.
- Git commits: `7a7c2a0`, `420013c`, `59683d5`.
- Source not compiled or runtime-tested after this UI update; check Android build and group hierarchy behavior on emulator.

## 2026-10-11 — Android native Settings, phase 1
- Verified remote HEAD `9f016d2280bb8d74dbd22e07e4ded4f1acf9d1d9`; inspected React SettingsPage and accountApi routes.
- Added native Settings screen to replace main navigation placeholder, including current-user password change and MANAGE_MEMBERS/ADMIN_OVERRIDE account list, account creation/update/login status and optional password reset.
- Added SettingsModels, SettingsApi, SettingsRepository and registered Retrofit client; wired screen in MainActivity.
- Commits: `2790877`, `fb39b5c`, `260747a`, `5fff5af`, `3d678bb`, `18f94e7`.
- **Not compiled or runtime-tested yet.** Still to port RoleCatalogSettings, refine mobile UX, verify API 204 responses and failure handling.

## 2026-10-11 — Android Groups edit button alignment
- Fixed screenshot issue where nested groups shift the “Змінити” action left/right: root groups retain a single Card; nested groups render as full-width rows rather than nested padded cards. Hierarchy is indicated by a left-arrow prefix, while all Edit buttons use the same trailing row alignment.
- Source commit: `a66e20a`. Build/emulator verification pending.

## 2026-10-11 — Android Settings role catalog (phase 2)
- Compared React `RoleCatalogSettings.jsx` and `roleCatalogApi.js` with native Settings.
- Added role sets and role definition models, Retrofit endpoints and repository for CRUD.
- Added localized native role catalog cards/dialogs with editing of custom sets and non-system roles, delete confirmation, set assignment and management permissions.
- Mounted RoleCatalogSection in Settings and wired repository from MainActivity.
- Commits: `6dbfe4a`, `7c2e55e`, `469c88d`, `818b1b5`, `dfe7c1e`, `c3b02e4`, `4214943`.
- **Compilation/runtime NOT verified.** Run Android Kotlin Gradle compilation; then test API role catalog read and CRUD. UI polish, server-side permission validation and translated system role names remain.

## 2026-10-11 — Android member editor field clarity and role translations
- Added explicit localized labels above Member type and Workspace role dropdowns in the member dialog.
- Localized built-in workspace roles FAMILY_ADMIN/PARENT/CHILD using role code, preserving custom names.
- Defaulted new members to CHILD workspace role when available rather than blindly selecting first role.
- Source commit: `51ee1da`. Not compiled or runtime tested yet.

## 2026-10-11 — Reward tabs responsive layout and Points navigation
- User selected adaptive option A. Located screenshot's 3-tab layout in `RewardsScreen.kt`, not DashboardScreen.
- Replaced 3 equal-width Row tabs with 2-per-row FlowRow; long labels can wrap to at most two lines with ellipsis, and request count appears as separate Badge.
- Fixed AppHeader's Points navigation to nominative UK «Бали» / RU «Баллы»; the Dashboard balance quantity string «балів» remains correct.
- Source commits `9f0a0af`, `1a8c1da`. Build/emulator verification pending.

## 2026-10-11 — Configurable currency declensions (phase 1, unverified)
- Custom currencies already existed as PointType (code/name/order/active), without localized quantity forms.
- Added V18 migration `point_type_name_forms` for UK/RU/EN/DE: one/few/many, unique per point type + language, scoped by point type FK.
- Added Java entity/repository/DTO and secured GET/PUT endpoints under /workspaces/{workspaceId}/economy/point-types/{pointTypeId}/name-forms; writes require MANAGE_ECONOMY or ADMIN_OVERRIDE and workspace membership.
- Added reusable Android `formatPointAmount` with Ukrainian/Russian 11–14 exceptions, singular/plural EN/DE, custom forms override and built-in POINTS fallback.
- Added Android Settings currency form editor with language selection and sample quantities 1/2/5/11/21.
- Reward catalog prices and request prices load and use configured forms; old backend fallback tolerates unavailable endpoint.
- **Partial integration**: other Android screens (balances/history/tasks/group points) and React still require migration to the shared quantity formatter and configured forms. Built-in COPPER/SILVER/GOLD fallback remains existing display name until configured. New backend migration and Android source **not compiled or tested**.

## 2026-10-11 — Currency morphology cross-client screen audit (SOURCE ONLY)

- Reviewed Android screen inventory (Login, Dashboard, Tasks, TaskDetails, CreateTask, RewardNegotiation, Rewards, Points, Members/Groups, Settings/RoleCatalog/CurrencyForms), and React pages (Dashboard, Tasks, TaskDetails, Members, Group, Points, Rewards, Settings, Login) with relevant modal/components.
- Web: introduced centralized `currencyQuantity` and `useCurrencyQuantity`; API read/save name forms; new currency forms editor in Settings; applied display formatting to Rewards catalog/request prices, Dashboard, Tasks, TaskDetails, Group balance, and Points balance.
- Android: shared `formatPointAmount` already exists and Rewards reads configured forms; updated Dashboard and Points balance and TaskDetails quantities to format built-in POINTS properly.
- **Remaining**: Android Tasks screen reward labels, group balances/member editor and currency history rows, CreateTask/negotiation currency previews, React point history and role/task forms; propagation of configured custom forms into all Android screens (currently Rewards only); standard forms for COPPER/SILVER/GOLD; translation gaps unrelated to currencies.
- **Builds, migrations and runtime QA not executed by assistant.** Screens audited by reading source, not clicking through UI. Request local Maven, Gradle, npm build and smoke-testing with the new V18 backend deployed. Do not mark full coverage complete.

## 2026-10-11 — Android dashboard quick action and shared header translations
- Fixed quick action navigation title `балів` -> `Бали` in DashboardScreen; preserved declension for numeric balance content.
- Translated current workspace role in shared AppHeader based on workspaceRoleCode (FAMILY_ADMIN, PARENT, CHILD) for UK/DE/EN/RU; custom role names remain unchanged.
- Commits: `1be2005`, `72b0d81`. Compilation/emulator verification pending.

## 2026-10-11 — Localized Android GroupEditor system role and currency labels
- Screenshot exposed untranslated POINTS/COPPER/SILVER/GOLD in group balance rows and system roles LEADER/DEPUTY/SENIOR/EXECUTOR/ASSISTANT/REVIEWER/OBSERVER.
- Added centralized group-editor label helpers that translate by stable `code`/`systemCode` for UK/DE/EN/RU, retaining user-defined names unchanged. Applied to overview balances, editor balance list, selection dialogs, and role listings.
- Commit `9f12445`. Not yet compiled/runtime tested.

## 2026-10-11 — Android Settings system currency and role localization
- Screenshot exposed raw English default point types (POINTS/COPPER/SILVER/GOLD) in currency declension settings and default role sets/roles/descriptions in Role Catalog Settings.
- Added shared `SystemCatalogLabels.kt` mapping built-in currency names, role set names/descriptions (MANAGEMENT/EXECUTION/CONTROL), and system role names (LEADER, DEPUTY, SENIOR, EXECUTOR, ASSISTANT, REVIEWER, OBSERVER) into UK/DE/EN/RU using stable system codes. Custom entries keep their own names and descriptions.
- Applied to `CurrencyFormsSection.kt` and `RoleCatalogSection.kt`, including role set selection.
- Commits `8391fe1`, `09a4e5d`, `7d7ea74`. Kotlin compile and emulator verification pending.

## 2026-10-11 — Web Members role badge localization
- Screenshot showed translated member type but untranslated workspace role badges (Parent / Child).
- Added shared `workspaceRoleLabel` using stable role code, preserving custom role names; added `workspaceRoles` translations for uk/de/en/ru.
- Applied to MembersPage badges and MemberEditor role dropdown. Changes are source-only; web build and browser verification remain pending.

## 2026-10-11 — Web Settings currency editor layout and saved-state UX
- Screenshot showed currency selector aligned beneath editor due to reuse of password form `.settings-form` grid, raw English default currency labels, and no indication whether forms were saved.
- Reworked `CurrencyFormsSettings.jsx` into independent `currency-forms-*` grid with scoped responsive CSS in `App.css`. Picker is left and language/forms/preview on right at desktop widths; stacks on narrow screens.
- System currency list uses localized POINTS/COPPER/SILVER/GOLD labels based on stable code; user-defined names are untouched.
- Existing saved forms are loaded per point type, populated into inputs and marked as saved. Built-in recommended forms are shown when no saved forms exist, explicitly marked as **not saved**. Unknown/custom currencies without saved forms show **not configured**.
- Added preview 1/2/5/11/21, load/error/saved statuses, scoped input/button styling.
- Source commits: `c5cf837`, `6024a16`. Browser and npm build not executed by assistant.

## 2026-10-11 — Web role set description localization
- Fixed untranslated Default management/execution/control roles descriptions in React RoleCatalogSettings. The existing i18n keys roles.roleSetDescriptions already provide Ukrainian, Russian, German and English translations, but the UI displayed raw set.description from the backend. Now descriptions use systemCode and translated keys; custom set descriptions remain unchanged.
- Commit `36413bc`. Web build and browser smoke test pending.

## 2026-10-11 — Android Settings member account role labels
- Fixed screenshot showing Parent and Child as raw server strings in Settings account cards. System roles now translated by workspaceRoleCode for UK/DE/EN/RU; custom role names stay unchanged.
- Header UK PARENT label changed from plural «Батьки» to requested singular «Батько». In account lists, where the PARENT role can belong to any parent, Ukrainian label is «Батько / мати».
- Commits `343a632`, `10f5a06`. Kotlin build/runtime verification pending.

## 2026-10-11 — Web parent role and duplicate badge cleanup
- Web app header uses individual-neutral PARENT label ('Батько / мати' in Ukrainian) rather than plural 'Батьки'. We cannot distinguish father and mother from existing PARENT role data; a per-member preferred title is still needed for gender-specific labels.
- Added PARENT_INDIVIDUAL translations for UK/RU/DE/EN.
- Hide redundant member type on member cards and in Settings account summaries when it equals workspaceRoleCode (e.g. PARENT/PARENT, CHILD/CHILD), preserving both for genuinely distinct custom roles/types.
- Source only; npm build/browser test pending.

## 2026-10-11 — Build audit and Android app-control foundation
- Current branch at audit start: `5ecca98014196ba16f09d3fb7f6a5663f0c85650`. Verified Maven/Gradle/npm project entry points and migration numbering (V18 is unique).
- Source-checked critical Java economy permissions (`MANAGE_ECONOMY` exists), GroupPointsTest exception getters, Android Rewards/Points currency paths, AndroidManifest, compile/target SDK 37, and Android routing. This **does not constitute successful local builds**: repository is not available to the local container and remote clone was blocked by DNS; no GitHub Actions workflows exist.
- Remaining audit risk: custom currency forms only fetched in Android Rewards; other UI screens show built-in fallback rather than configured custom forms. Backend concurrency requires integration test; Gradle/Maven/npm builds should be run on user's laptop.
- Started Android app-control research implementation: created read-only `AppUsageReader.kt` with explicit `Usage Access` check and usage aggregation, plus manifest `PACKAGE_USAGE_STATS`. No usage permission bypass, apps list UI, blocking or forced closing is implemented. Apps visibility/Android background execution restrictions still require design.
- Commits `f0af88a`, `73732ba`.

## 2026-10-11 — Device control, parental override, temporary child mode (phase 1)
- Agreed architecture has STANDARD Android, DEVICE_OWNER Android, and TEMPORARY_CHILD_SESSION on a parent's handset.
- Created Flyway V19 device-management foundation schema: managed_devices, device_installed_apps, device_app_policies, temporary_child_sessions, device_parent_override_sessions. Parent devices keep their permanent owner while a temporary child session borrows the device. A partial unique index prevents concurrent ACTIVE sessions on one device.
- Separate admin_visible/child_visible from access_mode. Enforcement capability remains device-dependent and must not be represented as guaranteed in Standard Android.
- Session charge modes STANDARD/FREE, bounded expiry, active/complete status and parent override audit data are foundational only. Password storage, PIN verification, call whitelist and application enforcement are NOT implemented in this commit.
- Build, migration test and UI integration remain pending. Commit `dc07518`.

## 2026-10-11 — Android offline balance cache wired
- Android OfflineBalanceStore (platform SQLite) now wired into PointsRepository and MainActivity. The server is always attempted first; if balance fetching fails due to IOException, last locally saved balance is displayed. History failures remain independent.
- PointsScreen explicitly indicates offline cached balance and hides manual balance operations while using the snapshot. If member listing fails due to IOException, cached current-member balance remains viewable based on existing authenticated currentUser data.
- Commits: `22e86d5`, `4069983`, `bb289b1`, `1852b95`. Kotlin compilation not yet verified.
- Limitations: no offline login/session hydration, rewards/policy cache, encrypted cache, transaction queue or offline spending yet; cached balances are for display only. Session identity and logout/account-switch scoping require continued hardening.

## 2026-10-11 — Android offline identity storage (phase 1)
- Added Android Keystore AES-GCM encrypted `OfflineAccountStore.kt` containing the last successfully authenticated `CurrentUserResponse` profile (including workspace/member IDs). No plaintext password is stored.
- `AuthSessionManager` saves the verified profile after online login/session validation and clears it on explicit logout or invalid session. Network IOException during restore no longer destroys the token/profile automatically. `MainActivity` instantiates the profile store.
- Offline login is **not yet enabled**. Next requirements: explicit opt-in local PIN enrollment, PIN verification/rate limiting, per-account separation, offline session expiry and synchronization/revocation semantics. A cached profile alone is never proof of authentication.
- New files/commits: `1bd0e70`, `572fa35`, `b0169cc`, `830d55c`; compile not run.

## 2026-10-11 — Android cache-first balance with refresh status
- PointsScreen shows stored current-user balance immediately while member list and new balance sync in background.
- Displays ⟳ refreshing, ✓ updated, or offline/last saved snapshot. Manual point operations unavailable until online balance confirmation.
- PointsRepository separates balance from history; history fetched independently after balance to avoid slowing refresh indicator.
- Commits `ca128ca`, `3b55c1e`, `cae00ed`, `afc0028`, `ba557e8`. Compile/runtime test pending.

## 2026-10-11 — Rewards timeout mitigation and offline catalog snapshot
- Screenshot: Android Rewards shows `timeout` and no catalog. Cause: seven aggregated concurrent requests with failure of an auxiliary request rejecting entire load.
- Added `OfflineRewardsStore` display-only workspace-scoped JSON snapshot (catalog, categories, point types, tasks). Requests/purchases/obligations excluded from stored snapshot to avoid presenting stale transactions as current.
- Android loads saved snapshot first, attempts server refresh next, and shows refreshing/offline status; auxiliary request IO failures are nonfatal to the catalog. This first-stage snapshot uses SharedPreferences and is not encrypted; review storage hardening before storing private/sensitive data.
- No offline purchasing supported, backend still owns purchases and point balance. No authenticated offline login or UI tests yet. GitHub commits `ad37cbd`, `5d2d8af`, `ab96ab2`, `1d62651`, `fb1263a`.

## 2026-10-11 — Android Settings offline guard
- Settings uses Android ConnectivityManager to detect absent network on entry and skips loading server-only settings in that state; change-password, currency forms, role catalog and account management are hidden while offline. A localized offline notice replaces repeated timeout errors. Language switcher remains available.
- IOException from Settings member/account load flips this screen into offline state.
- Added ACCESS_NETWORK_STATE permission in AndroidManifest.
- Note: Internet network presence does not prove backend availability. Currency and role widgets can still time out independently when backend is unreachable despite connectivity; a later shared API reachability state is required. Kotlin build not executed.
- Commits `a9e4ff0`, `efc3b2a`.

## 2026-10-11 — Shared Android backend reachability
- Implemented global `ServerReachability` StateFlow statuses CHECKING/ONLINE/OFFLINE. A shared OkHttp interceptor observes API responses and transport errors. Network connection alone is not treated as evidence of backend availability.
- `ApiClient.probeServer()` checks unauthenticated GET /api/me, accepts HTTP 200/401/403 as reachable, treats unexpected redirects, gateway errors and IO failures as unavailable. Probe has 6-second call timeout. Automatic checks run every 60 seconds online and 20 seconds offline while UI is active.
- MainActivity displays a localized shared offline banner on all routes with a manual Retry button. This banner reserves screen space rather than overlaying other controls.
- SettingsScreen uses the shared backend status instead of Wi-Fi presence. Server-dependent controls hide while offline; settings reload when backend becomes available. Fixed a previously malformed if/else/try/finally construct from the previous Settings changes.
- AuthSessionManager retains a still-active client session on IOException and HTTP 502/503/504 during refresh, while still rejecting unauthorized/invalid sessions and honoring the configured idle timeout.
- No offline PIN login or cached local authentication has been enabled by this work. UI display and integration require a verified Android build.
- Main commits: `aa94ff3`, `7000397`, `e9aba16`, `8b00757`, `d0b69ad`, `f8544c3`, `8b8f28b`, `e8318ea`.

## 2026-10-11 — Offline catalog fix and read-only dashboard/tasks snapshots
- Investigated Rewards blank screen while API offline. Root causes: cached catalog was only saved after 7 concurrent endpoints completed; a single essential timeout or long auxiliary request prevented saving, and UI hid existing cache while loading.
- Refactored RewardsRepository into `refreshCatalog` (fetches only rewards and immediately saves a member-scoped offline catalog of titles, descriptions, price, requirements) and `refreshSupplementary` (optional requests, categories, purchases, obligations, point types and definitions, with independent timeouts). RewardScreen now displays cache during refresh, recovers after backend reconnect and prevents offline purchases or submissions. OfflineRewardsStore no longer persists requests/purchases/obligations/tasks and keys by workspace+viewer.
- Added OfflineDashboardStore keyed by workspace/member/date. DashboardRepository writes snapshots after successful load and DashboardScreen renders them immediately (read-only and marked cached) while checking server.
- Added OfflineTasksStore keyed by workspace+viewer+target member/date. TasksRepository saves daily task snapshots and TasksScreen loads cache first, shows member fallback and disables create/claim/status actions while offline/stale. Management and task-detail screens are still not cached.
- **Limits:** one successful online catalog/task/dashboard load is needed before those screens can work offline; no offline login/PIN, offline purchases or task status submissions yet. Cache snapshots currently use application-private SharedPreferences and should be protected by encryption before production, and synchronization/conflict tests remain pending.
- Builds were NOT run in this tool environment. Main code commits: `cc0e3a7`, `62a5e2c`, `a24a6ca`, `5687526`, `d0c515d`, `d6af1fa`, `a3ed3b8`, `d13fb3b`.

## 2026-10-11 — Verified-password offline Android login
- Screenshot showed login always failing without backend even when profile/balance cache existed. The previous `OfflineAccountStore` stored only a profile and offered no local password authentication.
- Implemented per-username encrypted offline account records using Android Keystore AES-GCM, salted PBKDF2-HMAC-SHA256 (210,000 iterations) verification and local failed-attempt throttling (5 attempts -> 5 min). Password is **never stored in plaintext**. Each online password login enrolls/refreshes the account record; the enrollment expires after 7 days without verified online login.
- `AuthSessionManager.login` now automatically falls back to offline password verification if API status is offline, a network IOException occurs or HTTP 502/503/504 is returned. HTTP 401/403 is never accepted as offline fallback. Manual logout preserves encrypted enrollment but drops server token. Invalidated sessions can remove the enrollment.
- Offline login clears any JWT, stores only an in-memory offline-authenticated mode and updates idle activity. App requires a fresh **online login** when API reconnects; cached offline identity does not grant server write operations. User inactivity still expires at 1 hour.
- `LoginScreen` shows local sign-in wording and differentiates missing enrollment, expired access, wrong password and rate limiting from backend errors across UK/DE/EN/RU. `PointsScreen` now bypasses token-required API requests when the server is offline and reads cached balance.
- Requires successful online login **once after installing the new version**; earlier encrypted profiles lacked password verifiers and cannot be retroactively trusted. Offline access is currently read-only and does not enable offline purchase or task submissions.
- Source-only changes; **Kotlin compilation/emulator tests not confirmed**. Next: multi-account chooser UI, explicit offline enrollment management, tests for clock changes, storage tampering, logout/revocation and connection restoration.

## 2026-10-11 — Backend reconnected, but Android catalog/tasks stayed stale
- User observed global offline banner disappearing after backend restart while Rewards still showed "offline saved catalog", and Web-created task was not reflected on Android.
- Root cause: old unauthenticated `GET /api/me` health test accepted generic HTTP 401/403 (including proxy responses) as ONLINE; shared API interceptor also allowed generic status responses to set ONLINE. UI lacked explicit per-screen refresh and existing cache could remain stale after a failed catalog request.
- Backend now exposes unauthenticated `GET /api/client-health`, returning controlled JSON `{"service":"family-points","status":"UP"}` through `HealthController` (new endpoint has no private metadata), and `SecurityConfig` permits that route.
- Android `ApiClient.probeServer` validates 200 + expected JSON identity using independent short-timeout HTTP client and `Cache-Control: no-cache`; `ServerReachability` allows only validated probe to transition into ONLINE, not arbitrary HTTP status codes. During manual retry, the offline banner stays visible until verified recovery.
- Rewards, Tasks and Dashboard gained explicit localized refresh buttons and a 30-second online refresh while screens are visible. Rewards distinguishes "cached because offline" from "still cached after failed online refresh", and keeps read-only cache until catalog GET succeeds.
- **Both backend and Android must be updated**. Prior Android APK polling /api/me is incompatible with the new health contract. GitHub changes require Maven/Gradle compilation and emulator tests; not yet compiled here.

- Additional reconnection UX: offline-authenticated sessions without a server JWT still require interactive online login after reconnection. Android now displays a localized explanation on LoginScreen instead of silently returning to login (`add13ba`, `269c83c`).

## 2026-10-11 — Offline member balances and deferred parent point operations
- Screenshots showed Points only cached current member and Members failed with "Missing access token" in offline-only sessions.
- Added `OfflineMembersStore` for workspace+viewer scoped member/page snapshots, and wired `MembersRepository`, `MembersScreen`, `PointsRepository`, and `PointsScreen` to show cached member names/roles and last confirmed balances without a JWT. With backend online, member balances are prefetched and cached independently.
- Added `OfflinePointsQueueStore` (durable SQLite with UUID operations) for offline **EARN and PENALTY only**. UI lets an authorized parent queue actions while offline; queued amounts and counts are shown distinctly from confirmed balance. SPEND remains online-only.
- Added backend Flyway V20 `point_transactions.client_operation_id UUID` unique partial index; new `POST /workspaces/{workspaceId}/members/{memberId}/points/offline/{operation}` with `OfflinePointOperationRequest` and `@PreAuthorize(...canManagePoints...)`. `PointService.applyOfflineOperation` locks the target member, looks up duplicate UUID, compares payload and returns original result rather than applying a second time. Also added `MANAGE_POINTS` authorization for legacy manual earn/spend/penalty endpoints.
- Android retries locally queued operations after authenticated backend reconnect or when opening Points online. Shared `Mutex` serializes local replay, successfully acknowledged UUIDs are deleted from queue and confirmed balances refreshed; failed items are retained for later intervention. Server revalidates permissions.
- Cross-device economy safety: unconfirmed offline earn/penalty **must not be counted as spendable**; no offline purchases, spending, or confirmed balance mutation.
- Important limitations: newly installed APK must visit Members/Points online once to populate per-member caches. Offline store is app-private but not encrypted yet; encryption, account expiry and revocation handling still need testing. Backend/Maven + Android/Kotlin builds **not executed** in this tool environment.

- Added manual "Sync now" action on the Points screen when pending entries remain and backend is ONLINE; HTTP 403/400 errors remain queued with localized explanation. Pending deltas use Long arithmetic to avoid integer overflow. Commits `92c38aa`, `4026d20`.
- Corrected `OfflineMembersStore.cachedMembers` Kotlin expression-body return issue (commit `5af9b52`). Online cache and offline queue actions require first successful member/balance sync; remote backend migration V20 should run on backend restart.

## 2026-10-11 — Currency selection for manual/offline points operations
- User reported Android offline penalty dialog ambiguously says "10" without a currency. Added localized point-type selector in `PointsScreen` for Earn, Penalty and online Spend, including system and custom point types. The selected type is included in the quantity field label, and the modal can scroll on narrow phones.
- New `OfflinePointTypesStore` caches active currency catalog per workspace/viewer; `PointsRepository.refreshPointTypes` loads actual backend definitions when online. Without a previously synced catalog, currency-changing operations remain disabled offline rather than assuming a default type.
- `OfflinePointsQueueStore` migrated SQLite schema from v1 to v2 with nullable `point_type_id/code/name`; previously queued entries are preserved with NULL and retain the old default-currency replay behavior. New offline entries persist selected type, code and display name. Pending totals on UI are grouped by currency, not added across incompatible currencies.
- Android `OfflinePointOperationRequest` now includes nullable `pointTypeId`. Backend replay endpoint passes that ID to `PointService.applyOfflineOperation`, verifies type belongs to the workspace, checks active status for new writes and includes currency ID in UUID-payload idempotency comparison.
- Added backend `PointOperationTypedRequest` and protected `POST /workspaces/{workspaceId}/members/{memberId}/points/manual/{operation}` for currency-specific online Earn, Penalty and Spend; all require `MANAGE_POINTS` and server-side validation.
- Existing old endpoint remains for compatibility. No new Flyway migration required (SQLite migration happens locally; server point type ID already exists).
- GitHub source changed; Kotlin/Java builds and online/offline functional tests not run here. Verify chosen SILVER/GOLD currency balance in PostgreSQL after an offline replay and test legacy pending entries.

## 2026-10-11 — Points card stays stale while chip updated
- Emulator report: after an offline +20 earn and server sync, Denys chip correctly showed 130 while main Points card still displayed old 110 until leaving/reopening page.
- Fixed `PointsScreen.kt`: a single `memberBalances` map now updates on balance cache/network retrieval and after both automatic/manual offline-queue synchronization, and the visible selected-member amount uses that shared map. This removes dependence on route re-entry for the selected card to reflect confirmed balance.
- Confirmed at source level that the branch already includes a localized currency dropdown for earn/spend/penalty; the queue persists pointTypeId/code/name (SQLite version 2), and backend typed operations/replay accept pointTypeId. The user's currency-less dialog reflects an earlier installed APK; update Android after pulling.
- Commit `253cffa`. Android compile/device tests not verified.

## 2026-10-11 — Quick manual point amounts (Android)
- User requested faster manual earn/spend/penalty form: default 20, later use most recent amount, adjust by ±10 without keyboard.
- Added app-private `PointQuickAmountStore` keyed by workspace, signed-in account, operation (earn/spend/penalty), and currency ID. Values are only remembered after a confirmed online operation or successfully enqueued offline operation; no points or passwords are stored in this preference.
- Updated `PointsScreen.PointOperationDialog`: starts at saved/default amount, restores currency-specific amount on currency change; added −10/+10 buttons with non-negative/max 100000 bounds and preserved manual numeric input.
- Commits `1f29cfc`, `5926fc9`. No backend changes; Android compile and emulator verification pending.

## 2026-10-11 — Quick point amount and reward-linked spending
- Screenshot followed by interrupted ChatGPT reasoning: "Thinking stopped" is a ChatGPT response interruption, unrelated to Android app execution.
- The branch already contained `PointQuickAmountStore.kt` and corresponding PointOperationDialog changes: default 20 or last successfully used amount per workspace/viewer/operation/currency, step +/-10; this was verified in source.
- Extended Android Points spend dialog with a selectable reward catalog (active, non-approval, direct-capable only), currency/price preview and normal manual debit option. A selected reward triggers a **real purchase** via an authorized parent-on-behalf API, not a second manual spend. Reward prices and currencies are server-authoritative and the underlying RewardService still validates requirements and reputation.
- New backend endpoint `POST /workspaces/{workspaceId}/rewards/{rewardId}/purchase-for-member/{memberId}`, guarded by `MANAGE_REWARDS`/admin workspace permission. It calls existing `RewardService.purchaseReward` and links the point deduction to REWARD_PURCHASE and purchase ID. Request-required rewards are not listed in quick purchase.
- Android RewardsApi/RewardsRepository include the parent purchase API. PointsScreen reads cached reward list first and refreshes online; reward purchases require online connection and MANAGE_REWARDS. An expired reward selection never silently becomes a manual debit.
- Code commits `a59fb3a`, `a91081a`, `9cf0d3c`, `b27e650`, `c1b9950`, `5546fa8`, `7b3f289`. Source-level checks only; Maven/Gradle builds and emulator tests remain unverified.
- Follow-up: idempotency/retry safety for purchase-for-member, broader reward eligibility UX, better server error text and consideration of request/approval flow.

## 2026-10-12 — Prepare Contabo Beta deployment
- Decision: use currently paid but underused minimal Contabo VPS for continuously running family Beta, retain local laptop as DEV. Existing Cloudflare/Nginx/WireGuard arrangement and any Apache on port 80 must be inspected before changing ports.
- Beta plan: HTTPS `family-point.com` website + API, isolated PostgreSQL, backend Spring Boot systemd, static React build, signed release Android APK. Keep `dev.family-point.com` and Android `api-dev.family-point.com:8443` on DEV.
- Prepared `frontend/src/api/baseUrl.js`: production same-origin HTTPS by default, build-time `VITE_API_URL` override; existing Vite DEV hostname routing preserved. Both auth and general HTTP clients use the new helper.
- Android debug and release now use distinct BuildConfig API base URLs; release may override `-PfamilyPointsReleaseApiUrl`; debug still targets DEV. Disabled Android OS backup of private Beta application data.
- Added `deploy/beta/README.md` with first read-only VPS diagnostics, install sequencing, security and smoke-test checklist. No server changes or tests run by these commits. Next prerequisite: user runs SSH diagnostics and shares nonsecret outputs so actual deployment can be safely adapted.

## 2026-10-12 — Beta scope changed: empty workspace, setup wizard and templates
- User explicitly does NOT want to copy unusable local development database into Contabo Beta. Instead wants multiple fully independent families, family setup on first launch, participants and roles, plus selectable starter task/reward templates.
- Audit of current backend: Workspace is the family aggregate; UserAccount is tied to exactly one member/workspace. RoleCatalogBootstrapService, WorkspaceRoleService.createDefaultRoles and EconomyService default POINTS provide useful building blocks.
- No public signup/first workspace admin route exists. WorkspaceController has authenticated but unscoped GET all, GET/PUT/DELETE by ID (critical multi-tenant vulnerability); EconomyController has several similarly unchecked endpoints. Must secure before public multi-family Beta.
- Added docs/MULTI_FAMILY_ONBOARDING_PLAN.md containing six-step wizard, versioned localized templates, idempotent import, and development order. Updated deployment docs to remove DEV DB migration suggestion. A V1 pre-V2 schema migration still needs to be designed and tested for a blank Beta DB; schema-only V20 baseline is a carefully controlled alternative, no records.

## 2026-10-12 — First implementation package: tenant isolation for clean multi-family Beta
- Secured workspace root: GET /workspaces lists only the signed-in user's own family; GET /workspaces/{id} restricted to membership, PUT requires MANAGE_WORKSPACE, unrestricted POST and DELETE now explicitly return 403 while safe transactional onboarding/deletion flows are designed.
- Added WorkspaceIsolationFilter to the Spring Security chain **after JWT auth**: any authenticated request to /workspaces/{workspaceId}/... with a workspace ID different from the token's account family returns 403 before entering a controller. No per-servlet automatic filter registration. Method-specific privileges remain mandatory defense-in-depth.
- Expanded WorkspaceSecurity with canManageWorkspace/canManageMembers/canManageRoles, all scoped to current family. Protected EconomyController currency/settings modifications with MANAGE_ECONOMY; group mutations and permission grants temporarily require MANAGE_MEMBERS or MANAGE_ROLES; nested group-point writes retain MANAGE_POINTS check. Group scoped delegation refinement must be addressed before enabling delegated management in Beta.
- Added workspace guard to remaining read routes (WorkspaceRole, MemberExceptionPeriod, Point, RoleDefinition and RoleSet), protected default role creation and exception period creation. PointExchangeController now restricts rate editing/review to MANAGE_ECONOMY and checks parentMemberId equals the authenticated reviewer to prevent impersonation.
- Added WorkspaceIsolationFilterTest and WorkspaceControllerTest unit tests (no server/database required). They have not been run in the execution environment. Before public Beta: Maven test, PostgreSQL integration tests with two unrelated families, audit IDs nested inside request bodies, backend registration wizard and V1/bootstrap.

## 2026-10-12 — Clean database bootstrapping helper, no DEV records
- Investigated historical annotated tag v0.3-workspace (commit fac3adf827eb191b62445cd6c7c5bcb7662fa158). Its tree contains pre-Flyway mapped entities but no migrations V2–V20.
- Prepared scripts/db/generate-v1-baseline.ps1. It uses a disposable PostgreSQL Docker container bound to an automatically allocated localhost port and a temporary Git worktree; builds the tagged Spring Boot app with Flyway disabled and Hibernate DDL create. It exports only DDL to V1__baseline_schema.sql and checks for essential legacy tables; excludes psql-only backslash directives. It then creates a second *empty* test database and validates fresh V1–V20 and current Hibernate entity mappings. Failure retains logs and does not mark SQL validated. Cleans temporary DB/worktree.
- This script is not yet executed on the user's Windows laptop; a generated V1 migration file is therefore NOT yet committed. It never reads or transfers real DEV PostgreSQL accounts/tasks/transactions. Docker and Java 21 are required on laptop.
- Removed default dev user 'oleh' from React login, because a fresh multi-family Beta must start without a preselected identity.

## 2026-10-12 — Windows PowerShell Flyway V1 generator diagnostics
- User confirmed PostgreSQL 17 Docker image pulled and historical Maven phase completed, but git.exe failed to delete the temporary worktree in the script's finally block with Windows "Invalid argument". This may mask a preceding Maven/JPA/DDL failure; neither schema generation nor validation is yet confirmed.
- Patched scripts/db/generate-v1-baseline.ps1 so cleanup failures are warning-only, the original thrown error is reported, and any V1 created but not validated is archived to the per-run diagnostic directory for safe reruns.
- Need user to inspect the previous run's stdout/stderr logs in %TEMP%/fp-v1-c05be7abe52e and check if src/main/resources/db/migration/V1__baseline_schema.sql exists before rerunning; don't claim V1 completed.

## 2026-10-12 — Flyway first validation feedback: duplicate PostgreSQL public schema
- User ran V1 generation. Java 21, Docker Desktop and PostgreSQL 17-alpine worked; generated local `V1__baseline_schema.sql`, but validation failed with PostgreSQL SQLState 42P06: `CREATE SCHEMA public` in V1 was incompatible with PostgreSQL 17's pre-created public schema. Earlier cleanup tried to delete temp Git worktree and failed with Windows `Invalid argument`.
- Updated `scripts/db/generate-v1-baseline.ps1` with `-ValidateExisting` to preserve existing generated V1, skip the historical Git worktree, normalize `CREATE SCHEMA public;` to IF NOT EXISTS, and retry V1..V20/JPA validation against disposable PostgreSQL. Fresh generation now normalizes the schema statement too.
- Validation success has not been reported. Do not mark V1 done or deploy Beta before user confirms PASS and SQL is committed.


## 2026-10-12 — Multi-workspace identity refactor: additive database foundation
- Confirmed existing one-to-one `UserAccount.workspaceMember`, and `CurrentUserService` derives current workspace from this legacy link.
- Added Flyway `V21__account_workspace_memberships.sql` in commit `48d4a9e`: account-to-member membership join table with unique member binding, indexes and data-preserving backfill from existing user accounts.
- Legacy `user_accounts.family_member_id`, existing authentication, group permissions, DEV data and APIs intentionally remain untouched.
- **NOT YET VERIFIED:** Maven tests/Flyway execution on current laptop; no real multi-workspace sign-in, switcher, public account registration or invitation logic yet.
- Next: run Maven Testcontainers smoke test; introduce account membership entity/repository, transactional join/invite flow, explicit active workspace security context, migration of permission resolution and account management, two-family integration tests, and only then expose registration.
- Future personnel management remains a template/domain evolution, not an immediate Beta rewrite. Product intent is in section 36 of `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`.

## 2026-10-12 — V21 Java mapping
- V21 passed local Maven tests (user-reported BUILD SUCCESS).
- Added AccountWorkspaceMembership entity in a5077e4 and repository in 5fd0e2f.
- These new classes are untested so far. Authorization remains legacy single-workspace; multi-workspace switch and registration are not yet implemented.

## 2026-10-12 — PostgreSQL integration test for one account in two families
- Added a transactional integration test to `FamilyPointsApplicationTests` (commit `35260cd`). It creates two independent Workspace records, per-workspace roles/members, one global account and two `AccountWorkspaceMembership` links, then queries by account/workspace in disposable PostgreSQL using Testcontainers.
- **Not yet verified locally:** new test compilation/execution and all-suite pass. Current authentication still selects the legacy primary workspace. This test does not yet prove cross-workspace HTTP authorization.
- Next: confirm Maven tests PASS; implement explicitly selected workspace context with scoped JWT authorization, and add negative tests before enabling signup/switching.

## 2026-10-12 — Account workspace choices API
- Added `GET /api/me/workspaces` read-only controller in commit `c7fa3e6`. It resolves authenticated account server-side, lists only its membership records, and marks the legacy active workspace.
- It excludes inactive members and does not expose other accounts' memberships. No client-provided account ID is accepted.
- This is **not** workspace switching: existing JWT/current workspace authorization remains unchanged. Signup and invitation creation are still unavailable.
- Latest changes not yet locally compiled/tested. Next: run `git pull` and `mvnw.cmd test`, then add secure server-controlled workspace selection and cross-tenant authorization tests.

## 2026-10-12 — Workspace switching security gate
- Audited `CustomUserDetailsService`, `CurrentUserService`, `WorkspaceSecurity`, `CurrentUserResponse` and JWT filters. Permissions and identity context remain linked to legacy primary WorkspaceMember. Switching workspaceId alone would be unsafe.
- Added `AccountWorkspacesControllerTest` in commit `398b68e` to exercise signed-in account workspace-choice responses and active marker using mocks. The test has not yet been run locally.
- Decision: defer live workspace switching until active membership, role evaluation, `/api/me`, JWT and all scoped authorization agree on the same selected workspace; integration-test unauthorized cross-workspace access. Do not expose switching before then.
