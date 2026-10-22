# FamilyPoints — main-only development and safe Windows migration

**Adopted:** 2026-10-23 · **Canonical GitHub branch:** `main` · **Local Android migration:** PENDING.

## What was done remotely
- GitHub `main` was fast-forwarded to `393f3ee38419ff8657cc89813ecb55443ab23042`, previously the head of `feature/task-soft-delete`. It was a linear ancestor relationship, 1996 commits ahead and 0 behind — no forced update, no history rewrite and no branch deletion.
- Backend draft PR #4/calendar and PR #5/media now target `main`, remain DRAFT and unmerged. The CI workflow runs on pushes to `main` and on PRs. BETA is triggered by release tags/manual process, not arbitrary main pushes.
- The Android topic branch has diverged; a screenshot reports **18 local-only commits plus uncommitted Web/CSS/docs**. Those cannot be recovered or migrated by the GitHub connector, and they are NOT included in the remote promotion.

## Never lose the local work
**The following PowerShell procedure is a guided plan, not a command to run unattended.** In the existing FamilyPoints directory, first inspect local Git status, worktrees, branch differences and remotes.

```powershell
git status --short --branch
git branch -vv
git worktree list
git remote -v
git fetch origin
git log --oneline --left-right --cherry-pick origin/main...HEAD -n 35
```

**Before switching away from the Android branch**, create a uniquely named backup branch pointing to all current local commits and a stash retaining tracked/staged/untracked edits. For important **ignored** files, make a separate private filesystem backup (stash -u does not cover ignored data; do not add credentials, signing keys or private family data to Git).

```powershell
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backup = "backup/android-before-main-$stamp"
git branch $backup HEAD
git stash push -u -m "FamilyPoints local work before main $stamp"
git stash list
git branch --list $backup
```

Stop and inspect any nonzero error instead of continuing. Then safely update the local main checkout:

```powershell
git fetch origin
git switch main
git pull --ff-only origin main
git status --short --branch
```

If local `main` doesn't exist, `git switch --track -c main origin/main`; if `main` is checked out in another worktree or pull refuses fast-forward, stop and inspect instead of deleting/reinitializing it.

**Integrate** the Android backup into current `main`, preserving the Web fixes in current main. Choose the *real* backup branch name from `$backup` or `git branch --list 'backup/android-before-main-*'`.

```powershell
git merge --no-ff $backup
git status --short --branch
```

If conflicts appear, reconcile all code and docs, especially Android Compose, Web CSS and shared API dependencies. Use `git diff --check`; complete with `git add <reviewed files>` and `git merge --continue`. If unable to finish, `git merge --abort` safely returns to before the merge, while backup ref remains untouched.

After a successful merge, restore the stashed local edits **without deleting the stash**:

```powershell
git stash list
git stash apply 'stash@{0}'
git status --short --branch
```

The stash may cause conflicts. Resolve and test, then commit only verified files. Never run `reset --hard`, `git clean -fd`, `git push --force` or delete the backup before confirming all local-only commits and files are retained. `stash@{0}` is illustrative; check the actual entry before applying it.

## After consolidation
- Run affected tests: Android Gradle debug/unit, Web `npm run i18n:check` and `npm run build`, and Backend tests if shared API changed.
- Check `git fetch origin` and compare `main` to `origin/main` before pushing. Resolve other-track commits without overwrite; normal `git push origin main` only after reviews and CI plan. GitHub push rejection is a reason to reconcile, **not force**.
- Keep the Android topic branch, backup and stash until all unique commits and files have been reviewed in `main`; only then consider cleanup.
- Each chat starts with `origin/main` for current source/doc state; temporary feature PRs target `main`; no permanent alternative integration branch. Release still separately checks pinned/tagged source, DB migration, tenant rights, backup and signed APK. No automatic DEV/BETA/PROD deploy.

## Handoffs
- Android: [XTR-20261023-P-05](requests/XTR-20261023-P-05.md).
- Web: [XTR-20261023-P-06](requests/XTR-20261023-P-06.md).
