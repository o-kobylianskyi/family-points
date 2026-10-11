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
