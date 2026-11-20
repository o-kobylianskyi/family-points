# REL-BETA-STORAGE-01 — encrypted archive volume and recovery runbook

**2026-11-19 / proposed configuration ONLY; no BETA host operation or release authorization.**

## Scope / invariant
- `compose.beta.yml` keeps PostgreSQL in named `beta_postgres` and adds **separate** named Docker volume `beta_backup_artifacts` **only** to the backend, at `/var/lib/familypoints/backups` (read/write).
- Backend receives `FAMILYPOINTS_BACKUP_LOCAL_DIRECTORY=/var/lib/familypoints/backups`. Spring binds exactly `familypoints.backup.local-directory=${FAMILYPOINTS_BACKUP_LOCAL_DIRECTORY:}`; absent variable remains empty and the storage component refuses file operations. **Do not set it globally in other profiles.**
- No archive volume is mounted in Web, its Nginx downloads directory, or PostgreSQL. No port is opened for backend/database; Web remains localhost-only.
- Storage uses encrypted `.fpbackup` bytes, workspace ID directory, UUID name, SHA256 verification, hard-link no-overwrite publication, no symlink reads. A filesystem directory/volume is **not** evidence that its backups are RESTORABLE.
- This patch does not enable public Restore, Reset Execute, physical retention purge, scheduled deletion, container deployment, tag, APK publishing, data migration, or real family writes.

## Static verification (non-deploying; synthetic dummy secrets only)
From repository checkout, with Docker CLI + Compose available:
```bash
DB_PASSWORD=ci-dummy-db-password JWT_SECRET=ci-dummy-jwt-secret docker compose -f compose.beta.yml config --quiet
python3 -m unittest discover -s scripts/beta -p test_archive_volume_config.py -v
```
The Python suite independently obtains `docker compose ... config --format json` with dummy values and checks **effective** environment, mount type/path/volume, Web/database isolation, locality of ingress, disabled feature flags, and purposely corrupted negative variants. It does NOT start containers or read the host `.env`. Standard CI BETA Compose job invokes it and the full workflow still requires exact-SHA non-skipped evidence for release qualification.

## Operator checklist: authorized isolated DEV/BETA rehearsal ONLY
**No commands in this section were run during this PR. Do not point at the live family service without separate Architecture approval.**

1. Pin source SHA and compare PR scope with main/PR39. Before any actual BETA deployment, obtain explicit change approval; inspect runtime `.env`, Docker volume inventory, host and TLS **without logging secrets**. Verify archival backend path and mounted named volume via read-only `docker compose config` / `docker inspect`, and verify directory ownership/permissions with a disposable test deployment. Backend runs as UID 10001; ensure this UID can create directories/files inside the mounted volume. A new empty root-owned Docker volume **may not be writable** by UID 10001 without one-time controlled ownership initialization. Do not silently run backend as root.
2. Independently snapshot both **PostgreSQL data** (existing `scripts/beta/backup.sh` produces PostgreSQL custom dump and checks TOC) **and encrypted archive volume**. Ensure archive ciphertext, matching registry metadata, SHA256 manifests, and private archive decryption keys are protected; never write decryption keys into the backup volume. Store recoverable copies off-host with restricted access and retention controls. Storage volume and `beta_postgres` have separate lifecycle, so snapshots/restore points must be synchronized.
3. In a truly isolated **disposable** environment with separate project name/volumes, run a `pg_restore` into a fresh matching PostgreSQL server after confirming actual schema/migration compatibility. Verify table counts, `flyway_schema_history`, application schema validation, registry-to-ciphertext SHA256 and **offline** FPB1 integrity inspection using disposable credentials/keys. `pg_restore --list` alone is only archive TOC parsing, not recoverability. If archive storage read fails, preserve original evidence; do not purge or overwrite.
4. Keep **PostgreSQL version distinct**: current beta image is **postgres:17-alpine**, while integration Testcontainers tests are PostgreSQL 18. Run V53 Flyway migration preflight on an authorized disposable PG17 clone and verify existing original-owner/permit/receipt constraints; do not infer PG17 compatibility from PG18 CI.
5. Recovery after failed release must account for **forward-only Flyway**; swapping an older app binary may be incompatible with already migrated DB. First freeze writes in authorized maintenance window, preserve immutable backup/evidence and logs, restore DB **and encrypted archive volume** to a mutually consistent point in disposable rehearsal, then use a release-approved recovery plan. No auto rollback, `git reset --hard` or production DB restore in this PR.
6. **Destructive pitfall:** `docker compose down -v` removes named volumes, including `beta_postgres` and `beta_backup_artifacts` — never use it as a harmless restart. `docker volume rm` and host cleanup are likewise prohibited without separate explicit approval and validated off-host copies. Physical retention purge remains disabled.
7. Android is separate: debug SDK36 CI PASS is not a release-signed APK. Before any real publication, verify source SHA/tag, signing-certificate fingerprint, monotonic `versionCode`, SHA256 APK manifest, isolated install/upgrade and device/Google login smoke under approved disposable account.

## Hard stop criteria
BETA HOLD; `restorable=false`; `replaySupported=false`; Reset Execute HTTP423; no public executable RESTORE and no physical purge. Authorized Chromium WEB-01/WEB-02 role/account-switch acceptance, cryptographic Google JWK/aud/azp negative verification, populated unsupported source category/TTL boundaries, PG17 V53 preflight, real restore rehearsal, and signed Android operator testing stay **UNVERIFIED** until their owning QA/operator stream supplies actual evidence.

**After independent review:** integrate only via Architecture's conflict-aware release decision; perform one exact final SHA validation if checked-in code or PR39 test-merge changes. This isolated config PR does **not** merge PR39 or authorize deployment.
