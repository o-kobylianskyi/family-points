# Backend A — B-A-04e source-member closure, strict identity types and real PG18 evidence

**Date:** 2026-11-18 · **Scope:** TEST-ONLY original member source graph, no production portable format changes. **Status:** source committed; new CI pending.

## Verified previous baseline
- Full CI exact SHA `64f4cdea96b9df2e6e29006e229960c0a83e7f08`, Actions `44282865822` **SUCCESS** with actual Java21/Maven/PostgreSQL Testcontainers "Run all backend tests" SUCCESS; Java Surefire report artifact `14418511259` present. Android, Web, Compose and full-ci-attestation jobs each SUCCESS. This establishes only the old SHA, not this packet.
- This baseline supersedes the earlier RUNNING status recorded in B-A-04d. No product Restore acceptance or deployment inferred.

## B-A-04e code scope
1. Harden `DisposablePortableMemberReferenceClosure.verify` to accept only positive integral `Integer`/`Long` source IDs. Reject fractional floating-point, decimal aliases and overlong big integers that could be silently truncated by `Number.longValue()`. Do not use names/emails/role similarity as identity matching.
2. Independently reject duplicate source role codes, blank role codes, unsupported member types and >2000 source role/member rows in a captured catalog. Still require original workspace/role identity and unique original member ID.
3. Add `DisposablePortableMemberReferenceClosureTest` regressions: fractional/overflowed source ID alias, duplicate role code, unsupported member type, oversized roster and missing member-exception source parent.
4. Add `DisposablePortableMemberExceptionClosurePostgresTest`: disposable PostgreSQL 18 + real Flyway schema; two separate families, member exception periods in each, actual same-workspace scoped source SELECT and REPEATABLE_READ catalog; verified selected family resolves, but accidentally included neighboring member immediately fails `SOURCE_MEMBER_EDGE_UNRESOLVED`.
5. Existing PG18 `DisposablePortableMemberFkInventoryPostgresTest` enumerates actual member FK constraints and reports nonapproved sources; vNext **not** automatically allowed to export foreign tenant parents or global auth/account identity.

## Architecture gates / limitations
- The reviewed source FK matrix is partial. `MEMBER_EXCEPTION_PERIODS` is a newly identified source ancestry category and is NOT one of existing 23 legacy v1 portable category allowlist members. Its inclusion as a mandatory vNext source category needs A4 exact signed contract and Backend B/D consumer review.
- Await A4-01…09 named Architecture schemaVersion/role/member source categories, privacy/tombstone encoding, hidden FULL/SELECTIVE dependency, IMP and B/C/D field-level ACK.
- These changes are under `src/test/java` only; no production exporter, inspector, file encoding, owner binding, REST endpoint, SQL migration, autonomous cleanup, or Restore enablement.

## Independent QA matrix
Run `./mvnw -B -ntp clean verify` on exact new SHA with Docker/PostgreSQL18 and require actual Surefire counts (0 errors/failures, targeted test classes present). Prior positive and negative baseline must stay intact. Check `DisposablePortableMemberReferenceClosureTest`, `DisposablePortableMemberExceptionClosurePostgresTest`, `DisposablePortableMemberSourceCatalogPostgresTest`, `DisposablePortableMemberEdgeSchemaPostgresTest`, `DisposablePortableMemberFkInventoryPostgresTest`.

Enforce all four component jobs at the **same** SHA, no skipped Maven/Android/Web/Compose. Require independent QA acknowledgement before claiming acceptance.

**Next B-A-04f:** inspect exact-SHA failures and use discovered FK inventory to prepare D/B-reviewed category-source closure; no archive schema change until Architecture approves. BETA HOLD, HTTP423, `restorable=false`, `replaySupported=false`.
