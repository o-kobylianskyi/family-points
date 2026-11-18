# B-A-03c — PostgreSQL 18 concurrent source snapshot evidence packet

Date: 2026-11-18. Track: Backend A. Scope: source archive capture only.

## Source
- `src/test/java/com/olehkobylianskyi/familypoints/WorkspacePortableBackupConcurrentSnapshotPostgresTest.java` commit `344d235`.
- Testcontainers PostgreSQL 18 and Flyway; two distinct JDBC connections. A repeatable-read source transaction locks the family row and pins the original member-group snapshot; independent writer deliberately bypasses the family fence and commits a row rename. Exporter is invoked while the source transaction remains open.
- Invariants asserted: source digest matches encrypted archive source digest; archive contains original 'Before', committed DB contains 'After', a different family remains unchanged, original counts match and offline inspector still says restorable=false. Independent writer must actually complete (15-second bounded get).
- This demonstrates real PostgreSQL coherent MVCC capture at REPEATABLE_READ for this source call pattern, **not** production readiness of FULL/SELECTIVE restore. The test uses TransactionTemplate to pin the true physical isolation. Earlier B-A-03b unit checks guard coordinator weak/unknown isolation.

## Exact verification
`./mvnw -Dtest=WorkspacePortableBackupConcurrentSnapshotPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspacePortableBackupExporterTransactionContractTest,WorkspacePortableBackupExporterPostgresTest test`

CI acceptance must reference exact SHA and JUnit XML or concrete Actions logs; a queued/in-progress run is not PASS. The concurrency test is newly authored and **NOT_RUN locally** in this source-only GitHub connector session.

## Outstanding work
- The test does not yet assert Spring AOP/proxy entry of `registerAndStage`. Verify through real Spring transaction interceptor that coordinator starts REPEATABLE_READ and that an outer READ_COMMITTED caller fails before staging. Guard rejects Spring unknown or weak isolation.
- Direct invocation of `WorkspaceBackupArtifactStagingService.stage` can bypass coordinator. Its production callsites require review. No user-facing stage endpoint is introduced here.
- FAMILY_MEMBERS vNext contract remains Architecture/Backend B ACK pending; don't add identifiers or credentials speculatively.
- Source snapshot `digest` includes more than named archive categories. Consumer restore source category closure not yet solved.
- Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423 and BETA HOLD unchanged.

Next Backend A package B-A-03d: Spring-proxy entrypoint integration and direct-staging guard coverage; only after ACK B-A-04 portable member source fields.
