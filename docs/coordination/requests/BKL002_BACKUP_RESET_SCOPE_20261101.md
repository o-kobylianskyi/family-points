# BKL-002 / BKL-BACKUP-001 — Family reset modes, 30-day protective backup, independent restore

**Date:** 2026-11-01. **Priority:** P0 for pre-reset backup and full/selective reset; P1 separate backup browsing and restore, gated by security/QA. **Status:** user-approved product scope, architecture/security review and implementation PENDING. **Related:** Issue #15, IMP-0009, ADR-FAMILY-RESET-001, XTR-20261031-B-01.
**No actual reset, backup of user data, server changes or deployment is authorized by this document.**

## User decision
1. Provide **FULL** family reset and **PARTIAL/SELECTIVE** reset where admin explicitly chooses what to erase and what to retain.
2. Before destructive reset automatically create and validate a recoverable backup retained **30 days**; if backup cannot be created/validated, **fail closed**.
3. Add separately accessible Family Backup & Restore management.

## Reset semantics / UX
- FULL means all **supported deletable data inside selected workspace**; never erase global identity, guaranteed owner/admin access, unrelated workspaces, globally bundled catalog, system/security audit or unrelated deployments. Make retained exceptions explicit, not hidden.
- PARTIAL has an explicit category selection matrix: tasks/definitions/executions, rewards/requests/purchases, points/current balances/history, reputation, people groups, task folders, private templates, media, optional non-owner workspace members/currencies, subject to supported dependency closure.
- No implicit cascade or silent clearing extra categories. If selected combination breaks reference/economic invariants, return precise blocker and suggested required additional choice; never secretly co-clear.
- Distinguish RESET_BALANCE_WITH_AUDITED_ADJUSTMENT from REMOVE_HISTORY; audit retained globally. Preserve immutable security audit and policies.
- Preview lists per-category record counts, what is retained, blocked dependencies, backup size/availability and final post-reset onboarding plan. Never preselect destructive ALL in partial mode. FULL also requires typed name, fresh reauth and explicit confirmation.
- Old-family/account, shared identity, legal/retention and Google-only reauth policies remain Architecture P0 blockers. Execute retains HTTP 423 until approved.

## Protective snapshot design
- Existing scripts/beta/backup.sh is a **whole PostgreSQL cluster-application backup via pg_dump** on Docker host. This is distinct from a family-scoped portable backup. Do not allow family admin to restore the whole DB.
- Architecture+Backend+Release must choose whether pre-reset guarantee is a validated encrypted **tenant-scoped export** or a full-DB infrastructure snapshot with controlled tenant reconstruction. Claim tenant restore only after a tested tenant-restore implementation.
- Every reset binds a UNIQUE snapshotId to workspaceId, requested selection, operationId, family dataEpoch, manifest/checksum, schema/application version, media attachments, creation/expiry timestamp and backup verification status. Prevent cross-tenant reads; no secrets/credentials in tenant export.
- Durable state machine: REQUESTED -> BACKING_UP -> VERIFIED -> AUTHORIZED_FOR_RESET -> RESET_COMPLETED, or BLOCKED/FAILED. Never destructive writes before VERIFIED; stale family epoch / changed source snapshot forces new preview and backup, not accidental deletion.
- Secure encrypted backup off the primary database/host, least-privilege service keys, no public URLs, short-lived download authorization, quotas and size limits; 30-day retention from successful creation; lifecycle expiration and deletion logs. Keep infrastructure disaster-recovery backups independent.
- Verification should include manifest integrity and read/restore rehearsal on isolated disposable fixtures before claiming restorable; checksum alone is not sufficient proof of semantic restore.
- Handle retention exceptions mandated by law/security transparently; backup retention duration and ability to restore are separate concepts.

## Independent Backup / Restore (separate feature)
- Family admin may create a manual family snapshot, list snapshots with creation/expiry/schema/status, download authorized export where enabled, and request a scoped restore.
- Restore is not a naive table import: preview conflicts, version compatibility, member/account linkage, immutable record references, currencies, media, idempotency keys, audit, epoch/offline invalidation. Restore transaction/saga must be atomic or compensating with durable outcome; preserve owner and other families.
- Require fresh owner authorization/confirmation and optionally protective current-state snapshot before restore; deny overwrite on unsafe unresolved references. Separate system-level Postgres disaster recovery from tenant UI restore.
- Optional policies for manual snapshot retention and post-expiry restoration need approval; don't advertise guaranteed undo without verified recovery tests.

## Ownership and phased batches
**Architecture P0:** Update ADR-FAMILY-RESET-001 and XTR-20261031-B-01 with approved FULL/PARTIAL allowed category matrix, retention semantics, snapshot guarantee/storage, owner/auth/finance policy, and minimum restore capability. New feature cannot be smuggled in as approved deletion of real family.
**Backend P0:** Implement preview/plan, server-owned snapshot binding + validation, encrypted scoped export or separately approved recoverable checkpoint, FULL/PARTIAL execution with dependency safety, audited atomic deletion and epoch+onboarding; stable idempotent recovery. Keep execute disabled until architectural/security gates.
**Release/DevOps P0:** Design off-host encryption, storage/30-day TTL cleanup, key handling, quotas, non-prod isolated restore validation, backup health/alerting and deployment safety. Keep whole-DB beta backups separately.
**Web P0:** Settings > Family data > Reset: FULL vs SELECTIVE, choose retain/delete, impact preview, backup VERIFIED, explicit reauth+typed confirmation; don't enable execute before backend gate. Separate Backup & Restore panel later.
**Android P1:** Support post-reset epoch/queue/cache recovery; optionally read-only backup status, no destructive offline action. No Android reset execution until supported contract.
**QA P0:** Synthetic disposable PostgreSQL fixtures: full vs partial with cross-FK/ledger constraints, backup fail-closed, checksum vs actual restore, timeout/duplicate click, tenant A/B and shared-account, expiry/retention, stale epoch, failed reset+snapshot survival. Browser/manual live tests deferred by user.
**Designer P1:** Clear labels for FULL vs SELECTIVE, retained vs removed, backup guarantee and 30-day countdown; four locales, parent-only danger-zone UX.

## Hard gates
- No family record deletion unless usable backup VERIFIED, authorization and snapshot fingerprint current, approved table/semantic dependency plan, transaction safety and idempotent audit are all satisfied.
- If backup cannot be demonstrably restored, describe it as archival export, **not** user-available rollback.
- Never restore entire shared PostgreSQL dump as though it were a single-family backup.
- Existing family data is not modified by preparing this feature; deployment and user-triggered destruction require separate explicit decisions.
