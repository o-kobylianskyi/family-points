# B-A-04d — pinned full four-component CI gate

Date: 2026-11-18. Backend A QA release handoff. NO deployment.

Source packet: `docs/coordination/requests/B-A-04D_MVCC_AND_CI_QA_20261118.md`.
This commit intentionally carries `[full-ci]` to force Java/Maven PG18, Android, Web and Compose at the same source SHA. The prior B-A-04c full run `44281984930` had Java still in progress at the latest inspection; do not label it passing until non-skipped steps and reports are available.

Required targeted test classes: `DisposablePortableMemberSourceCatalogPostgresTest`, `DisposablePortableMemberReferenceClosureTest`, `DisposablePortableMemberEdgeSchemaPostgresTest`, `DisposablePortableMemberFkInventoryPostgresTest`, `WorkspaceBackupRetentionRollbackGapPostgresTest`. New MVCC concurrent insert test must show same REPEATABLE_READ snapshot and later visibility on new transaction.

Capture exact SHA, actual non-skipped Maven run and Surefire test counts, Android/Web/Compose outcomes. Failure means B-A-04d NOT ACCEPTED. No production archive format, import/restore execution, owner permits or destructive retention changes. Architecture A4-01…09 approval required before vNext.
