# BKL002-QA-20261116-20 — Authenticated portable point exchange rate/request/ledger source graph

**Producer:** Backend P0. **Consumer:** Independent QA issue #15. **State:** READY_FOR_QA / NOT_RUN by Backend (source-only connector cannot execute Maven, JDK or Docker). Pin exact HEAD and do NOT use real family data.

## Implementation
- `WorkspacePortableBackupExporter` `efaa597`: `POINT_EXCHANGE_RATES` scoped by rate.family_id and `POINT_EXCHANGE_REQUESTS` scoped via request.member_id→family_members.family_id. Exporter blocks rate point types from foreign family, request exchange_rate/currency/reviewer foreign family, requests without `POINT_TYPES+POINT_EXCHANGE_RATES`, and `POINT_LEDGER` containing any source_type POINT_EXCHANGE without **all three** archive categories. Error codes `BACKUP_EXCHANGE_RATE_FOREIGN_CURRENCY`, `BACKUP_EXCHANGE_REQUEST_FOREIGN_RELATION`, `BACKUP_EXCHANGE_RATE_CURRENCY_CATEGORY_MISSING`, `BACKUP_EXCHANGE_REQUEST_PARENT_MISSING`, `BACKUP_EXCHANGE_LEDGER_GRAPH_INCOMPLETE`.
- Snapshot content digest `59eac35` and backup request whitelist `8d1e21b` now accept new categories under original family-epoch lock. No HTTP execute or VERIFIED promotion.
- `ProtectiveBackupImportInspector` `b99ca55` and `f15bb4e` validate signed rate/request and point types FK graph, direct rate family, valid nonzero from/to points, positive integer amounts, approval status. Request original snapshot (rate_from_amount, rate_to_amount) is validated by from_amount being exact multiple and to_amount matching multiplier; do **not** compare current mutable rate amount to old request snapshot. Optional processed_by_member_id is explicitly marked IDENTITY_REBIND_REQUIRED.
- Same encrypted archive contains `POINT_LEDGER` with `source_type=POINT_EXCHANGE`. Source_id must reference present exchange request, status APPROVED, **exactly two** matching transactions, same member, one SPEND of -from_amount in from_point_type and one EARN +to_amount in to_point_type. Other historical source_type blockers remain. `restorable=false` unconditional.
- Financial closure `1988200` inventories both new economic categories; count expectations updated to 14 `f049607` and `86206fa`. `replaySupported=false` remains mandatory.
- Existing independent PostgreSQL source auditor fixture extended `9fda117`: incomplete ledger-only selection rejected, full signed archive counts rate=1 request=1 point_types=2 point_ledger=2, inspector verifies complete pair, tampered ciphertext denied, previous source audit negative tests unchanged.
- New signed fixture `ProtectiveBackupExchangeSourceGraphTest` `a20f6f7`, rewritten to deterministic Jackson ObjectNode edits `b6fa9d9`, additional snapshot multiplier negatives `f7d1db3`. Tests valid rate+request+2-leg source graph, rate missing, wrong rate currency, wrong exchange amount, pending status, wrong member, missing or duplicate ledger legs, cross-family rate and original rate snapshot inconsistency.

## QA run
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=ProtectiveBackupExchangeSourceGraphTest,DisposableExchangeLedgerSourceAuditorPostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePointLedgerSourceClosurePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## Acceptance
1. EX-PORT-01 real PG18 exporter encodes all four relevant categories in signed V50 archive; member, rate, from/to currencies, historic request rate snapshots and ledger request source ID agree; no source mutation.
2. EX-PORT-02 POINT_LEDGER with existing POINT_EXCHANGE but missing request/rate/point-type category fails closed before usable artifact.
3. EX-PORT-03 foreign family exchange request/rate/reviewer/point type is rejected; signed cross-family rate in offline inspector throws SecurityException; archived request member remains IDENTITY_REBIND_REQUIRED until target proof.
4. EX-PORT-04 original request exchange ratio (not current mutable rate), approved status and exact 2-leg source history; missing/duplicate/mismatched/repriced history blocked; malformed/foreign/tampered ciphertext denied.
5. EX-PORT-05 report exact start+end HEAD SHA, PASS/FAIL/ERROR/SKIP, failure logs and new defect IDs to Issue #15. Earlier QA source reviews or old PASS are not current-SHA green; release requires full four-component non-skipped CI.
6. EX-PORT-06 `WorkspaceFinancialRestoreClosureService.replaySupported=false`, `ProtectiveBackupImportInspector.restorable=false`, protected family operation still HTTP423 and BETA HOLD. **No production native exchange replay.**

## Still blocked
- Need target member identity, point_type code and denomination policies, historic exchange request/rate reconciliation, financial ledger source_id ID mapping, immutable transaction order and balanced global source→target proof before native replay. REFUND source contract caller-dependent and remains unsupported. No durable protected backup VERIFIED / owner fresh reauth / production selective or full reset execute.
