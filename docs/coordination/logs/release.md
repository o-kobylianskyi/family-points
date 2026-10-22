# FamilyPoints — Release / DevOps journal

New batches: CI, tags, VPS, deployment, database backups and Android APK publication. Keep details and operational history in `FAMILYPOINTS_CONTABO_DEPLOYMENT_LOG.md`.

## 2026-10-22 — Track initialized (DOCUMENTATION ONLY; NO DEPLOYMENT)
- Incoming: IMP-0001 visuals/assets, IMP-0002 backend auth/flags/CORS, IMP-0003 APK crash, IMP-0004 multi-family isolation, IMP-0005 offline consistency.
- Separate release gates required for source SHA/tag, real CI/device/HTTP tests, database migrations and backup/rollback. No SSH, server, DNS, database or APK modifications performed by this task.
- Never touch unrelated Contabo WireGuard, DEV port 8443, 1C/BAS routing or existing user data without specific authorization.

## 2026-10-22 — Release/DevOps intake and deployment gate review (SOURCE/DOC REVIEW ONLY)
- Observed active branch `feature/task-soft-delete` at `3bc7a88de10298ddb6b6f2acbe8172361f7d5617`; `main` lags at `c6847c6` per shared state. Read `AGENTS.md`, coordination README/PROJECT_STATE/IMPACT_REGISTER/ADRs/protocol, existing Release track log, Contabo journal, Android APK distribution guide, `compose.beta.yml`, release gate workflow, `scripts/beta/deploy-release.sh` and `scripts/android/publish-apk.sh`.
- Reviewed incoming release impacts IMP-0001..0005. **Review intake only; Release ACK/acceptance still pending** until consumer code/contract checks and required build/runtime tests. No checkbox changed in the shared impact register.
- The historical operations log records a running BETA stack, but current VPS/container/tag/DB state has **not** been live-verified in this session. Later Google registration CORS and invitation fixes were reported **COMMITTED/UNDEPLOYED**; do not treat the newest branch as published. Server-host `compose.beta.yml` had a manual feature-flag hotfix: reconcile working-tree differences and protect private `.env` before invoking `deploy-release.sh`, whose clean-tree preflight would otherwise stop.
- Android distribution is **BLOCKED** pending rebuild and real-device verification of the corrected signed APK after the previously observed release startup `IllegalAccessError`. Preserve release signing identity, increment `versionCode`, verify in-place upgrade, SHA-256, manifest and handset login before publishing `/downloads/` files.
- Deployment gates for each tagged release: exact annotated tag/SHA and successful relevant CI; Backend/Web/Android contract review; Flyway migration risk and real DB backup with restore plan; scoped two-family authorization tests; BETA HTTPS smoke and critical auth/onboarding flows; verified on-device signed APK where applicable; rollback and environment-drift record.
- Independent infrastructure risk from the Contabo journal: the separate customer test 1C HTTPS-IP certificate was recorded as expiring **2026-10-21** with auto-renewal scheduled. As of 2026-10-22, verify **actual certificate renewal and Nginx reload** by read-only inspection; do not assume the timer succeeded. This is not permission to alter customer 1C, WireGuard or firewall rules.
- Tests this session: GitHub source/document review only. **No VPS SSH, host changes, CI execution, DNS/TLS change, DB backup/restore, signed APK build/publication or deployment**.
- Next actions: read-only VPS release/tag/working-tree and cert inventory; reconcile uncommitted host overrides with Git; inspect actual CI/tag outcome; collect Android phone release verification; then propose a pinned, explicitly approved BETA publication packet.
