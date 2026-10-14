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

## 2026-10-12 — Fail-closed membership context preparation
- Added `WorkspaceMembershipResolver` (`cc49a2e`) to resolve account + explicit workspace into its active member after verifying ownership, account enabled, active member, and exact workspace ID. Unauthorized/missing links are rejected with 403.
- Added unit tests (`3512f69`) for permitted membership and missing, inactive, disabled, forged or null workspace membership cases.
- These changes do NOT activate multi-workspace JWT switching, permissions or new registration routes; existing DEV authorization remains untouched.
- New commits have not been compiled or tested locally. Next: local `git pull` + `mvnw.cmd test`, followed by coordinated active-workspace/authorization implementation and two-family HTTP integration tests.

## 2026-10-12 — Signed workspace JWT foundation (not active)
- Added `JwtService.generateWorkspaceAccessToken` and `extractWorkspaceId` (`beb1288`), leaving existing token generation and authentication filter unchanged. Tokens include a signed `workspaceId` claim when explicitly requested; legacy tokens have no such claim.
- Added `JwtWorkspaceClaimTest` (`de303bb`) for signed claim, legacy compatibility and invalid ID guard.
- SECURITY: These helpers do not validate membership by themselves. Do not expose token issuance until the caller verifies active membership AND all request-level identity, permissions, `/api/me` and resource isolation are consistently bound to the selected workspace. Do not trust arbitrary request IDs/headers.
- Code changes have not been built or tested in the user's environment yet; `git pull` + Maven tests required.

## 2026-10-12 — Verified workspace membership context preview
- Audited legacy JWT filter, CurrentUserService and membership resolver. JWT workspace claims are not yet used for authorization; legacy account member remains active.
- Added read-only `GET /api/me/workspaces/{workspaceId}/context` (`00fd415`) returning role/permissions of the specifically verified account membership, never those of the legacy account member. Resolver rejects missing or inactive membership; the endpoint also rejects mismatched role workspace.
- Added two mocked controller tests (`4ac48c6`) for selected member role and foreign role rejection.
- No switching, JWT claim activation, or family registration was exposed. Tests have NOT been run locally. Next: `git pull` + `mvnw.cmd test`; audit all permission consumers before replacing active context.

## 2026-10-12 — Context-local permissions groundwork
- Added `WorkspaceContextAuthorization` (`1cfb539`) that resolves a verified account membership in an explicit workspace and then checks that the member's role belongs to the same workspace. Role permissions and ADMIN_OVERRIDE apply only within that verified context.
- Added 3 Mockito tests (`eff3526`) for selected-context role, foreign role denial, and absent membership denial.
- Deliberately NOT connected to JWT filter/WorkspaceSecurity yet. Existing request authorization and DEV login remain unchanged. New code is untested locally; run Maven suite before enabling workspace switching.
- Future mandatory work: integrate selected workspace with JWT authentication and all permission evaluators/DTOs consistently; add two-workspace HTTP integration tests, then enable safe switching and registration.

## 2026-10-12 — Centralized current member accessor (backward compatible)
- Added `CurrentUserService.getCurrentMember()` and routed its member/workspace ID helpers through this accessor (`5357df4`, `17ac0f1`).
- Switched `WorkspaceSecurity` permission checks to the accessor (`d476939`). Added `CurrentUserResponse.from(account, member)` while retaining legacy overload (`234d733`), and routed `/api/me` through the accessor (`c2098a5`).
- Effective member STILL resolves `UserAccount.workspaceMember` (legacy). JWT workspace claims remain inactive: no cross-workspace switching/signup enabled. This batch prepares one controlled integration point without changing DEV authorization behavior.
- These changes have not been compiled locally: run `git pull` and `mvnw.cmd test`. Next security prerequisite: unify all permission consumers and JWT context under verified selected member before any switching endpoint is exposed.

## 2026-10-12 — Deny workspace-scoped JWT in legacy authentication filter
- Identified a privilege-boundary risk: `JwtService` already supports signed `workspaceId` claims, but legacy `JwtAuthenticationFilter` would authenticate those tokens using permissions derived from `UserAccount.workspaceMember` (the primary workspace).
- Updated the legacy filter to authenticate only valid tokens **without** a workspace claim (`53a303d`). Signed scoped tokens are intentionally not authenticated until coordinated workspace context handling is implemented across all permission consumers.
- Added `JwtAuthenticationFilterWorkspaceScopeTest` (`5021743`) verifying backward-compatible legacy token acceptance and fail-closed scoped-token behavior; not run in user's environment yet.
- No user-facing workspace switching or registration. Next: `git pull`, Maven tests, then move JWT-selected member and authorization together, including HTTP cross-tenant tests.

## 2026-10-12 — Immutable per-workspace security session foundation
- Added `WorkspaceSessionService` (`14b0636`) constructing a permission snapshot from the *selected and verified* `WorkspaceMember` via `WorkspaceContextAuthorization`, never from the legacy primary workspace member.
- Added `WorkspaceSessionServiceTest` (`aa36e00`) for workspace-local permissions, denial without membership, and immutable permission snapshot.
- SECURITY STATUS: this class is NOT yet used by the JWT filter or regular authorization, and no scoped JWT may authenticate through the legacy filter. No user-facing workspace switch or family registration is enabled. Further audit of all `getWorkspaceMember()` and direct role permission callsites is required before activation.
- Latest Java commits not locally tested. Run `git pull` and `./mvnw.cmd test` (Testcontainers PostgreSQL required) and share any errors.

## 2026-10-12 — Fix missing membership for newly created accounts
- Found a continuity bug in V21 rollout: migration backfilled existing accounts, but `UserAccountService.createAccount` was still writing only `user_accounts.family_member_id` and not `account_workspace_memberships`.
- Updated account creation to save the account and its `AccountWorkspaceMembership` within the existing `@Transactional` service method (`e650779`). Newly created accounts can now be returned by `GET /api/me/workspaces`; database failure should roll back the transaction.
- Added mocked service tests (`6aefaef`) for membership creation and duplicate-account rejection. Neither test nor full Maven suite has been run in this tool environment.
- Existing account authorization, JWT scoped-token denial, and workspace switching remain unchanged. Do not enable scoped JWT until every permission consumer is audited and multi-workspace HTTP authorization is covered.

## 2026-10-12 — Existing account joins an additional workspace (internal primitive)
- Added `AccountMembershipLinkService` in `756be44` to write a membership for an existing global UserAccount and a workspace-local active member, rejecting disabled accounts, unavailable members, already-linked members, or duplicate account membership within that Workspace.
- Added four unit tests in `2f8dee0` for successful linking and negative cases. No public endpoint or invitation mechanism has been exposed; linking must only be called after future verified invitation/consent/authorization rules are defined. Database uniqueness enforces one account per member but **not yet** one membership per account per workspace under concurrency; a database-level constraint/locking strategy is still required before public invitations.
- Existing JWT scoped-token guard and DEV authorization remain unchanged. New code is not locally compiled or tested; run `git pull` and `mvnw.cmd test`. Full JWT context and end-to-end two-tenant authorization remain unfinished.

## 2026-10-12 — Membership uniqueness at PostgreSQL boundary (V22)
- Added Flyway V22 (`432661d`) introducing account_workspace_memberships.workspace_id, backfilling from family_members, FK, unique (account_id,workspace_id), and a trigger rejecting member/workspace mismatches.
- Updated AccountWorkspaceMembership entity (`5073478`) to persist workspace_id from the selected member, allowing continued account membership writes after V22.
- Existing V21 records remain intact. If real DB already contains duplicate account/workspace membership rows, V22 intentionally fails rather than silently dropping data; investigate and reconcile such records.
- IMPORTANT: Flyway V22 has NOT yet been run locally; run `git pull` + `mvnw.cmd test` against Testcontainers. No JWT switch, registration endpoint, or invitation exposure yet.

## 2026-10-12 — Local Maven run: V22 succeeds, stale entity SQL in failing integration test
- User supplied `mvnw.cmd test` log: 35 tests, 0 assertion failures, 1 error in `FamilyPointsApplicationTests.sameAccountCanBelongToTwoIndependentWorkspaces`.
- PostgreSQL Testcontainers 17.11 successfully migrated fresh schema V1..V22; Hibernate bootstrapped and all other test classes passed.
- Failing SQL was `insert into account_workspace_memberships (account_id,family_member_id) values (?,?)`, missing the V22 `workspace_id` column. PostgreSQL trigger correctly rejected the row (SQLSTATE 23514).
- Current GitHub branch `AccountWorkspaceMembership.java` already declares `@JoinColumn(name = "workspace_id", nullable = false)` and sets `workspace = workspaceMember.getWorkspace()` in its constructor (commit `5073478`). This mismatch strongly suggests local source/class files were older than current branch when the test ran (or local edits overrode mapping). No Flyway rewrite is justified from this log.
- Next validation: `git pull`, inspect local entity for `workspace_id`, rerun `mvnw.cmd clean test` (stop running backend/java process if Maven clean cannot remove target). If still failing, collect git status/diff and fresh SQL; only then change code.

## 2026-10-12 — V22 local verification recovered
- User confirmed `BUILD SUCCESS` after pulling current changes and rerunning the Maven test suite following the V22 workspace_id insert failure.
- Previous failing test `sameAccountCanBelongToTwoIndependentWorkspaces` is no longer blocking the suite. This is consistent with the local entity mapping catching up to committed `AccountWorkspaceMembership.workspace` changes.
- Confirmation covers the Maven test suite and Testcontainers Flyway V1–V22 path; it does **not** establish completed HTTP cross-workspace authorization or production readiness.
- Next: implement coordinated selected-member JWT/CurrentUserService/permission evaluation with explicit two-tenant HTTP denial tests before activating workspace switching or registration.

## 2026-10-12 — Scoped effective-member resolution behind trusted authentication details
- Last user confirmation: Maven `BUILD SUCCESS` including Flyway V1..V22 after synchronizing local source.
- Created `WorkspaceAuthenticationDetails` (`940436e`) holding immutable accountId/workspaceId/memberId, documented as server-only verified authentication metadata.
- Updated `CurrentUserService.getCurrentMember()` (`e40cc72`): when trusted scoped authentication details are present, check account identity, resolve current active DB membership for workspaceId and check exact memberId; otherwise preserve legacy primary-member behavior. `getCurrentMemberId()`, `getCurrentWorkspaceId()`, `WorkspaceSecurity`, and `/api/me` already use this accessor.
- Added unit tests (`c421807`) for valid scoped member, mismatched account/member rejection, and backward-compatible legacy context.
- **Crucial:** JwtAuthenticationFilter still rejects workspace-scoped tokens and does not construct `WorkspaceAuthenticationDetails`. No public switching endpoint, JWT activation, or registration yet. Remaining direct primary-member lookups and tenant-facing controllers still require audit and HTTP authorization integration tests.
- Latest package NOT run locally; run `git pull` and `mvnw.cmd test` before proceeding.

## 2026-10-12 — Scoped JWT tests for live workspace selection
- Added two tests to JwtAuthenticationFilterWorkspaceScopeTest (commit d91c46e) verifying that a scoped JWT obtains only the selected workspace's privileges rather than the primary account's ADMIN_OVERRIDE, and an inaccessible workspace is rejected without authentication.
- Existing legacy token tests are retained; scoped token test with unconfigured legacy-only filter still expects denial.
- Pending local Maven run; HTTP controller authorization across two workspaces, direct account-member usage audit, and user-facing workspace switching remain incomplete. Latest verified BUILD SUCCESS predates commit 420c1d5 and these tests.

## 2026-10-12 — Workspace route isolation handles revoked memberships
- Updated WorkspaceIsolationFilter (`5075718`) to return HTTP 403 when CurrentUserService throws ResponseStatusException for revoked/invalid scoped memberships, rather than allowing an uncaught exception during filtering.
- Added two filter-level tests (`2cb0e84`) proving a revoked selected workspace and a scoped session addressing another workspace both receive 403 before the request reaches its controller.
- These are MockFilterChain tests, NOT end-to-end MockMvc HTTP/security integration tests with two real database workspaces. No switching or public registration activated.
- Compilation and tests pending local run; use `git pull` and `mvnw.cmd test`.

## 2026-10-12 — MockMvc + PostgreSQL cross-workspace HTTP isolation test
- Added integration test `scopedJwtCannotReadAnotherFamilysHttpWorkspaceRoute` to `FamilyPointsApplicationTests` (`8dda3f3`), using the existing Testcontainers PostgreSQL database and Spring Security MockMvc filter chain.
- Test builds three workspaces, assigns the same account to A and B, issues signed JWT scoped to B, expects GET B = 200, GET A = 403, GET unrelated workspace = 403, and checks `/api/me` reports B's member/workspace ID.
- Added `@AutoConfigureMockMvc` / MockMvc injection; current JWT issuance to public clients and workspace switching remain disabled. This newly added test has NOT been executed locally; user must run `git pull` and `mvnw.cmd test` and share output.
- Follow-ups: test revoked memberships/disabled account at HTTP level, permission-sensitive routes, cross-tenant nested resource IDs, refresh preserving selected workspace, and audit remaining legacy `getWorkspaceMember()` usages before public Beta signup.

## 2026-10-12 — HTTP revocation checks added
- Extended the PostgreSQL Testcontainers + MockMvc workspace isolation test (`7a4bc6b`) to verify that an already-issued scoped JWT is rejected with 401 after its selected workspace member is soft-deleted, and that a legacy JWT is rejected with 401 after the global UserAccount is disabled.
- Existing two-workspace positive/negative route and `/api/me` checks remain in the same test. These are expectations, NOT a verified passing local run; newest suite has not been executed by the user yet.
- Workspace switching and public account/family signup remain disabled. Run `git pull` and `mvnw.cmd test`; if errors arise, review surefire output before editing code.

## 2026-10-12 — GitHub Actions CI and batched development
- Added `.github/workflows/ci.yml` (commit `6456331`): triggers on pushes to feature/task-soft-delete and main, pull requests, and manual workflow dispatch.
- CI: ubuntu-latest, Temurin Java 21, Maven dependency cache, Docker verification, `./mvnw -B -ntp clean verify`, Surefire/Failsafe artifacts retained 14 days, read-only GitHub contents token.
- Development workflow: commit cohesive feature batches with tests, use Actions result for each commit before marking PASS. Log commit SHA, tests, CI state (PENDING/PASS/FAIL), follow-up work; retain local Windows/Android device smoke testing as separate checks.
- Status as of writing: workflow file committed, **CI run not yet verified**; GitHub combined commit statuses endpoint returned no status entries. Do not claim success until Actions run is inspected.

## 2026-10-12 — First GitHub Actions CI success confirmed
- User supplied screenshot of FamilyPoints CI run #3 for commit `9cb5e4e` on `feature/task-soft-delete`: green success, duration 1m 5s.
- Earlier runs #1 and #2 failed after ~13–14 seconds; adding `chmod +x ./mvnw` in workflow commit `9cb5e4e` was followed by successful third run. This supports operational CI verification for the committed backend test suite.
- Next development workflow: coherent multi-file feature batches, CI status checked by exact commit. Keep Android/device/UI exploratory testing separate.

## 2026-10-12 — Remember last successful username in Web and Android
- Android: added LastUsernameStore with separate SharedPreferences so logout/token clearing does not erase username (b614f9d); LoginScreen no longer defaults to `oleh`, loads `initialUsername` and saves username only after successful online/offline login (2d85030); MainActivity wired persistence (690b250).
- Web: LoginPage loads username from localStorage and updates only after successful login, with no password storage (ba0b2f0).
- Desired parity rule: authentication form defaults to latest successfully used login per device/browser; login failures must not overwrite it. Android is not yet covered by backend-only CI; compile/test with Gradle or extend workflow. Status: code committed, Android build not verified.

## 2026-10-12 — Android CI added alongside backend CI
- Workflow `.github/workflows/ci.yml` updated in commit `588797f`: second independent job `android-debug` runs on Ubuntu with Java 21, Android SDK platform 37, Gradle wrapper 9.6.0, `:app:assembleDebug` and `:app:testDebugUnitTest`. Android test reports are uploaded as artifacts.
- Backend Maven/Testcontainers job remains unchanged. Both jobs run in parallel; do not mark Android PASS until actual Actions result is available.
- Android LoginScreen and Web LoginPage already remember the most recently successful username per device/browser (Android `b614f9d`, `2d85030`, `690b250`; Web `ba0b2f0`). Android compilation now becomes part of every push CI run.

## 2026-10-12 — Web workspace switching + Android API foundation
- Web `authApi.js` adds membership listing and activation (`e9012a0`). `AuthContext.jsx` validates `/api/me` for the new token before committing it and performs a full reload to clear React component state after switching (`84efa1c`). `AppLayout.jsx` shows the family chooser only if >1 linked workspaces (`ece9fcd`).
- Android `AuthApi.kt` declares list/activation Retrofit contract and `AccountWorkspaceChoice` (`b31dced`), but the Android UI must NOT expose switching until all tenant-sensitive offline caches are namespaced or cleared safely; retain authenticated-offline semantics.
- CI status PENDING / not verified for this batch. Need front-end build CI, Android API compile, tenant cache isolation review, and end-to-end switching tests. Backend + Android jobs trigger on push and run independently; no need to wait for CI to continue unrelated work.

## 2026-10-12 — Three-platform CI and Android workspace switching
- CI commit `362039f`: added parallel Node 22 / npm ci / Vite production build for Web, alongside Maven/Testcontainers and Android Gradle checks.
- Offline Android store audit: dashboard/tasks/members/point-types/rewards/points and queues already namespace records using workspaceId and member/viewer IDs. OfflineAccountStore keeps one encrypted profile per username: switching replaces the cached profile with the latest verified workspace; offline selection is not allowed.
- Android AuthSessionManager commit `d99700f`: list workspaces and activate strictly online; validate `/api/me` new workspace before replacing token, refresh enrolled offline profile and activity timestamp.
- Android WorkspaceSwitcher component `b071891`: explicit chooser, busy/error states. MainActivity wiring `8b9be79`: show only in online dashboard, clear task navigation state and update current user when switching.
- CI checks for newest commits are PENDING/not confirmed at writing. Must validate Kotlin compile, Node build, actual Android device display, and cross-workspace cache behavior before declaring beta ready.

## 2026-10-12 — Reward redemption UX: balance and duplicate-click protection
- Web RewardsPage commit `0570932`: show currently authenticated member's per-currency balances via existing `/members/{memberId}/points/balances`, refreshed after mutations; add explicit success notice after reward purchase; add synchronous React ref lock to prevent repeat button presses while requests are in flight.
- **Scope:** this is UI/in-flight duplicate prevention, NOT server-enforced once-only redemption. Browser reloads, parallel devices and concurrent requests may still purchase twice. A future server-side configurable cooldown and idempotency key/transaction guard are required before claiming duplicate rewards are impossible. Cooldown must be per member/workspace/reward, configurable; protect direct purchase and request purchase against concurrency; include PostgreSQL integration tests.
- Web CI for commit pending. Android rewards UX parity not yet implemented.

## 2026-10-12 — V23 reward purchase cooldown + serialized redemption
- Migration V23 (`37a399b`): reward_definitions.cooldown_seconds NOT NULL DEFAULT 30, nonnegative check and reward purchase lookup index.
- JPA/API (`05e35cd`, `d0c0c61`, `5b92c2d`): persistent configurable cooldown and response field; defaults to 30 seconds on new catalog rewards; zero disables cooldown.
- RewardPurchaseRepository (`3387629`) exposes most recent member/reward purchase. RewardService (`387a149`) locks member row before direct and request purchases, enforces cooldown before debit, returns HTTP 409 for retry during the interval, including other device requests. Requested and direct purchases share the definition cooldown; cancelled prior purchases excluded.
- Web RewardsPage (`bd20f15`) supports cooldown input and displays it in catalog; retains in-flight duplicate click protection and balance/success notice from prior commit.
- Unit test (`f5ee4c9`) asserts repeat purchase responds 409 before point spending.
- CI STATUS: PENDING for latest commit; GitHub Actions concurrency cancels superseded intermediate pushes, so judge final batch SHA rather than old cancelled runs. Further work: formal idempotency keys for network retries, concurrent HTTP PostgreSQL test, Android UI parity, cooldown remaining time UX.

## 2026-10-12 — Fix approved reward not redeemable by managing member
- Report: approved Minecraft request had status READY_TO_PURCHASE but Catalog still showed only «Попросити»; admin owner saw no redemption action in Requests.
- Web RewardsPage (`c50a5b5`): show «Отримати» for the current member's READY_TO_PURCHASE catalog request; display pending request state rather than duplicate request button; Requests redemption and readiness refresh are now based on request ownership, not absence of MANAGE_REWARDS permission; show success notice and refresh balance after redemption.
- Backend RewardController (`0739224`): canManageRewards checks active CurrentUserService.getCurrentMember() rather than the account's legacy primary member to prevent cross-workspace role leakage.
- CI status pending; need validation on logged-in owner and request not belonging to owner, including manager/parent account.

## 2026-10-12 — Reputation displayed in reward balances; CI cooldown test correction
- Added authenticated `GET /api/me/reputation` in MyReputationController (`42cfd91`) returning reputation balance for the active workspace member; no cross-member query parameter and no use of legacy primary member.
- Web RewardsPage now loads reputation alongside current currency balances (`d99fd97`) via `reputationApi.js` (`da80924`). Reputation remains distinct and not spendable.
- Added MyReputationControllerTest (`365c699`) for selected member/workspace and balance.
- Investigated GitHub Actions failure for previous commit `461d24f`: backend Maven RewardPurchaseCooldownTest failed because Mockito expected `findByIdAndWorkspaceIdForUpdate(8L,3L)` but implementation correctly invokes memberId first `(3L,8L)`. Fixed test setup in `1fa37b5`. Prior Android SDK setup CI also failed and remains under follow-up. The latest CI run has not been confirmed PASS.
- Future UX parity: show reputation on dashboard/Points and Android screens. Note that rewards/catalog minimum reputation threshold already exists.

## 2026-10-12 — Reputation dashboard parity and Android CI sdkmanager fix
- Web DashboardPage `1da5953`: loads current workspace member reputation through /api/me/reputation and displays separate non-spendable metric beside points.
- Android AuthApi `6bc69e8`, DashboardScreen `ad22cdf`, MainActivity `eee7dcc`: load active member reputation only online; verify memberId and workspaceId before showing; do not show stale reputation while offline or after switching workspace.
- Investigated Actions run 38293200489: backend and Web passed; Android failed during setup-android with `Failed to find package 'platform-tools\\n platforms...'`; sdkmanager packages were interpreted as one multiline value. Workflow `6638c0b` now supplies a space-separated package list. Latest Android CI not yet confirmed.
- Pending Android Gradle compilation and on-device UI smoke test; track latest run by SHA. Reputation Points-page and member-profile history still pending.

## 2026-10-12 — Android deprecated API warnings cleanup
- AppUsageReader (`fa431e9`): replaced deprecated `unsafeCheckOpNoThrow` with `checkOpNoThrow` (supported across app minSdk 26), preserving usage access guard.
- MainActivity (`932fb61`): white system bar colors now explicitly set only on Android SDK <35 via a scoped legacy helper; Android 15+ relies on edge-to-edge system surfaces. Light system bar icons still configured using WindowCompat.
- CI Android compilation not verified at time of commit; user should pull and run `gradlew.bat :app:assembleDebug`; real-device visual validation recommended due to status/navigation bar behavior changes.

## 2026-10-12 — Reputation history and non-obstructing Android workspace selector
- Backend `MyReputationController` now exposes `GET /api/me/reputation/history` for authenticated active member; returns recent 100 transactions (id, amount, sourceType, description, createdAt), commit `fbdd57d`. Controller unit test updated (`9fcf2b8`), new history test (`72aee03`, enum correction `ed38d9f`).
- Web API history (`ca969ff`) and Dashboard latest five entries (`af5585c`); dates, amounts and descriptions shown.
- Android Retrofit history DTO/API (`98385ae`). Android picker moved from floating bottom overlay to dashboard top scrollable section (`ddeb322`, `3869889`) and hidden on offline-authenticated sessions (`222832c`, `fe70239`).
- Prior CI `5011840`: Backend PASS, Web PASS, Android FAIL during SDK setup: `Failed to find package 'platforms;android-37'` on hosted sdkmanager. Local user explicitly confirmed Gradle `BUILD SUCCESS` on Windows, but CI remains unresolved for Android SDK 37 availability. Next: inspect available preview channel/runner and fix reliable SDK 37 installation or use compatible published SDK version after verifying plugin and source support.
- New package pending CI. Android reputation history UI and history pagination are follow-ups.

## 2026-10-12 — Reward cooldown causes Web/Android logout: fix error response
- User reports second reward purchase redirects to login on both Web and Android. Root cause hypothesis grounded in source: RewardService throws ResponseStatusException(409) but GlobalExceptionHandler lacked specific handler; servlet error dispatch to protected `/error` could turn business conflict into unauthenticated 401.
- GlobalExceptionHandler now returns explicit structured JSON with original HTTP status for ResponseStatusException (`862473b`), avoiding fallback error-dispatch ambiguity. Unit test confirms cooldown stays 409 (`82eade8`).
- Android RewardsScreen shows localized cooldown notice for 409 without calling onUnauthorized (`3e8663e`). Web rewardApi gives a friendly cooldown fallback for 409 (`a8cd7a5`).
- CI status unverified for this package; confirm by repeating same reward while JWT active and inspecting browser Network response. If 401 persists, inspect the exact failed endpoint and server logs rather than assuming it is the cooldown response.

## 2026-10-12 — Independent redemption controls per reward
- User reported that buying one reward appeared to activate a global 30-second block for all others. Source audit: backend RewardService cooldown lookup was already scoped to `(memberId, rewardDefinitionId)`; Web RewardsPage incorrectly reused a single global `processing` flag and synchronous `actionLock` for all reward purchases.
- Web commit `4021de6`: per-reward in-flight locks (`purchaseOnce`, `purchaseLocks`, `pendingPurchases`), shared key for catalog and matching approved request; distinct rewards can be purchased while another purchase is pending. Other form mutations still use the original global run lock.
- Changed catalog label from misleading 'Повторно через 30 с' to 'Пауза для цієї нагороди: 30 с', indicating configured cooldown, not a countdown. Server continues to enforce real cooldown individually per member and reward definition.
- CI status pending. Follow-up: server idempotency key for retries, proper remaining-time display based on timestamp, and Android UI per-reward in-flight parity. If distinct reward API returns HTTP 409, inspect precise network response rather than assuming UI-only failure.

## 2026-10-12 — Edit existing catalog rewards (Web + Backend)
- Implemented `PUT /workspaces/{workspaceId}/rewards/{rewardId}` requiring MANAGE_REWARDS in selected workspace (`eea20ed`), handled by transactional RewardService.updateReward (`620b1f4`). Validates reward belongs to workspace and replaces definition fields, categories and definition requirements without recreating existing purchase history.
- Web rewardApi PUT (`73f3ebe`), RewardsPage editing action (`ff03bfe`): managers can choose Edit from catalog; shared create/edit form preloads title, description, point type, price, cooldown, reputation threshold, acquisition/approval, duration, categories and task-based before/after conditions; Save updates same ID, Cancel resets form.
- Authorization regression tests added (`acd2264`). CI not yet confirmed. Caveat: existing non-task advanced requirement types are not represented in Web task picker and must be preserved/handled before calling this a complete advanced-condition editor. Pending Android edit UI parity.

## 2026-10-12 — Verified reward-edit CI; Android SDK preview-channel fix
- GitHub Actions run 38297807371 for commit c293dd0: Backend Maven/Testcontainers PASS, Web Vite PASS, Android FAIL before compilation at setup-android: stable sdkmanager could not find `platforms;android-37`.
- Updated Android CI (`0e9a9cd`) to provision standard platform-tools/build-tools first and install API 37 explicitly via `sdkmanager --channel=3` (preview/canary channel). This is an attempted CI-environment correction; pending new CI result.
- Automated green builds do not yet cover full reward-edit user flows. Advanced non-task requirements may be erased on editing using current Web form and need preservation/coverage before production use.

## 2026-10-12 — Reward redemption count limits (staged delivery)
- Flyway V24 `59a9838`: six nullable positive count limits (member / family × lifetime / calendar day / calendar ISO-style Monday-start week), and `extra_request_allowed` flag. NULL means unlimited; existing rewards unaffected.
- RewardDefinition JPA mapping `529e58e`, RewardDefinitionRequest `4fefc8e`, RewardDefinitionResponse `6a81568`; create/update service now saves limit configuration.
- RewardPurchaseRepository `5a5fc52` count query; RewardDefinitionRepository `24e321d` pessimistic reward row lock. RewardService `3c96308` checks configured member and shared-family count limits before spending points on both direct and approved-request purchases. Count excludes cancelled purchases and uses purchases' original timestamps. Weekly boundary is Monday local server date.
- Web RewardsPage `485305b` exposes six optional count limits and `extraRequestAllowed` in create/edit form; copy clarified in `04944c3` that approval/one-time exception issuance is NOT yet implemented.
- Tests `5c315a2` check personal lifetime and shared-family lifetime blocking before debit. Updated existing cooldown test `5e2fe26` for locked repository lookup.
- **Status:** PENDING CI. Not yet implemented: total-minute limits, daily/weekly minute budgets for time rewards, dedicated additional-permission approval/one-time consumption flow, Android edit and remaining quotas UI, member/workspace timezone selection and real cross-account concurrency integration tests. Do not claim those pieces are operational.

## 2026-10-12 — Read-only virtual schedule for recurring tasks (first phase)
- Diagnosis: member task list GET returned only persisted TaskInstance, while existing TaskGenerationService was called at creation for LocalDate.now(), so a DAILY definition (#39) had no instance the next day. Future dates likewise were empty.
- New DTO TaskScheduleOccurrenceResponse (`5bf9392`) models stable `definitionId:date:default` and distinguishes virtual PLANNED (null instanceId) from persisted TaskInstance.
- TaskService.getMemberSchedule (`443e1b6`) combines active assigned definitions valid for requested date with existing instances (existing status wins), checks target member access and per-definition read authorization, and does not insert rows. New GET `/workspaces/{workspaceId}/tasks/members/{memberId}/schedule?date=YYYY-MM-DD` (`258e6b0`).
- Web taskApi (`90ca939`) and TasksPage (`3edb7c3`) show read-only planned cards for requested current/future days beside actual task instances; existing status/action flow unchanged. Regression tests in `123bacf`.
- Important limitation: planned tasks are currently read-only placeholders; their start/complete actions are not materialized yet. Existing DB unique constraint (definition/member/date) still prevents multiple slots per day. Follow-up: TaskSchedule/slot entities, occurrence-key DB migration, atomic materialization and deadline processing, cross-platform Android parity and schedule exception rules. Do not claim multi-daily occurrence support yet.
- CI pending at write time.

## 2026-10-12 — Start virtual task instances and repair automatic missed-task penalties
- User reported Clean room absent from manager list, five scheduled daily tasks visible for Denys but only View action, and automatic penalties not working. Clean room existence in actual DB could not be confirmed; it may be a reward condition rather than a separate TaskDefinition. Do not fabricate.
- Locked TaskDefinition repository `6f67ea8`; TaskService.startScheduledOccurrence `f68d896` is transactional: only assigned current member, eligible date not future, one instance per definition/date, starts pending instance and writes system audit. API `47375d5` and Web API `7ec71aa`, TasksPage `5aaf28f`: 'Почати' for own planned occurrences.
- Automatic penalty root cause: previous deadline service only considered persisted TaskInstance and was only callable through MANAGE_TASKS endpoint, while virtual recurring occurrences had no stored row. Added system-only TaskService.missBySystem `07dca3f` with DB lock, exception periods, system audit and existing one-time penalty guard. DeadlineService `3482c9d` creates only overdue mandatory occurrences with dueTime, then processes missed candidates; @Scheduled enabled `7706e78`, default poll every 5 minutes. Safety revision `ff68f54`: catch up only yesterday/today to avoid retrospective mass penalties; no automatic penalty for tasks without dueTime or not mandatory. Unit test `c14274b` checks no future materialization.
- CI newest commits are pending, no pass claim. Still TODO: robust calendar/occurrence-slot support for multiple times per day, Android start action, proper cross-device concurrency integration tests and service-level deadline integration tests. Deadline processing uses application/server timezone, not yet workspace timezone. Verify scheduled job's effects on an isolated test workspace before deploying to public BETA.

## 2026-10-12 — Task details management: edit definition and confirm dated occurrence
- User noticed TaskDetailsPage management section offered only Activate/Deactivate even for their authored/administered task.
- TaskService.completeScheduledByManager (`972ab76`) adds transactional manager-authorized completion of one selected dated occurrence: locks TaskDefinition, validates schedule/active/assigned member/past-or-today date, reuses or creates TaskInstance, processes reward once and records who confirmed in the audit. Already completed is idempotent; unsupported terminal states are rejected. Controller endpoint `c2fedc5`.
- Web API `69f677f`, TaskDetailsPage `6fb952c`: 'Редагувати' opens the existing CreateTaskModal initialized with definition + participants; 'Позначити виконаним' confirms a particular date, requires confirmation and reloads instance. Local date comparison hardened `b865b6a`.
- CI pending; needs scenario tests of manager vs unrelated member, duplicate completion and rewards, scheduled-date validity, editing all participant combinations. UI currently displays management actions independent of scoped role while backend rejects unauthorized calls; follow up gate UI with server-provided canManage metadata.

## 2026-10-12 — CI triage after scheduled-task management package
- Run 38303219123 on commit 4f457f4: Web PASS. Backend compiled and ran 57 tests; 56 passed, one failed at RewardRedemptionLimitTest.workspaceLimitChecksAllMembersNotJustBuyer. In Mockito, unstubbed boxed Integer getMemberTotalLimit() returned 0, not production's null for unlimited; service consequently hit personal quota first. Corrected test's personal limit null stubs in `9e0d430`; follow-up CI not yet confirmed.
- Android failed before compilation: sdkmanager --channel=3 still cannot locate platforms;android-37 on hosted runner. Local Android Gradle build previously reported BUILD SUCCESS; do not confuse environment setup failure with Kotlin failure.
- Previous TaskService.findForOccurrenceUpdate missing declaration was resolved in `4f457f4`: compilation proceeds to tests.

## 2026-10-12 — Scheduled task manager completion regression checks
- Actions run 38303462235 (f0f9966): Backend Java/Maven/Testcontainers PASS; Web Vite PASS; Android still FAILS installing Android API 37 on hosted runner before Kotlin compilation.
- Added TaskManagerCompletionTest (`65277d9`): rejects manager completion of future occurrences before creating TaskInstance; checks that confirming an already COMPLETED dated instance neither creates a duplicate nor processes points/reputation a second time. Uses mocked authorization, generation and repositories.
- Tests newly pushed; CI pending. Follow-up needs PostgreSQL integration test to exercise transaction locks, concurrency and exactly-once ledger semantics (these Mockito tests do not prove concurrency).

## 2026-10-12 — TaskScheduleSlot first persistence layer (V25)
- Introduced Flyway `V25__task_schedule_slots.sql` (`9093a7c`): reusable slots keyed by (task_definition_id, slot_key) with nullable scheduled time and enabled flag; no historical TaskInstance changes. Existing tasks retain implicit default occurrence and are not duplicated by migration.
- TaskScheduleSlot entity (`2bf0097`) and repository (`1e01c2e`) map slot definitions.
- TaskScheduleSlotController (`5b5a94e`): GET list requires task read authority; POST create requires task manage authority, scoped workspace task lookup, validated stable slot key. These APIs create schedule templates, NOT dated instances or rewards.
- Controller unit tests (`00c21ad`) confirm read without writes and denied creation without manager permission.
- CI pending. **Staged, not user-visible multi-daily execution:** task_instances still unique by definition+member+date and start-scheduled still addresses one implicit default instance. Next steps: V26 with slot_key on task_instances and revised unique key; slot-aware locking, deadlines and per-instance rewards; projection/web UI + Android parity. Do not offer multi-slot completion before these correctness checks pass.

## 2026-10-12 — V26 per-slot occurrence identity, first execution pipeline
- Fixed previous Mockito test compile error by declaring checked AutoCloseable exception in TaskManagerCompletionTest (`4d83913`); previous CI failed compiling tests, Web pass, Android sdkmanager API37 unavailable.
- Flyway V26 (`ef8df88`): add task_instances.slot_key NOT NULL DEFAULT 'default', preserve historical rows and replace old unique constraint with (task_definition_id, member_id, scheduled_date, slot_key). Validate migration on CI/PostgreSQL before rollout.
- TaskInstance slot key + snapshot due time (`cdd74bf`), repository slot query (`5916c44`), slot repository key query (`505da43`). Virtual schedule DTO includes slot identity (`404b0d0`).
- TaskService (`f1eb5b2`) now projects each explicitly defined schedule slot rather than one default per day (unless no slots); starts concrete occurrence by slot under definition lock; legacy default action retained. REST (`b94b1af`), Web API (`d9d8843`) and TasksPage (`3045260`) use slot key for start. No speculative future TaskInstance rows are created.
- Important still-incomplete: per-slot admin completion, independent deadline/missed handling, details page slot navigation, editing slots in Web/Android. Existing legacy default-only workflows may not cover non-default slots yet; do NOT claim full multi-slot lifecycle. CI pending.

## 2026-10-12 — V26 slot-specific admin completion, deadlines and details
- CI for e6a753a: Web PASS, Backend FAIL from V26 migration PostgreSQL 42883 comparing name[] to text[]; TestManagerCompletionTest mock still old repository method. Fixed with text cast in V26 (`c7874ab`) and updated mock (`ee7def3`).
- TaskService manager completion accepts slotKey with legacy default overload (`26b6b05`); manager confirm API accepts slotKey (`e45e018`). Instance due time and candidate repository now use occurrence snapshot instead of definition-level due time (`17f6ceb`).
- DeadlineService now examines each enabled slot with its own deadline, skips unbounded slots, and calls locked system materialization (`60d607d`, `061ec0a`); unit test constructor adjusted (`7720502`). Catch-up remains yesterday+today only. Penalty/idempotency relies on existing missBySystem guards.
- Slot-aware detail lookup implemented in Backend (`eda6b10`, `e60b7d5`), Web API (`0726d2c`), detail route/actions (`b34cc27`) and planned-card navigation (`94495ac`). This allows manager to mark morning or evening occurrence separately rather than accidentally completing the default slot.
- CI pending. Follow-up: test with real Postgres concurrent requests, add management UI for configuring slots, include Android, check timezone and paused status semantics before beta.

## 2026-10-12 — Package 1 stabilization: V26 compilation, deadline guard, CI green for Backend/Web
- Started with failed GitHub Actions run 38311324290: Web PASS, Maven compiler FAIL because LocalDateTime was unqualified in TaskService.materializeOverdueSlot. Fixed `88c00b0`.
- Mandatory tasks without an explicit dueTime now expire at the next calendar-day boundary instead of never being penalized (`3a57771`). Query now considers null dueTime but strictly filters mandatory snapshot (`3a913eb`, `15efebd`). This preserves the one-day bounded catch-up; no historical mass penalties.
- Added deadlineFor null-time and explicit-time assertions, plus generator recurrence mock in TaskDeadlineRecurringTest (`e673220`).
- CI run 38312518524: Maven PostgreSQL Testcontainers **BUILD SUCCESS, 62 tests / 0 failures / 0 errors**, Web Vite **SUCCESS**; Android SDK37 package unavailable on runner (existing infrastructure limitation).
- Outstanding beta gates: concurrent transactional ledger test, two-slot deadline/penalty integration test, slot-management UI, timezone normalization, and old default-generation interaction with explicit slot schedules. Do not claim these are tested or finished; package 2 editor should handle slot reconciliation and update semantics safely.

## 2026-10-12 — Package 2 Web TaskSchedule editor (substantial implementation)
- TaskScheduleSlot gains editable scheduledTime and enabled state (3799f26), repository supports listing all slots including disabled, slot-scoped lookup and key lookup (511a772).
- Secured controller PATCH /{slotId}/enabled and PUT /{slotId} (ea9ee5f), requiring manage rights and validating definition/slot association; reserves implicit key 'default' at server boundary (f6734de). No physical DELETE: preserve historical occurrence identity.
- When explicit slots exist but all are disabled, neither future schedule projection nor deadline generator restores implicit default (48017a6, 6bbeb09). Existing dated TaskInstances remain in history.
- Web API CRUD-like edit + activation (ab557e7), independent schedule editor component (f17b2b7), embedded in TaskDetailsPage (bcf57f7). Supports add, edit time, disable/enable, inline errors, loading states, responsive wrap; no manual API requests.
- Controller tests expand to verify management authorization before writes and listing disabled slots (d7b228e). CI pending. Important remaining risk: time edits may affect future projections while existing PENDING instances retain their original dueTime snapshot; intended historical stability, requires product decision for already materialized future instances.

### Package 2 final CI verification
- Fixed TaskGenerationService constructor usages in TaskVirtualScheduleTest (40a12ce) after CI test compilation failure.
- GitHub Actions run 38313245292 on 40a12ce: Java 21 / Maven / PostgreSQL Testcontainers SUCCESS; Web / Vite production build SUCCESS. Android SDK37 installation failed on hosted runner (known infrastructure issue; local Android testing agreed).
- Backend and Web schedule editor code now builds; full interactive browser/real-user acceptance testing is still pending.

## 2026-10-13 — Hide unauthorized schedule controls and member browsing
- Child account screenshots showed editable schedule controls then 403. Added per-task GET /definitions/{id}/permissions (a4bc094, 91248fc); TaskDetailsPage now calls server-authoritative canManage and only mounts management controls/TaskScheduleEditor for authorized managers (923ed85, 835be83).
- Child saw member tabs for Oleh, Denys, Maryna. TasksPage now gates member switching by VIEW_ALL_TASKS/ADMIN_OVERRIDE and task creation by CREATE_TASKS/ADMIN_OVERRIDE (d190b39). Backend getMemberTasks now independently denies viewing another member's persisted executions absent those privileges (d9951c5), matching existing getMemberSchedule protection.
- New GitHub Actions run 38314823205 pending at time of write. Need test child login after git pull and both front/backend restart, verify own tasks only, management buttons absent, direct foreign member endpoint returns 403. Ensure authorized parent can still manage.

## 2026-10-13 — CreateTaskModal multi-slot visibility fix
- User showed a one-time task creation screenshot without schedule-slot controls. Existing multi-slot controls were accidentally conditional on recurrenceType != ONCE, so the default ONCE path hid the feature.
- Removed that gate (6183c62): the option “Кілька виконань за вибрану дату або протягом дня” now appears for new ONCE and recurring tasks. Initial times 09:00 and 15:00 rather than blank. UI distinguishes one selected date from recurring active days.
- Creation already loops through createTaskScheduleSlot for each time after creating the definition; errors surfaced in modal. **Known remaining transactional gap:** task definition creation and schedule slot creation are multiple HTTP requests, not a single transaction. Legacy creation may create initial default instance; verify one-time multi-slot generated default/historical instance interaction before production release.
- CI run 38315539475 pending at time of documentation.

## 2026-10-13 — Atomic task + initial multi-slot creation
- GitHub Actions run 38315606945 (prior UI iteration): Backend/Web PASS, Android SDK37 runner setup FAIL.
- Created initialSlotTimes request field with max 12 non-null LocalTime entries (3af03a7); TaskService.createDefinition now saves slot rows under existing @Transactional service method after persisting definition and before eager execution generation (2595c72), validating unique times and skipping eager default materialization when explicit slots supplied. Any exception rolls back task, participants, work node and slots together.
- TaskController forwards initialSlotTimes (4295ab8); CreateTaskModal submits slots in same definition POST instead of separate follow-up POST requests (d4bf9b9).
- GitHub Actions run 38369284773 at d4bf9b9: Backend Maven/PostgreSQL Testcontainers PASS; Web Vite PASS; Android SDK37 installation still failing on hosted runner.
- Remaining: user acceptance smoke test for one-time two-slot and daily three-slot tasks; concurrency/ledger integration tests, slot editing with existing snapshots.

## 2026-10-13 — Management permissions and legacy default-query safeguards
- Verified GitHub Actions 38369437726: Backend Maven/PostgreSQL PASS, Web Vite PASS; Android hosted runner still fails before Kotlin compilation due to SDK37.
- TaskService.getOpenDefinitions now filters by TaskAuthorizationService.canRead before returning entries, preventing tasks outside caller read scope from being displayed; historical old date-only instance lookup changed to explicit 'default' slot key to avoid non-unique result with several daily occurrences (45e372c).
- Added regression test in TaskManagementViewTest ensuring the created-by-me tab excludes a different author's task (cb82cb3). Existing regression test covers admin view using effective canManage instead of explicit ADMIN participation.
- Remaining: comprehensive per-role HTTP access tests including child/group membership, history endpoint visibility and actual user environment reproduction for 'Clean room'; CI for current changes pending.

## 2026-10-13 — Task management/audit package extension
- Tightened TaskService.getOpenDefinitions: read permission filter before exposing eligible open definitions; default-key instance lookup used instead of non-unique definition/date lookup (45e372c).
- Added created-by-me regression test (cb82cb3). Existing management test covers effective manager listing.
- TaskScheduleSlotController now appends DEFINITION_UPDATED user audit events on slot creation, rescheduling and enabled toggles, with old/new values where appropriate (77ab123). Uses same task manage authorization and transaction. Controller test constructor updated (b48ec5b).
- API/Web permission protections from earlier package remain in place. CI pending for this package; actual child/parent manual test and Clean room data root cause not yet confirmed.

### CI follow-up for management/audit batch
- Initial run 38370229897 failed compiling TaskScheduleSlotControllerTest: new TaskAuditService dependency absent from several test constructors/import. Repaired in f1176d5.
- GitHub Actions run 38370322845 at f1176d5: Backend Java/Maven/PostgreSQL Testcontainers **SUCCESS**, Web Vite **SUCCESS**. Android SDK37 hosted runner failure persists (local checks agreed).
- Management/audit batch complete for code covered by CI. User-specific 'Clean room' visibility still requires checking its actual assignment/creator/role in the user's database.

## 2026-10-13 — Package 4 Android recurring schedule parity, initial implementation
- Added TaskScheduleOccurrenceResponse Kotlin model (7513fc0), Retrofit schedule projection / start-scheduled endpoints (9d1c346), TasksRepository methods (458c1a8).
- Android TasksScreen now loads read-only virtual recurring occurrences for the selected date, displays slot time/status and Start action for own past/current occurrence only (93efa28); graceful schedule endpoint failure keeps persisted list (e77db6a); no false empty-tasks message when only virtual schedule is present (643620e).
- Added persisted instance slotKey to Spring TaskInstanceResponse (b6d4aec) and Android model (9a70691), preserved slotKey on task navigation (15aa258), enabled slot-specific instance lookup (5178fb8, ebd313b, 5727d8e, 08c3a70).
- Android task list now hides other member selector without VIEW_ALL_TASKS/ADMIN_OVERRIDE and creation button without CREATE_TASKS/ADMIN_OVERRIDE; detail screen loads server task-permissions endpoint (1afb4ab, c000d4a, 274ef64) and hides management/subtask create controls for unauthorized users (60852db).
- All Android UI behavior is **unverified by Kotlin compilation**: GitHub hosted SDK37 install fails before Kotlin build; user agreed local Android Studio testing. Backend/Web CI pending. Follow-up: offline schedule snapshots, Android editor for schedule slots, slot-specific parent confirmation, per-role auth scenario tests.

### Android schedule offline snapshot extension
- OfflineTasksStore.Snapshot now includes nullable schedule field (8157148); nullable for backward compatibility with existing cached JSON.
- TasksRepository now fetches schedule alongside persisted tasks and writes the schedule into existing workspace/viewer/target/date-scoped cache (42fc3c5), avoiding cross-account cache leakage.
- TasksScreen shows cached planned occurrences offline as read-only and refreshes schedule after successful online sync (9c1a793). No offline queued writes are introduced.
- Android compilation remains **UNVERIFIED** until user's local Android Studio SDK37 build; CI for Backend/Web latest snapshot pending.

## 2026-10-13 — Package 4 Android slot execution parity (implementation)
- Existing Android TasksScreen already had read-only virtual occurrence projections, slot-aware start action, offline cache and member browsing guard. Added the missing slot lifecycle functionality to TaskDetailsScreen instead of duplicating it in a new UI.
- Retrofit TaskApi now supports manager completion by definition/date/slot (e1504cc); TaskDetailsRepository exposes slot-aware start and manager completion operations (4840567).
- TaskDetailsScreen now reloads on selectedSlotKey changes, shows virtual PLANNED occurrence actions for an assigned member and for managers, allows manager completion on existing PENDING/IN_PROGRESS dated slots, and hides execute controls for anyone other than the actual instance member (8c959d9, 90d4cb5). Delegation button now requires actual executor or manager.
- Android TaskDefinitionCreateRequest includes initialSlotTimes (e4953a8). CreateTaskScreen supports 2–12 time entries for ONCE and recurring schedules, localized section label, duplicate/invalid time validation, and uses the same atomic Backend create API as Web (2407790); '+' suggests the next hour (fe5e5e9).
- Android CI cannot install SDK37 on hosted runner; do not mark Android Kotlin compilation PASS until user runs local Gradle/Android Studio. Backend/Web CI pending for latest branch head.

## 2026-10-13 — Android virtual schedule REST contract hardening
- Examined current Android screen and actual Spring TaskScheduleOccurrenceResponse record. Android Kotlin model wrongly expected fields definitionId/date/time while backend emits taskDefinitionId/scheduledDate/scheduledTime; Gson would silently leave members at defaults. Fixed with @SerializedName mappings (c5347f8).
- Added JVM Gson deserialization regression for virtual occurrence with exact JSON field names, date, time, occurrence key and null instanceId (ed89223).
- Schedule retrieval failure now falls back to workspace/viewer/member/date-scoped cached schedule without hiding persisted task list. HTTP 401 still triggers normal reauthentication (4bff391).
- Android :app:assembleDebug was reported BUILD SUCCESSFUL by user before this batch. Re-run :app:testDebugUnitTest :app:assembleDebug locally after pulling these new changes, because hosted CI cannot provision Android SDK 37. No claim of successful post-change Kotlin tests until their result.

## 2026-10-13 — Android schedule consistency and default-slot guard
- Verified prior run 38404016400: backend/postgres and web successful; Android hosted runner SDK37 install failure.
- Server TaskService now rejects START and manager COMPLETE with implicit "default" slotKey whenever any explicit slots are configured, including disabled templates; avoids duplicate out-of-band occurrence/rewards (1f963ae).
- Android tasks list displays planned occurrences in chronological time order, and Start button is guarded by PLANNED status as well as member/date/connectivity checks (0f6b793).
- Local Kotlin regression/testDebugUnitTest results are not yet available for this package; Android CI remains SDK37 blocked. Manual child/parent and slot status smoke test remains mandatory.

## 2026-10-13 — Regression gate for explicit slots
- Investigated GitHub CI 38409207973: backend test `TaskManagerCompletionTest.alreadyCompletedInstanceDoesNotDebitOrRewardAgain` crashed because its mocked TaskService lacked the newly accessed TaskScheduleSlotRepository; Web passed.
- Fixed fixture injection and added regression verifying manager completion of implicit `default` is rejected if explicit slots exist (ee5b627).
- Verified GitHub CI 38427778510: Backend Java/Maven/PostgreSQL Testcontainers SUCCESS, Web/Vite SUCCESS; Android runner SDK37 setup still FAILS. Prior local Android assembleDebug was reported SUCCESS; latest Kotlin source requires separate local check.

## 2026-10-13 — Multi-slot economy idempotency tests
- Added two-slot regression in TaskManagerCompletionTest: separate completed occurrences earn 1 point and 2 reputation each; repeat confirmation does not repeat reward (34a366b, eb6e3ad).
- Added separate missed-slot regression: instances 71 and 72 each incur one penalty of 1 point and -2 reputation; repeat miss does not charge again (c490161). Completed test actor fixture for audit path (278dc92).
- Verified GitHub Actions 38430152420 at 278dc92: Backend Java21/PostgreSQL Testcontainers Maven tests SUCCESS, Web Vite SUCCESS. Android runner SDK37 setup still FAILS (unrelated).
- Existing safeguards include per-instance rewardProcessed/penaltyProcessed state and row locking for instance actions or definition locking for manager completion. Further DB concurrency/fault-injection and reward negotiation tests remain open.


## 2026-10-13 — Economy failure-injection regression (continued)
- Reviewed the transferred economics package and actual TaskService / PointService / ReputationService behavior; prior workflow 38429755217 has cancelled Backend and Android jobs, while documented run 38430152420 at 278dc92 passed Backend and Web.
- Added reward and penalty failure-injection unit tests in TaskManagerCompletionTest (5b552ace): reputation posting deliberately throws after the points service is called. Assert that the exception propagates and rewardProcessed / penaltyProcessed do not become true.
- These are mocked-service unit tests, **not** proof of DB transaction rollback or exactly-once concurrency. JPA transaction boundaries, concurrent requests, database constraints and negotiations still need PostgreSQL/Testcontainers integration tests.
- No production financial data or migration altered in this package. CI for commit 5b552ace requires verification.


## 2026-10-13 — BETA deployment vertical slice (priority shift)
- User explicitly requested stopping excessively fragmented economics packages and prioritizing a large practical Web/Android BETA deployment package.
- Inspected actual project Spring Boot 4.1.1/Java21, Vite same-origin production API base, security routing, Android debug/release API configuration, CI and backend environment variables.
- Added `Dockerfile.beta`, `frontend/Dockerfile.beta`, `frontend/deploy/nginx.beta.conf`, `compose.beta.yml`, `.env.beta.example` and `BETA_DEPLOYMENT.md` (commits 6ae7642, 792d2d2, 3c7fda5, b86e77d, ddaa157, ae0faa0).
- Added Compose syntax validation CI job in bd0c84a. The stack deliberately exposes web only on 127.0.0.1:18080, uses an isolated PostgreSQL named volume and separate internal Docker network, and requires an external HTTPS reverse proxy. No user server, database or balances were modified.
- CURRENT STATUS: files committed, server build/start/network/HTTPS/Android Beta linkage unverified. Public registration, bootstrap, cross-workspace isolation, backup/restore and external network hardening remain BETA gates.
- NEXT: inspect actual target server OS/container support and reverse proxy; deploy to isolated host/VM; verify Flyway startup and API smoke tests; enable deliberate invitation/bootstrap and Android Beta endpoint; real family end-to-end acceptance tests.


## 2026-10-13 — BETA operational follow-through after reported Android BUILD SUCCESSFUL
- User reported local Android `BUILD SUCCESSFUL`. Exact Gradle command/build revision not supplied; record as user-confirmed local assembly, not CI or release APK verification.
- Prepared configurable HTTPS Android debug API origin via `familyPointsDebugApiUrl` (c962ce1), replacing the hardcoded debug-only URL while preserving its default.
- Added BETA Web+Backend smoke check `scripts/beta/smoke.sh` (955445f) and PostgreSQL backup script `scripts/beta/backup.sh` (ee62cf6); ignored backups and dump files in Git (faf9478).
- Extended `BETA_DEPLOYMENT.md` (a5ad51c) with Windows server/Linux VM separation, localhost smoke/backup instructions, explicit Android endpoint selection and Flyway fresh-database caveat.
- Extended CI Compose-validation job to syntax-check both scripts and verify sample environment file (720bd69).
- No remote deployment, container build, complete authenticated E2E test, migration-on-empty-DB verification, or backup restore has been run; these remain gates. The project may share a Windows Server with IIS/Apache/1C; do not touch those services during BETA deployment.
- NEXT decisive action: establish target OS/VM and HTTPS reverse proxy, run isolated BETA stack and smoke, validate migrations and bootstrap identities, configure Android APK against verified BETA URL, family acceptance tests.


## 2026-10-13 — Family registration strategy correction
- Public Contabo BETA backend has a freshly migrated database; first-user SQL bootstrap proposal was explicitly stopped before file creation or DB mutation.
- Verified `AuthController` currently implements login/refresh only; `WorkspaceController.POST` is deliberately forbidden; `UserAccountService.createAccount` presupposes a member. `AccountWorkspaceMembership` exists but old one-to-one account/member coupling still applies.
- Updated `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` (941cb3e) with self-service family registration, global account multi-workspace membership, optional age/composition-informed templates, separate economies and release isolation tests.
- NEXT: implement transactional onboarding and global-account workspace-scoped security across Web/Android; no dangerous public signup shortcut or test admin SQL inserted.


## 2026-10-13 — Delivery workflow: DEV development before Contabo BETA
- User clarified implementation sequence: finish account registration, isolated family creation, multi-workspace identity and personalized initial templates on local DEV; verify integration, Web and Android; deploy tested revision to Contabo BETA only after backup/migration review.
- Existing Contabo BETA remains online and unchanged during DEV work. No manual SQL first-admin insertion.
- Deployment policy recorded in docs/MULTI_FAMILY_ONBOARDING_PLAN.md and FAMILYPOINTS_CONTABO_DEPLOYMENT_LOG.md.

## 2026-10-13 — DEV-first invitation persistence package (COMMITTED / NOT TESTED)
- Added `V27__workspace_invitations_and_join_requests.sql` (e35eea5, refined ec70a0f), a schema-only foundation for workspace invitation records (hashed token, expiry, optional recipient and role, limits/revocation), non-enumerable hashed join codes, manager-reviewed join requests, invitation redemption ledger, and durable short-lived pending invitation sessions that can survive registration and Google redirects.
- Invitation secrets and pending browser handles are represented by hashes; transactional API validation, cookie/OAuth state binding, login/signup screens and invitation redemption logic are **not implemented**. No public invitation endpoint is enabled.
- **NOT VERIFIED:** Migration V27 has not yet been run on local DEV/test PostgreSQL. Do not promote to BETA before local validation and functional implementation. Existing Contabo BETA remains on Flyway V26.
- NEXT: add server-side authenticated invitation/join request services with workspace membership authorization and HTTP interface, safe invitation-handle exchange and account registration continuity; test on isolated DEV PostgreSQL.

## 2026-10-13 — DEV workspace join request service (COMMITTED / NOT TESTED)
- Added `WorkspaceJoinService.java` in commit `412936b`: secure random one-time shareable join-code generation; hashed join-code persistence/rotation and disable; authenticated account may submit manager-reviewed join request; member managers may query pending requests or reject. No membership granted by knowing a code.
- Current service depends on V27 schema. **No controller/API routes were committed**; attempt to add controller was blocked before the file was created. Thus service is not reachable over HTTP and does not enable open joining. This is a safe partial step, not a complete registration/invitation workflow.
- Explicitly deferred: rate limiting/abuse control, code issuance verification tests, approver role mapping, secure invite redemption and cross-account identity signup continuation. Approval needs an explicitly scoped member profile/role before it can be safely implemented.
- DEV build and Flyway V27 execution remain **NOT VERIFIED**. Contabo BETA intentionally stays at V26; do not update it yet.

## 2026-10-13 — DEV API routes for workspace join requests (COMMITTED / NOT VERIFIED)
- Added `WorkspaceJoinController` in commit `fe23c79`; POST /api/workspace-joins/requests requires auth, manager-only endpoints rotate/disable code, list and reject pending requests.
- Checked `SecurityConfig`: all routes require authentication by default; method security enabled. Manager routes invoke `workspaceSecurity.canManageMembers(workspaceId)`. Code possession alone cannot grant membership.
- **NOT TESTED** Maven compilation, Flyway V27, integration tests, and running DEV HTTP responses. No approval, invite redemption, signup continuity, rate-limits or user interface yet.
- Contabo BETA intentionally remains unchanged. Next: local compile, test and implement protected invite flow and account registration; no production rollout.

## 2026-10-13 — DEV join request approval (COMMITTED / NOT YET TESTED)
- Added transactional manager approval in `WorkspaceJoinService` (2c822d7) and POST `/api/workspace-joins/workspaces/{workspaceId}/requests/{requestId}/approve` (0f5b051). Request takes `roleId`, `memberName`.
- Service row-locks a pending request, validates role belongs to requested workspace, blocks elevated roles with ADMIN_OVERRIDE/MANAGE_MEMBERS/MANAGE_ROLES or FAMILY_ADMIN, rejects existing account membership, creates a new independent workspace member with type OTHER and an account-workspace link, then marks request APPROVED with reviewing account. All writes occur in one Spring transaction.
- Does not modify legacy `user_accounts.family_member_id`; global-account multi-workspace switching must use existing membership resolver.
- **NOT VERIFIED:** DEV Maven compile/tests, Flyway V27 applied, concurrent approval tests or full authorization negative tests. No Contabo BETA rollout. Future work: inviter flow, OAuth signup context, throttling, read UX, role/identity hardening.

## 2026-10-13 — Invite lifecycle contract; implementation blocked (DOCUMENTED)
- Reviewed committed V27 tables, existing API, Spring Security default authenticated routes and workspace join-request approval.
- Attempted to commit DEV invitation issue/preview/pending-onboarding service, but GitHub tool rejected the write; no source file was created and no runtime state changed.
- Updated `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` in b39c811 with explicit server contract: workspace-limited issued token, digest-only storage, minimal preview, protected pending signup context through Google redirect, authenticated acceptance with replay/expiry/revocation checks, and rate-limited abuse controls.
- Pending: actual service and controllers, multi-workspace registration, tests, Flyway V27 validation. Contabo BETA remains unchanged.

## 2026-10-13 — DEV invitation issue/preview/revoke API (COMMITTED; NOT TESTED)
- `WorkspaceInvitationService.java` (26ff7a1): issues random opaque seven-day invite tokens and persists only SHA-256 digest; previews minimal family label/expiry for valid unrevoked invitations; allows revocation by ID. No membership granted by opening invite.
- `WorkspaceInvitationController.java` (c1fb57c; hardened 911beaf): manager-scoped issue/revoke with `MANAGE_MEMBERS`; authenticated-only POST preview with token in JSON body, not URL query. This preview is intentionally not yet usable by a visitor without an account.
- Pending: rate limiting and abuse protection before anonymous preview; short-lived secure pending invite context across password/Google registration; explicit acceptance with transactional replay/limit checks; expiry integration tests and local Maven/Flyway V27 verification.
- Contabo BETA remains untouched at V26. Treat these commits as development-only.

## 2026-10-13 — DEV invitation pending context (COMMITTED / NOT TESTED)
- Added short-lived server-backed invite context exchange/resume to `WorkspaceInvitationService` (852a0bd), supported by preexisting V27 `pending_invitation_sessions` schema. Only SHA-256 digest of random context handle is persisted. Both context and underlying invitation validity, revocation and use count are checked on resume.
- Added POST `/api/workspace-invitations/context` and `/api/workspace-invitations/context/resume` (30da43d). **Both routes are authenticated-only** in this incremental build, not anonymous first-run signup; request body carries token/handle so it is not in URL query.
- These routes do not create membership, redeem invitation, or secure OAuth/browser persistence. No automatic creation of user/family has been enabled. Future signup continuity needs browser-bound cookie/state strategy and validation before opening anonymous routes.
- No local compile, V27 Flyway, or API test results received yet. Keep Contabo BETA at V26. Next: compile and integration tests on DEV, then gated signup/OIDC and atomic invitation acceptance.

## 2026-10-13 — DEV authenticated invitation acceptance (COMMITTED, UNTESTED)
- `WorkspaceInvitationService` (d94d480): invitation creation now requires an explicit destination workspace role; validates role belongs to workspace and excludes admin/privileged permission sets. Added transactionally atomic authenticated `accept(handle, memberName)`: checks handle expiry and invitation revocation/usage, locks invitation/session rows, rejects existing membership, creates workspace-local member and account link, writes redemption audit, increments use count, consumes handle.
- `WorkspaceInvitationController` (a96480d): issued invitation endpoint now requires JSON `{"roleId":123}` (breaking DEV API change); added POST `/api/workspace-invitations/accept` with JSON `{"handle":"...","memberName":"..."}`.
- Explicitly excludes email-targeted invitations for now (the current issue endpoint creates only unaddressed invites); recipient restrictions and account identity linking need separate implementation. No account is created by acceptance: authenticated existing user only.
- **NOT VERIFIED:** compile, V27 migration, concurrency tests, security test coverage, OAuth continuation or Web UI. Before rollout check error handling, user role mapping and integration tests. Contabo BETA unchanged at V26.

## 2026-10-13 — DEV invitation security unit tests (COMMITTED, EXECUTION PENDING)
- Added `WorkspaceInvitationServiceTest` (775ce82): invalid invite and session handles fail before DB query; privileged role issuance refused; expired/consumed invite context refused; invalid member name/handle rejected on acceptance.
- These are fast unit-level negative tests. They do NOT prove race safety, correct membership creation, Google redirect persistence, V27 schema migration, or full tenant isolation.
- **Not yet run** in CI/DEV; local user should fetch branch and run Maven targeted test first. Contabo BETA remains unchanged.

## 2026-10-13 — Consolidated DEV onboarding validation checkpoint (COMMITTED / NOT EXECUTED)
- Inspected existing `UserAccount`, `CustomUserDetailsService`, JWT and `FamilyWorkspaceProvisioningService`. Current account still has non-null legacy `family_member_id`, and default authentication authorities derive from that membership. Full global-identity decoupling requires coordinated migration and authorization updates, not a standalone registration endpoint.
- Attempted commit of gated account-creation service, but GitHub action blocked the write. That Java file was NOT created; no registration API was exposed.
- Added `scripts/dev/check-onboarding.ps1` (a48f372): single command displays git working tree, checks Flyway version inventory for V27 and duplicate numbers, compiles Java without touching DB; optional `-WithTests` runs Maven tests.
- Script has NOT been executed on user's laptop. Run in DEV first, inspect results, then implement coordinated identity/registration with integration tests before BETA deployment. Contabo BETA unchanged.

## 2026-10-13 — DEV first-family registration API package (COMMITTED; UNTESTED)
- Implemented `FamilyRegistrationService` (37c7279): transactionally provisions a new workspace + default FAMILY_ADMIN/PARENT/CHILD roles and POINTS economy via existing FamilyWorkspaceProvisioningService, creates BCrypt-backed account and first account-workspace membership. Validation rejects malformed username, weak password, missing names and duplicate usernames.
- Added `FamilyRegistrationController` (4cd6326), POST `/auth/register-family` taking `username/password/familyName/ownerName`; HTTP route enabled at security filter (65b30ef) but controller rejects requests with HTTP 503 by default unless `familypoints.registration.enabled=true`. **Do not enable outside isolated DEV until throttling, data validation, CSRF/signup abuse controls, multi-tenant tests and unique-key race handling are ready.**
- Added negative unit tests `FamilyRegistrationServiceTest` (43474e9). Neither Maven compile nor tests ran in this agent. First-user account still uses legacy `UserAccount.familyMember` compatibility; this is not yet a standalone global identity or Google login.
- Pending: full integration and concurrent username duplicate tests; username/password rate limiting; Web form; authenticated existing-user create-another-family; signup invitation continuity, global-identity migration and isolated DEV acceptance. Contabo BETA deliberately unchanged at V26.

## 2026-10-13 — DEV multi-family session & second family creation (COMMITTED / NOT TESTED)
- Added `WorkspaceSessionController` (7cc6c85): authenticated GET `/auth/workspaces` lists only active member workspaces for current account; POST `/auth/workspaces/{workspaceId}/select` checks active membership with WorkspaceSessionService and returns JWT explicitly scoped to that workspace. JWT filter already revalidates scoped membership and uses its permissions on each request.
- Added `AdditionalFamilyService` (7a47f32) and `AdditionalFamilyController` (5284fd0): authenticated existing manager account can transactionally create a second family and its admin member/profile, with account-workspace link; does not overwrite legacy user_accounts.family_member_id. Feature toggle `familypoints.additional-family.enabled=false` is default; controller requires MANAGE_MEMBERS or ADMIN_OVERRIDE.
- No migration added, preserves V21/V22 unique workspace membership constraints and V27 for invitation flow.
- **NOT VERIFIED:** Maven compile, test suite, manual API switching or tenant-isolation negative tests. Must also review legacy authorities when no explicit workspace JWT and ensure all workspace-scoped endpoints enforce tenant context before exposing broader signup. DEV only, Contabo BETA unchanged.

## 2026-10-13 — Larger DEV React registration package (COMMITTED; BUILD NOT VERIFIED)
- Inspected `frontend/src/context/AuthContext.jsx`, `frontend/src/api/authApi.js`, `AppLayout.jsx`, and backend `AccountWorkspacesController`/`WorkspaceTokenController`: workspace selection and switching UI already existed and matched `/api/me/workspaces/{workspaceId}/activate`. Avoided adding duplicate functionality.
- Added `frontend/src/api/registrationApi.js` (8f45156) and `frontend/src/pages/RegisterFamilyPage.jsx` (e4c2db2) for first-family signup plus automatic password login after successful registration.
- Wired `/register` route into `App.jsx` (b6ed94f), conditional entry point in `LoginPage.jsx` (47d8219). UI is hidden unless `VITE_ENABLE_FAMILY_REGISTRATION=true`; backend separately defaults `familypoints.registration.enabled=false`. Both must be explicitly enabled on isolated DEV only after security review. Existing BETA is untouched.
- Extended `scripts/dev/check-onboarding.ps1` (7955bb2) to build React frontend in addition to Java compilation and optional tests.
- NOT YET TESTED: Maven, React build, local DEV HTTP signup, duplicate username race, abuse limits, invitation continuity, Google OIDC. Do not expose public signups.

## 2026-10-13 — Visual DEV onboarding without real user data (COMMITTED / BUILD UNVERIFIED)
- `RegisterFamilyPage.jsx` (a6e9646): /register now renders by default as a **visual-only demo**, without needing VITE_ENABLE_FAMILY_REGISTRATION. Includes sample data button, local validation and a client-only next-step preview of family/parent/admin role/POINTS. In default mode submission does NOT call backend and does NOT save passwords, memberships or workspace records.
- `LoginPage.jsx` (8fc80f2): always shows a link to the demo registration route.
- Intentional guard: real registration remains separately disabled in backend by default; production form can only call server if VITE_ENABLE_FAMILY_REGISTRATION=true and backend registration explicitly enabled. Do not enable during visual checks.
- This is frontend-only; no changes to Contabo BETA. React build and browser behavior must be verified on local laptop after Git update and Vite restart. Never use real personal data in demo.

## 2026-10-13 — Four-stage local visual onboarding wizard (COMMITTED; NOT BUILD-VERIFIED)
- Replaced simplistic demo register page with 4-stage in-memory React wizard: family name/admin, optional extra members and child ages, economy POINTS with configurable example task points and illustrative screen-time limit, and summary/finish preview.
- New responsive CSS under `frontend/src/pages/RegisterFamilyPage.css`. Changes committed as 89b260a and 9cfa3de.
- Button fills exclusively fictional sample records; all actions are React state changes only: no fetch(), localStorage writes, account registration or PostgreSQL mutations.
- It is intentionally a visual-only demo, including when user has a prior authenticated session. It must not be treated as a completed signup or as configured enforcement of screen-time settings/tasks.
- Untested in local Vite build/browser; next stage: local visual review, accessibility/translation refinements, then validated mapping of approved onboarding selections into versioned server templates.
- Contabo BETA not updated.

## 2026-10-13 — Database tenant-role integrity hardening (COMMITTED / NOT TESTED)
- Added Flyway V28 (a40e33e): `family_members.family_role_id` and `workspace_invitations.member_role_id` must point to roles owned by exactly the same workspace. PostgreSQL triggers reject cross-tenant inserts/updates even outside application authorization.
- Migration explicitly checks existing rows and fails rather than silently moving or deleting mismatched tenant data.
- **Not run** on DEV PostgreSQL; backup and validate V27/V28 on a disposable test database before touching DEV. No Contabo BETA changes. Existing visual wizard remains demo-only.

## 2026-10-13 — Backend tenant hardening and regression alignment (committed, not executed)
- Registration validation (b87572c) now rejects names over 100 characters and returns explicit client HTTP errors. Updated negative unit tests (3fc15bf) for the new error types and field length.
- WorkspaceMembershipResolver (ec4c7a6) now also verifies that the selected member role belongs to the selected workspace. Updated existing resolver tests, including a foreign-workspace role scenario (6a93a0a).
- No frontend updates. No tests have been run on the user laptop, no migration or deployment performed. Public registration flags remain disabled. Before rollout run Maven and Flyway verification on isolated DEV PostgreSQL.

## 2026-10-13 — Consolidated DEV regression checkpoint (code committed, tests NOT run)
- Audited V27–V29 onboarding/membership services and current Java unit tests. Fixed the invitation test JdbcTemplate mock signatures (b25b87c) to match actual method arguments; existing workspace session isolation tests already covered selected member permissions and forbidden foreign workspace.
- Extended `scripts/dev/check-onboarding.ps1` (066e7a8) with `-BackendOnly` and mandatory V29 inventory check; `-WithTests` still runs Maven tests, optional React build runs without `-BackendOnly`.
- All GitHub changes are DEV-only and do not modify a running database or Contabo. Compile, Flyway migrations and tests remain UNVERIFIED without a local execution report.
- Next hard gate: run consolidated checkpoint on laptop, fix real compiler/test failures, then run disposable PostgreSQL migration tests and two-family permission scenarios. Avoid new public registration/OIDC features until this succeeds.

## 2026-10-13 — First real Maven test failure, targeted fix
- User ran local Maven tests and reported one error: CurrentUserWorkspaceContextTest.legacyAuthenticationStillUsesPrimaryMember raised 403 at CurrentUserService.getCurrentMember:69.
- Cause: legacy test mocked WorkspaceMember but never set isActive(), so Mockito default false caused correctly fail-closed service behavior.
- Test fixture fixed with isActive=true; added negative tests for missing and inactive primary members (f341c26). Production authorization rules intentionally unchanged.
- Awaiting user to rerun the consolidated tests; fix is committed but not verified locally by assistant.

## 2026-10-13 — Disposable PostgreSQL integration gate added (COMMITTED / NOT YET RERUN)
- Extended existing Testcontainers PostgreSQL 17 Spring Boot integration suite rather than introducing an independent database script. The suite creates a disposable DB and Flyway migrates from V1 to the current V29; local/Contabo DB untouched.
- New tests: verify successful Flyway V27/V28/V29 and external-identity table (45e3948), reject role from different family on member insert (45e3948), permit family-less global account and reject foreign role in invite (4271fba), reject duplicate verified provider+subject assignment to another account (433189e).
- Requires Docker Desktop running for integration tests. Maven/unit test PASS was reported before this package; newly added PostgreSQL tests have NOT YET been executed.
- Public registration, Google login and Contabo release remain disabled/pending.

## 2026-10-13 — Join/invitation end-to-end service tests (COMMITTED; EXECUTION PENDING)
- Fixed WorkspaceJoinService duplicate pending request handling: INSERT ON CONFLICT DO NOTHING previously returned success; now checks affected rows and returns HTTP 409 (8749f56).
- Added disposable PostgreSQL integration scenarios (79ee290): 48-hex join code -> pending request only -> duplicate 409 -> manager approval -> membership in only target workspace, plus invitation issue -> preview -> short-lived context -> acceptance -> one-use-only -> no outsider workspace membership.
- Tests use existing Spring Boot + Testcontainers PostgreSQL 17 setup; they do not send HTTP through MockMvc for join/invite operations. Controller authorization integration still requires separate tests.
- No running database, frontend, Android or Contabo BETA modified. Maven tests NOT run against new commits.

## 2026-10-13 — HTTP and revocation regression package (COMMITTED; NOT RUN)
- Fixed Java test request-body escaping in existing MockMvc manager-authorization scenario (006d4e5).
- Extended Testcontainers PostgreSQL tests: invitation created and pending context established, then revoked; subsequent resume/accept returns 404 and cannot create membership. Rejected join request drops from pending queue, cannot subsequently be approved and does not create membership (99cf990).
- Coverage now includes manager-only HTTP join-code rotation, cross-workspace deny, duplicate joins, approvals, redemptions, revocation and rejection. These are automated test definitions, not yet confirmed passing.
- No real data and no Contabo BETA changes. Await local Maven full suite.

## 2026-10-13 — Global-account-only signup foundation (PARTIAL, UNTESTED)
- Implemented `GlobalAccountRegistrationService` (2d12637): validates credentials, hashes password and saves a global UserAccount without a primary family; relies on existing V29 nullable family_member_id.
- Added `GlobalAccountRegistrationController` (6ff086d) on POST /auth/register-account, gated by `familypoints.global-account-registration.enabled:false` (default OFF).
- Added isolated service tests (56562dd) for invalid/duplicate username and no implicit family membership; NOT RUN.
- **Blocking issue:** SecurityConfig still requires authentication for /auth/register-account. Attempts to update SecurityConfig were blocked; anonymous registration is not operational even if flag enabled. Keep the flag OFF.
- AdditionalFamilyController still requires workspace manager authority, so family-less accounts cannot yet create first family. Must complete cohesive account creation/first-family flow after authorization and test review.
- No deployment, database migration or frontend changes. Contabo BETA unchanged.

## 2026-10-13 — First-family onboarding implemented, pending verification
- Added POST /api/onboarding/first-family (ec73aab), authenticated and gated by familypoints.first-family.enabled:false.
- Added FirstFamilyOnboardingService (1625c1b): rejects disabled accounts and existing family membership, provisions first workspace using existing AdditionalFamilyService.
- Added pessimistic account-row locking via UserAccountRepository.findByIdForUpdate (16a1d25, 0aa8001) to serialize concurrent first-family requests.
- Added isolated unit tests (0846ab4); tests not run yet. Feature remains disabled. No Contabo deployment.

## 2026-10-13 — Global account workspace chooser bug fixed
- Existing /auth/workspaces and /auth/workspaces/{workspaceId}/select APIs already implement workspace selection and scoped JWT after live membership validation; no new duplicate endpoint created.
- Fixed /api/me/workspaces list for globally registered accounts with zero memberships: it previously always resolved getCurrentWorkspaceId(), which returned 403 when primary member null. The active selection is now derived from a verified WorkspaceAuthenticationDetails claim on the authenticated request, or null for legacy/global unscoped sessions (838a4f7).
- Updated AccountWorkspacesControllerTest for scoped selected family and zero-family account list (73665da).
- Not yet built locally; user has prior unstashed changes risk and should review git status before pull.
- Public registration/first-family flags remain OFF; Google OAuth not enabled; no BETA deployment.

## 2026-10-13 — Google ID token verification foundation (COMMITTED, NOT YET RUN)
- Added managed `spring-security-oauth2-jose` dependency (a779e30) for Google JWT cryptographic verification.
- New `GoogleIdentityService` (a2007e4, 1301ea3): validates Google JWK signature, expiry, issuer, audience (=configured Google client ID), authorized party for multi-audience tokens, then selects global account ONLY via `account_external_identities(provider='google',provider_subject=sub)`. An unknown or unlinked Google subject is rejected. Email alone never links accounts.
- Added `POST /auth/google` for already-linked enabled accounts, returning existing application JWT and gated by `familypoints.google.enabled:false`; default OFF (252e4c8, f806042).
- Added isolated disabled/allowed controller regression tests (0affbff), NOT RUN.
- **Scope:** no Google account creation or association UI, no account-link flow or browser Google OAuth redirect in this package. Provisioning must require secure proof of authenticated account ownership and verified Google subject.
- Maven build and integration tests must be run before enabling flag; existing tests from earlier packets are also unverified. Contabo BETA unchanged.

## 2026-10-13 — Google account-link package completed in code (tests not yet executed)
- Implemented authenticated POST /api/account/google/link, gated by familypoints.google.linking-enabled:false (b202120); requires current password and verified Google ID token before linking.
- Refactored GoogleIdentityService to use the same GoogleTokenVerifier as GoogleAccountLinkService rather than maintaining separate validation logic (0b43b07).
- Added isolated GoogleAccountLinkService and GoogleIdentityService tests for password check, unlinked subject rejection and enabled account lookup (19cd5a3, fbbdc58).
- DB V29 unique constraints remain authoritative against duplicate provider-subject and multiple Google identities per account. Email never used for implicit linking.
- Code committed, Maven/containers have NOT yet been executed. Flags remain disabled. No Contabo BETA change.

## 2026-10-14 — Web global account onboarding and Android API contracts (BUILD PENDING)
- Web AuthContext now treats password login as global identity, fetches workspace choices, and routes to /choose-family instead of immediately requiring /api/me. Restored unscoped/global tokens can remain authenticated with no selected family.
- Web added /register-account and /choose-family pages and API calls for feature-gated global account signup and first-family creation. A selected family is verified by /api/me before saving a scoped JWT and remounting tenant state.
- Web login page and ProtectedRoute redirect users without an active workspace to the chooser; existing legacy application views are unchanged.
- Android AuthApi gained typed registerAccount and createFirstFamily contracts. Android Compose UI and login session handling for family-less/multiple-choice users remain unfinished; do not claim Android flow complete.
- Backend public registration and first-family creation remain OFF by default. No production or Contabo deployment.
- Frontend build and Android Gradle build NOT executed by assistant. Require laptop smoke test and resolve failures before enabling.

## 2026-10-14 — Android onboarding implemented (build not yet verified)
- Added global-login workspace choice, first-family Compose screen and Android signup mode; unscoped JWT no longer calls /api/me before workspace selection.
- Android changes committed as 455e931, 563303d, 7126a15, a46a190, 27a2058 and 30811a2.
- Gradle build and emulator tests outstanding. Server registration flags stay OFF. Contabo unchanged.

## 2026-10-14 — Post-onboarding guard fixes, awaiting local build
- Android: successful global authentication followed by workspace-selection network failure no longer silently downgrades to offline login (38c79e4).
- Android: expected WorkspaceSelectionRequiredException no longer flashes a generic login error during navigation (d4ea687).
- Web: global account with no selected workspace now also participates in idle expiry/token refresh checks, not only accounts with active /api/me context (d9b2e22).
- Source reviewed against JwtService: workspace JWT claim is precisely workspaceId.
- Android assembleDebug and Web npm build NOT run in this environment. Do not assume BUILD SUCCESS. No Contabo BETA changes.

## 2026-10-14 — Additional family creation in Web (verification pending)
- Added POST /api/account/families client, AuthContext createAdditionalFamily and header button '+ Створити сім’ю' with a dialog for family name and owner name.
- Button appears also for one-family accounts if current member has MANAGE_MEMBERS or ADMIN_OVERRIDE; existing workspace selector still appears only if 2+ choices.
- On success, verifies and activates newly created workspace JWT and reloads feature context. Backend API already existed and continues to enforce permissions and familypoints.additional-family.enabled:false default.
- Commits a8f3212, 14bc4c9, cff656e. No frontend build executed here, no DEV flag enabled, no Contabo deployment.

## 2026-10-14 — Additional-family local DEV flag
- Screenshot confirms Web creation dialog works and backend returns 503 when familypoints.additional-family.enabled remains OFF.
- Added scripts/dev/run-backend-with-family-creation.ps1 (936f9bb) to explicitly launch local DEV Spring backend with FAMILYPOINTS_ADDITIONAL_FAMILY_ENABLED=true only in process environment; requires existing DEV DB_PASSWORD and JWT_SECRET.
- BETA/Contabo unchanged; end-to-end family creation and isolation not verified.

## 2026-10-14 — Web header family controls layout
- Moved family selector and create-family button to the left header section; kept language switcher, member identity and logout on the right.
- Family select now shares .language-switcher styling, with width/overflow limits and wrapping at narrow viewport widths. Commits: 368d187, 1b2e915.
- No BETA deployment; frontend build to be verified on local DEV.

## 2026-10-14 — Compact Web buttons and chooser footer (BUILD PENDING)
- The workspace chooser now presents families top-left and logout independently at bottom-right (f72e81a).
- Added a common compact, neutral outlined button treatment for ordinary Web actions and login/chooser actions, preserving selected navigation, icon-only and destructive controls (23dec37).
- React build and screenshot regression checks outstanding; Android/Backend/Contabo untouched.

## 2026-10-14 — Email verification / Google Web signup integration (COMMITTED, NOT BUILD-VERIFIED)
- New additive Flyway V30 with nullable account email, email_verified_at, case-insensitive unique email index and hashed one-time verification/reset tokens (c0c7fac). Legacy accounts retain access.
- Normal password registration now mandates email and creates a disabled account until confirmation (a28ba70, d94e91f); SMTP sender, 24h confirmation, 30min reset and resend with 2min per-account throttle implemented (9c3c124, da6ff12, 42f135d, 6f4f712).
- Google verified-token signup creates a new global account only with verified Google email; matches to existing emails are blocked and never auto-linked. Public Google registration endpoint feature-gated OFF (c892434, 8ad0b31).
- Web now has email registration/verification/password reset pages and conditional Google Identity Services button controlled by VITE_GOOGLE_CLIENT_ID. Android registration now requires email but native Google sign-in remains outstanding.
- SMTP and Google client IDs require configuration; see docs/ACCOUNT_EMAIL_GOOGLE_SETUP.md. All public feature flags remain OFF by default. No changes to Contabo.
- Local Maven/Vite/Android builds and full DB integration were not run in this assistant session; do not enable public signup or deploy until release gate passes.

## 2026-10-14 — Email is shared contact, username remains unique
- Product decision: parents can create accounts for children using one guardian email; username is the unique login.
- Added V31 to drop V30 unique email index safely if V30 was already migrated (28ae4e1).
- Password and Google registrations no longer reject email reuse, but retain unique username / Google-sub identity constraints.
- Recovery and verification resend now require username+email, preventing ambiguous email-only account selection. Web forms and tests updated.
- Build / Flyway V31 tests not yet run. No Contabo deployment or flags changed.

## 2026-10-14 — Eight-character minimum password
- Password registration and password recovery use 8–128 characters consistently in controllers/services and Web forms.
- Legacy gated family registration now also accepts 8–128; account profile password changes already required 8 minimum.
- Added boundary tests for 7-character rejection and 8-character acceptance in global signup.
- Android registration delegates validation to server; no local 12-char constraint.
- Requires laptop Maven/Vite/Android build verification. Existing SMTP and Google provisioning remain configuration-dependent; Contabo unchanged.

## 2026-10-14 — Auth provider hardening and DEV preflight
- Previous user local build gate confirmed: Backend Maven tests PASS, Web Vite PASS and Android assembleDebug PASS before this packet.
- Added HTTPS-only callback URL policy for email links, with explicit HTTP exception for localhost/127.0.0.1 local development (21b4a25).
- Added initial EmailTokenService tests for malformed/expired tokens, token hashing, cooldown and URL safety (165c86b).
- Added read-only scripts/dev/check-auth-config.ps1 to verify SMTP/Google environment presence without displaying secrets (9b4a3d2).
- These new changes are committed but NOT freshly compiled/tested. No SMTP account, OAuth client, DNS or Contabo settings created or modified.
