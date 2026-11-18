# B-A-03a — source snapshot consistency hardening / QA handoff

Date: 2026-11-18. Owner: Backend A. Scope: **source exporter only**, independent of unapproved FAMILY_MEMBERS archive wire-format changes.

## Source and change
- Verified `WorkspacePortableBackupExporter.export`: reads first source digest; separately reads scoped rows and checks per-category row count; recomputes digest; seals archive. `WorkspaceResetSnapshotService.digest` locks `families.data_epoch` with `FOR UPDATE` and hashes relevant family-scoped FK graph.
- At PostgreSQL READ COMMITTED, those successive queries can see different statement snapshots despite `@Transactional` and the family row lock; the row lock serializes cooperative writers, but cannot assert every legacy/direct SQL writer takes that lock. This is a **snapshot consistency issue**, not authorization of Restore.
- Commit `a9c9b667`: exporter declares `@Transactional(isolation = Isolation.REPEATABLE_READ)`, preserving REQUIRED propagation. This provides a stable PostgreSQL MVCC view **when a Spring proxy begins a new exporter transaction**.
- Commit `b49191ed`: `WorkspacePortableBackupExporterTransactionContractTest` asserts declared isolation and REQUIRED propagation. Source-authenticated encryption, manifest V50, digest format and scope unchanged. No runtime import/restore/execute change.
- **Important limitation:** annotation isolation is not applied when the exporter joins an existing READ_COMMITTED caller transaction. Existing `WorkspacePortableBackupExporterPostgresTest` instantiates exporter directly and calls it in a default `TransactionTemplate`, so this test does not exercise Spring proxy transaction isolation. This is not claimed fixed. Evaluate all actual callsites and either require/create a verified REPEATABLE_READ transaction boundary or fail closed on weaker transaction before marking P0 atomicity accepted.

## QA test request (disposable PostgreSQL 18 only)
1. `./mvnw -Dtest=WorkspacePortableBackupExporterTransactionContractTest test` (annotation contract).
2. `./mvnw -Dtest=WorkspacePortableBackupExporterPostgresTest test` (existing V50 source export compatibility).
3. Independent concurrency test still **required**: two PG connections, exporter inside actual Spring proxy transaction at REPEATABLE_READ, writer modifies an included member/ledger table without family lock while export is staged; archive must represent exactly one consistent snapshot or fail closed, never mixed rows. Also test a calling REQUIRED transaction at READ_COMMITTED: until addressed, P0 remains open.
4. Full Maven + Web + Android + Compose exact-SHA CI after integration; no gate may be treated green based only on a test source commit.

## Blocking producer↔consumer contract
- B-A-02 `FAMILY_MEMBERS` source proposal still requires Architecture and Backend B ACK. Do not insert speculative sensitive member/account data or change archive wire format.
- Other BKL-002 gates unchanged: `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD. No production export/download, no family reset.

## Next Backend A package
- **B-A-03b:** inventory exporter invocation and outer transaction isolation; implement fail-closed isolation guard or a vetted separate export entrypoint compatible with existing callers, plus independently orchestrated PG18 concurrency integration test.
- **B-A-04:** after architecture approval, implement versioned FAMILY_MEMBERS source catalog/digest/inspector and source-FK closure.
