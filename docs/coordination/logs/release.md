# FamilyPoints — Release / DevOps journal

New batches: CI, tags, VPS, deployment, database backups and Android APK publication. Keep details and operational history in `FAMILYPOINTS_CONTABO_DEPLOYMENT_LOG.md`.

## 2026-10-22 — Track initialized (DOCUMENTATION ONLY; NO DEPLOYMENT)
- Incoming: IMP-0001 visuals/assets, IMP-0002 backend auth/flags/CORS, IMP-0003 APK crash, IMP-0004 multi-family isolation, IMP-0005 offline consistency.
- Separate release gates required for source SHA/tag, real CI/device/HTTP tests, database migrations and backup/rollback. No SSH, server, DNS, database or APK modifications performed by this task.
- Never touch unrelated Contabo WireGuard, DEV port 8443, 1C/BAS routing or existing user data without specific authorization.
