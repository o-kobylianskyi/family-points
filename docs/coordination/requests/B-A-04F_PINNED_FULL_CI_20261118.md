# B-A-04f pinned full CI source-graph acceptance — 2026-11-18

This is a non-deployment `[full-ci]` trigger: run **all four component jobs** on this exact commit after B-A-04f test-only financial/group parent closure.

Required backend suites:
```bash
./mvnw -B -ntp clean verify
```
Confirm targeted `DisposablePortableFinancialSourceClosureTest`, `DisposablePortableFinancialEdgeSchemaPostgresTest`, `DisposablePortableMemberSourceCatalogPostgresTest`, `DisposablePortableMemberExceptionClosurePostgresTest`, `DisposablePortableMemberReferenceClosureTest`, as well as historical B-A-03k retention rollback and race tests. Report exact run ID/SHA, actual non-skipped Maven test step, Surefire counts, errors/failures/skips and four-job Android/Web/Compose results. Fail closed on a source-schema drift test failure; do not weaken a check to make CI green without evaluating the actual migration.

This package **does not** establish financial semantic closure. Backend D must review polymorphic source_type/source_id, REFUND, immutable task/reward prices, group/reputation semantics, and no implicit refunds. Backend B must ACK vNext consumed member, currencies, original assignment and selected dependency graph. Architecture A4-01…09/IMP remains open before changing runtime exporter or inspector. Owner proof from Backend C remains separate.

No real family mutation, no automatic file purge, no production Restore or Reset Execute, HTTP423 / BETA HOLD.
