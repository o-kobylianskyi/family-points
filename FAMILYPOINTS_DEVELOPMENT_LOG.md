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
