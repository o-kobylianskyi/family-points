# Designer → Web / Android / QA / Architecture — BKL-002 backup, reset & restore UX v1
Date: 2026-11-01. Priority: P0 usability/safety specification. **DESIGN SPEC, not approval to execute.**

## Decisions and scope
Source of truth: `BKL002_APPROVED_FILE_BACKUP_RESTORE_20261101.md` overrides earlier off-host storage assumptions. There is **no external encrypted object storage**. Server-local protected backups expire 30 days after creation; downloaded encrypted `.fpbackup` files are controlled by the user, and FamilyPoints **cannot** revoke or delete them when local TTL expires. Only the selected family may be affected. Other families, global identity, owner access, global catalog and immutable security audit survive. All write buttons remain DISABLED until Backend/Architecture's versioned contracts, tested restore executor and rollout gate exist. Current reset `/execute` is HTTP 423.

## IA: Settings → Family data
Three visibly separate top-level panels with distinct headings and intent; do not blend Reset/Restore into one button:
1. **Backup / Резервні копії** (neutral): family-scoped snapshot list: created, expires, status, size, data scope, application/schema version. Actions Create, Download `.fpbackup`, Inspect. Unimplemented file operations disabled with a reason, never mock success.
2. **Restore / Відновити** (caution): upload encrypted file → integrity/schema/family check → FULL / SELECTIVE → impact preview and replace/merge/blocked policy → explicit confirmation + fresh reauth → protective current-state backup → durable receipt and re-entry. Do not promise a restored family on `VERIFIED` archive metadata alone.
3. **Reset / Очистити дані** (danger): FULL vs SELECTIVE → impact preview → VERIFIED recoverable pre-reset family backup → typed family name + fresh reauth → explicit execution and immutable receipt. For FULL, show retained identity/audit exceptions in same viewport as deleted family-local records.

## Category controls and default matrix
Each row must independently show category, affected record count (if trusted), proposed action, retained exceptions, dependencies and BLOCKED explanation. SELECTIVE starts from approved suggestions, but **does not execute automatically** and cannot silently add dependent categories. FULL selects all **supported** deletable categories only. Explicit row statuses `DELETE / RETAIN / RESET_DEFAULT / RESTORE / UNCHANGED / BLOCKED` must not be reduced to a colored checkbox.

| Category | SELECTIVE reset initial suggestion | FULL reset | SELECTIVE restore |
| --- | --- | --- | --- |
| Tasks, schedules, runs, task history | DELETE | DELETE | User-select RESTORE or UNCHANGED |
| Rewards, requests, purchases | DELETE | DELETE | User-select RESTORE or UNCHANGED |
| Points, reputation, balances, relevant history | DELETE only if dependency/ledger policy passes | DELETE only if policy passes | Explicit conflict/reconciliation strategy; BLOCKED when undecided |
| People groups and task folders | DELETE | DELETE | Explicit FK/relink plan; BLOCKED if unresolved |
| Private templates | DELETE | DELETE | Preserve customizations on unselected categories |
| Family-uploaded media | DELETE only if safe | DELETE only if safe | Reattach bytes/references only when verified |
| Non-owner participants | RETAIN | DELETE where safe; owner retained | Explicit mapping; never overwrite global accounts |
| Workspace currencies/config | RETAIN | RESET_DEFAULT/DELETE where safe | Apply only with approved currency mapping |
| Family settings | RETAIN | RESET_DEFAULT where safe | User-select where supported |
| Global auth, owner access, other families, immutable audit, global catalog | ALWAYS RETAIN | ALWAYS RETAIN | NEVER imported/overwritten |

For SELECTIVE restore use a **fresh explicit selection** from compatible archive categories. Unselected categories stay unchanged. If Backend cannot choose replace-vs-merge safely, mark BLOCKED and show human action, never silently overwrite. “Restore full family” refers only to supported family-local content, not entire DB/credentials.

## Preview and confirmation interaction
- Step A: choose family and mode. Show exact active workspace name/id, authorized manager role, source archive date/epoch (restore), family current epoch and unverified info placeholders; changing workspace invalidates all prior previews and confirmations.
- Step B: editable category matrix; label `Suggested` is not `Selected and applied`. Explain dependency closure as an explicit suggestion the user must confirm; no auto scope expansion.
- Step C: read-only impact plan: `Will delete`, `Will retain`, `Will restore/replace`, `Will remain unchanged`, `Blocked`; include unknown counts honestly and independent immutable history exceptions.
- Step D: safety gate shows VERIFIED **recoverable** protective snapshot, creation and 30-day expiry timestamps; verify backend claims restore feasibility, not just checksum. For restore, show **protective snapshot of current target state** too.
- Step E: high-friction explicit confirmation with family-name typing and fresh auth, server-bound operation UUID, operation fingerprint/revision/dataEpoch. Never create a new UUID on uncertain response; provide `Check operation status`.
- Step F: stable operation receipt with `IN_PROGRESS / COMPLETED / FAILED / BLOCKED / EXPIRED` and recoverability/backups. A browser reload/network timeout must not imply failure or trigger a duplicate operation.
- On server 423: show “Операція поки недоступна: перевірки безпеки не завершено” and no active destructive CTA. On 401/403: access denial; 409: stale plan and refresh; 422: explicit dependency reason; 429: auth rate-limit wait; 5xx/timeout: unknown outcome until receipt lookup. Translate by stable codes once Backend publishes them, never infer by parsing prose.
- Block archive with wrong family, unknown schema, tampered bytes, wrong key, expired local copy or unavailable encryption key; distinguish *local copy expired* from user's retained downloaded file.

## Copy (UA / RU / DE / EN)
| Intent | UA | RU | DE | EN |
|---|---|---|---|---|
| Backup | Резервна копія сім’ї | Резервная копия семьи | Familiensicherung | Family backup |
| Reset FULL | Повністю очистити дані сім’ї | Полностью очистить данные семьи | Familiendaten vollständig zurücksetzen | Reset all supported family data |
| Reset SELECTIVE | Вибірково очистити дані | Выборочно очистить данные | Daten gezielt zurücksetzen | Reset selected data |
| Restore FULL | Повністю відновити дані сім’ї | Полностью восстановить данные семьи | Familiendaten vollständig wiederherstellen | Restore supported family data |
| Restore SELECTIVE | Вибірково відновити дані | Выборочно восстановить данные | Ausgewählte Daten wiederherstellen | Restore selected data |
| Keep | Залишити без змін | Оставить без изменений | Unverändert lassen | Keep unchanged |
| Blocked | Заблоковано через залежності | Заблокировано из-за зависимостей | Wegen Abhängigkeiten blockiert | Blocked by dependencies |
| Export warning | Завантажений файл зберігається у вас і не видаляється автоматично через 30 днів | Скачанный файл остаётся у вас и не удаляется автоматически через 30 дней | Heruntergeladene Dateien bleiben bei Ihnen und werden nach 30 Tagen nicht automatisch gelöscht | Downloaded files remain yours and are not automatically deleted after 30 days |

Do not use reassurance like “Undo guaranteed”; say recovery **only after tested restore**. Always make warnings understandable without color alone; support focus, keyboard, mobile, dark/light.

## Web ownership
Existing `frontend/src/api/backupApi.js`, `resetApi.js` and `FamilyResetReadiness.jsx` are the integration baseline. Keep P0 UI in disabled/read-only mode until authoritative Backend HTTP file and execute routes and security proof are approved. Use same category wording and mode semantics in preview, receipt and Settings. A disabled action should explain why, not disappear. Do not invent URLs, auth proofs, payload shapes or status fields.

## Android ownership
Read-only backup status is P1. No background upload, destructive/reset/restore queue or offline replay. After a successful authorized reset/restore receipt, honor new dataEpoch; quarantine stale offline mutations and clear scoped UI/cache by account + workspace + generation.

## QA acceptance
- Four locales × light/dark × small screens; owner/manager vs child access.
- FULL/SELECTIVE matrix, zero selection, incompatible dependencies BLOCKED, unknown counts, retained owner/global account/audit.
- Corrupted/foreign/wrong-key/truncated/oversized .fpbackup, unsupported schema, key loss, local TTL versus downloaded file.
- Current-family A, other-family B, shared global account; never leak/alter B. Timeout→receipt recovery using SAME UUID and original immutable payload.
- Disposable-only end-to-end: protected export→decrypt→stage→FULL/SELECTIVE restore→relation/media/ledger compare→epoch+offline invalidation. **Not proven yet**.
- No real family deletion, no Android device-owner removal, no production deployment under this specification.

## Architecture decisions still required
Per-category replace-vs-merge, finance/audit retention and ledger compensation, identity/owner/shared account, FK/media remapping, key/passphrase portability, fresh Google reauth, verified recoverability rules, exact versioned DTO/errors/receipt endpoints. Designer sign-off is **copy and workflow only**, not security or delete authority.
