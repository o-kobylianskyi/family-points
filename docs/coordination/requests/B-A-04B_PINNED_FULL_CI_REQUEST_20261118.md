# Backend A B-A-04b — exact-SHA full CI and QA acceptance request

**2026-11-18 — TEST-ONLY source graph.** Request actual four-component non-skipped CI at this commit and independent QA intake. **No deployment.**

## Coverage
- `DisposablePortableMemberSourceCatalogPostgresTest` — full REPEATABLE_READ, original family/role/member source IDs, tombstone timestamp fidelity, two-family isolation, no names or global auth exposure.
- `DisposablePortableMemberReferenceClosureTest` — member FK graph positive/negative, original executor vs assignee, explicit null vs omitted field, foreign/duplicate source identities, processed-by optional FK, unreviewed source categories, row limits.
- `DisposablePortableMemberEdgeSchemaPostgresTest` — PostgreSQL 18 + Flyway source columns actually exist and maintain expected nullable/required FK semantics.
- `WorkspaceBackupRetentionRollbackGapPostgresTest` — physical unlink before forced SQL rollback, read-only absent-file detection and unlink error preventing EXPIRED.
- `WorkspaceBackupReconciliationRacePostgresTest` and `WorkspaceBackupRegistryPresenceRacePostgresTest` — concurrent file/registry and SHA changes cannot authorize file deletion.

## CI acceptance, no skipped steps
```bash
./mvnw -B -ntp clean verify
```
Require Java/Maven + PG18 tests, Android Gradle unit/build, Web regression/lint/build and Compose safety all executed at same SHA. A green routing/scoped workflow with skipped Maven is **not** a passing backend test. Record exact run ID, SHA, jobs, Maven Surefire totals and exact class outcomes; route actual failures to Backend A for repair.

No production changes to `WorkspacePortableBackupExporter`, `ProtectiveBackupImportInspector`, manifest format or restore gate. A4-01…09 remains Architecture schema/IMP and consumer approval dependent; Backend B only conditionally accepted logical historical member source.

`restorable=false`; `replaySupported=false`; Reset Execute HTTP423; BETA HOLD. No live families.
