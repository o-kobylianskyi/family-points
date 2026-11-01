# ADR-FAMILY-RESET-002 — BKL-002 consolidated architecture decision (2026-11-02)

**Status: ACCEPTED AS IMPLEMENTATION CONTRACT, with explicit fail-closed restrictions.** This decision closes product/architecture ambiguity for a bounded safe implementation; it does **NOT** assert all existing 70 tables/156 FK links are implemented, does not unlock `reset/execute`, does not permit deletion of real data, and does not mean Release/QA signed off. **Supersedes obsolete postponement** in ADR-FAMILY-RESET-001 and any external backup storage assumption.

**Authority:** user explicitly approved reset defaults, local 30-day backup retention, encrypted file export, FULL/SELECTIVE file import, and cancellation of external storage; remaining rules below are safety-first architecture defaults, requiring automated verification. See `docs/coordination/requests/BKL002_APPROVED_FILE_BACKUP_RESTORE_20261101.md`.

## A. Scope, actors and ownership
- Workspace/family is tenant boundary; owner/admin access, workspace ID, global UserAccount/auth, other family memberships, global template/catalog and immutable security audit are **ALWAYS retained**. No global account password changes during reset or restore.
- Add an explicit durable `family_owner_account_id` (or equivalent immutable owner reference) plus audited transfer, restricted to authorized full-family owner operation. For new families bind authenticated creator **in same transaction** as workspace creation. For legacy workspaces, **no heuristic owner assignment** (role/creation date/lowest ID). Until an owner has been enrolled by a separately authenticated, auditable procedure, destructive operations are **OWNER_NOT_ESTABLISHED** and locked.
- **Only verified family owner** may execute FULL/SELECTIVE reset or FULL/SELECTIVE restore. Workspace manager may preview/list/export own family's archives subject to explicit granular permission; delegated group admins and children never execute reset/restore. Role name `ADMIN` alone is not ownership proof. Owner delegation for destructive operations is **not supported in MVP**; owner transfer remains a separate audited flow.
- Global account shared by two families: credentials and other-family memberships NEVER modified; recheck actor account, workspace, ownership and current epoch inside same transaction; lock concurrent owner change/membership-linking as appropriate.

## B. Reset categories and approved product defaults
- SELECTIVE initially suggests deletion of task definitions/schedules/executions, reward definitions/requests/purchases, points/reputation and eligible economic history, groups/task folders, private templates, family-owned media. Members other than owner, currencies and workspace settings are **initially retained**; no action is executed until explicit selection and confirmation.
- FULL resets every **supported deletable family-local category** to fresh-start state; reset settings/currencies to supported defaults; optionally unlink non-owner memberships only after closure validation; never delete workspace identity, owner, owner management capability, global auth, unrelated families, global catalog, immutable audit.
- `selection=[]` → 400 `EMPTY_RESET_SELECTION`; both FULL and SELECTIVE require exact preview. FULL with an unsupported category must return `RESET_CATEGORY_UNSUPPORTED` and leave everything unchanged, not quietly perform a partial FULL.
- **No implicit co-deletion**. If requested selection conflicts with retained references, emit `RESET_DEPENDENCY_BLOCKED` with entity classes/counts/required alternatives. Do not expose dependent private row contents. Source graph/snapshot fingerprint and dataEpoch must remain current at execution.
- Device Owner/OS controls, external mails, user auth, account-level secrets, system backups **never silently reset**. They need separate explicit workflows.

## C. Financial and historical integrity
- Immutable security/audit entries survive all modes. Never silently change ledger entries or edit history to make balance equal zero.
- Two distinct policies:
  1. `ZERO_BALANCE_KEEP_HISTORY`: append scoped, immutable reset-adjustment ledger entries for each existing member/group/currency/reputation with exact inverse derived balance; preserve historical transaction IDs and referenced definitions.
  2. `CLEAR_ECONOMIC_HISTORY`: supported **only** when every related earning, task execution, purchase, grant, refund, entitlement, group ledger and foreign-key/source link is included in an approved dependency-closed deletion set. No automatic refunds or deletion of fulfilled time entitlements.
- Any unrecognized/unclear historical source or active reward entitlement => `RESET_FINANCIAL_POLICY_BLOCKED`. MVP may support only ZERO_BALANCE_KEEP_HISTORY for those workspaces. This is intentional bounded support, not a silent overwrite.
- If FULL semantics cannot safely clear a requested finance/history category, FULL is blocked with a precise reason; does not degrade to selective unnoticed.

## D. Restore semantics and conflicts
- Restore must be tenant-scoped, encrypted file, versioned schema and manifest, strict authenticated archive integrity; deny other-workspace archive by default. No global password/membership ownership import.
- **SELECTIVE** touches precisely selected categories; unselected data unchanged. **FULL** restores every supported family-local archive category to a consistent state; both modes preserve protected identities/audit.
- Approved initial conflict policy: `REPLACE_SELECTED` for categories with a proven, dependency-closed mapping and atomic transactional replacement, **not** generic merge. Before destructive replacement create a new VERIFIED protective snapshot of the current target family. If existing linked retained data would break, `RESTORE_CONFLICT` with no changes. No implicit merge; `MERGE` explicitly **unsupported in MVP** until category-specific rules/tests accepted.
- Fresh UUID/PK remap for family-local groups, tasks, rewards/media where necessary; archive IDs are logical source IDs not authority to overwrite target/global IDs. Preserve foreign family and owner references; reconcile media polymorphic targets only when resolvable in selected restored or retained target scope.
- Preview MUST report category counts, conflicts, selected replacement, retained data, FK graph, schema/epoch, reason codes. Apply checks owner, valid fresh proof, protected backup, transaction lock, unchanged preview fingerprint/epoch, idempotency UUID. Commit records+audit+epoch+onboarding marker together or roll back. Lost response uses GET receipt; same UUID does not reapply.

## E. Confirmation and authentication
- Require exact **family display name** typed at confirmation plus explicit selection impact acknowledgement; the server checks normalization on preview/current name and invalidates on rename. A fixed RESET <id> placeholder is insufficient.
- Fresh proof max 5 minutes, one use, bound to account, owner, family, preview ID/digest, category selection, operationId, epoch and action type (RESET vs RESTORE). Never send passwords in receipts or persist plaintext. Proof consumed only in successful atomic operation, or terminally invalidated under defined failure/retry semantics. CSRF/CORS/rate limiting/session and audit required.
- Local-password account: verify locally against current password. Google-only account: use **recent verified Google reauthentication** (OIDC token whose audience/nonce/sub/auth_time or equivalent provider guarantees are validated); **do not** regard an old access JWT or social-login presence as fresh reauth. If secure fresh provider proof not available, return `FRESH_REAUTH_UNAVAILABLE`; **never bypass**. A local password set via independent verified flow can enable password proof afterwards.
- All authorization checked again inside transaction; child/admin/non-owner cannot bypass via stale token or external member role.

## F. Backup, retention, file portability
- Immediately before destructive RESET or RESTORE, create and validate a **recoverable** snapshot of target family. If snapshot unavailable/corrupted/unrestorable/expired, fail closed and never mutate. `VERIFIED` is not merely metadata/encryption checksum: disposable replay/structural FK verification is required for supported category set. Include media bytes needed for restore.
- Server stores temporary encrypted backups locally for **30 days from successful snapshot creation**; secure permissions, quota/size limits, scheduled physical file+registry cleanup and audit, no offsite/cloud storage. Jobs must not delete an in-use active restore object.
- User may download a **portable .fpbackup file**. Downloaded copy is controlled by user and cannot be deleted or expired by the server's 30-day TTL; server expiry never promises deletion of an exported copy.
- **Portable key strategy for MVP:** use user-supplied export passphrase transformed with a memory-hard KDF (Argon2id preferred with versioned parameters, or reviewed supported equivalent) and unique per-file salt; derive AES-256 authenticated encryption key. Do not store passphrase or key inside archive; lost passphrase means file cannot be recovered. Provide separate server-managed encryption key for automatic temporary pre-reset snapshots; its recovery/rotation is a Release responsibility and must be tested before enabling Reset. No hardcoded key, no direct raw pg_dump for tenant import.
- Current 64 MiB codec limit is binding until bounded streaming/chunks supported; oversized archives fail with `BACKUP_SIZE_LIMIT`, not truncated. Malformed, duplicate UUID, inappropriate schema, unknown category, dangling polymorphic media or FK, zip-slip/path traversal and cross-family data must fail closed.

## G. State machines and rollout
- Backup: REQUESTED → BACKING_UP → VERIFIED / FAILED / EXPIRED, with durable verified physical artifact reference and expiry. Reset: PREVIEWED → BACKUP_VERIFIED → CHALLENGED → EXECUTING → COMMITTED or FAILED (no partial commit). Restore: UPLOADED_QUARANTINED → VALIDATED → PREVIEWED → BACKUP_VERIFIED → CHALLENGED → EXECUTING → COMMITTED or FAILED. Output has operationId, workspaceId, old/new dataEpoch, stable outcome/receipt.
- On successful reset with restartSetup, mark wizard DRAFT under same atomic commit and preserve family/owner; no auto-import/auto-activate templates. Android offline operations under old epoch held/quarantined, never blindly replayed.
- Existing read-only endpoints stay enabled under current permissions. `reset/execute` remains HTTP 423 until Backend implements approved supported closure and passes synthetic disposable PostgreSQL plus consumer/Release gates. File restore execute likewise disabled until actual all-category supported apply, owner proof and recovery tested.
- Release: no real family reset/import, no PROD/BETA deployment just because this ADR is accepted. QA: validate FULL/SELECTIVE, economic integrity, owner/shared accounts, Google-only negative and positive proof, idempotency/lost response, failure rollback, portable export/import, wrong key/tamper, tenant isolation, 30-day physical cleanup, client epoch.

## H. Explicit unsupported cases — not architecture blockers for bounded MVP
1. Generic category MERGE in file import; return RESTORE_MERGE_UNSUPPORTED.
2. Destructive delegation to ordinary family admins without explicit owner binding.
3. Restore from a file for a different family ID (family migration).
4. Automatic refund/reversal of already fulfilled reward entitlements.
5. Clear immutable security audit/global identity or other-family data.
6. Device Owner / OS policy wipe.
7. Schema changes without reviewed versioned migration converter; return RESTORE_SCHEMA_UNSUPPORTED.

**Implementation handoff:** Backend owns per-table/per-FK closure mapping from `BKL002_RETENTION_FK_INVENTORY_20261031.md`, tests and exact DTO; Architecture rejects unsupported combinations as above, not withholding general policy. Web/Android consume only tested source-backed HTTP contract. QA determines acceptance separately.

**Unblocking meaning:** Architecture policy chosen for bounded safe development; `BKL-002 Development` = IN PROGRESS, `Reset Execute` = BLOCKED until implementation/test gates, `Release Gate` = BLOCKED until integration/green CI. No irreversible operation authorized.
