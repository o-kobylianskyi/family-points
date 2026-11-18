# Backend A B-A-04e — exact-SHA full CI / QA intake

**2026-11-18 · TEST ONLY · no deployment.** This commit includes the literal **[full-ci]** trigger to execute all four required components on one SHA.

Verify actual Java 21 `mvnw clean verify` execution with PostgreSQL 18 Testcontainers; verify Kotlin/Android debug+JVM, Web regression/lint/build, Compose and release safety. No skipped required steps. Capture exact SHA, run id, Surefire test counts, and explicit failures/errors/skips.

New tests to inspect:
- `DisposablePortableMemberReferenceClosureTest`: unsupported member types, duplicate role code, source roster bounds, fractional numeric and BigInteger/BigDecimal identity aliases, historical member-exception-period FK.
- `DisposablePortableMemberExceptionClosurePostgresTest`: two real synthetic families, source-local member exception periods and rejection of accidental neighbor FK leakage.
- Retained regression: `DisposablePortableMemberSourceCatalogPostgresTest`, `DisposablePortableMemberEdgeSchemaPostgresTest`, `DisposablePortableMemberFkInventoryPostgresTest`, `WorkspaceBackupRetentionRollbackGapPostgresTest`.

Source changes remain limited to `src/test/java` and documentation. No production exporter/inspector format change; original role/member vNext requires named Architecture A4-01…09 field/format/IMP decision plus Backend B/C/D review. No live family, no accounts, no automatic retention deletion. Reset Execute HTTP 423, Restore `restorable=false`, `replaySupported=false`, **BETA HOLD**.

Source QA: `docs/coordination/requests/B-A-04E_STRICT_SOURCE_ID_AND_EXCEPTION_CLOSURE_QA_20261118.md`.
