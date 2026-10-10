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
