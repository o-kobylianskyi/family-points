# B-A-04f — Economic and group source FK closure: isolated QA and contract request

Date: 2026-11-18. Owner: Backend A. **Source/test-only; no production archive change.**

## Prior verified CI
B-A-04e exact-source full gate `fc639206f8193a6f02c43ca7338f993107a9277e` Actions run `44286446835` was **IN_PROGRESS** at the time of B-A-04f pickup: Java/Testcontainers and Android ongoing, Web and Compose complete SUCCESS. Do not infer full PASS until the specific run and non-skipped Maven test reports finish.

## New independent package
- `DisposablePortableFinancialSourceClosure`: test-only reviewed source FK ancestry matrix. It demands source-family-owned, unique positive IDs for POINT_TYPES, MEMBER_GROUPS, REWARD_DEFINITIONS, TASK_DEFINITIONS and POINT_EXCHANGE_RATES. It verifies sources:
  - POINT_LEDGER → POINT_TYPES
  - REWARD_PURCHASES → REWARD_DEFINITIONS and POINT_TYPES
  - GROUP_POINT_LEDGER → MEMBER_GROUPS and POINT_TYPES
  - GROUP_MEMBERSHIPS → MEMBER_GROUPS
  - TASK_EXECUTIONS → TASK_DEFINITIONS and explicitly nullable reward/penalty POINT_TYPES
  - TASK_DEFINITIONS → explicitly nullable reward/penalty POINT_TYPES
  - REWARD_DEFINITIONS → POINT_TYPES
  - POINT_EXCHANGE_REQUESTS → POINT_EXCHANGE_RATES and from/to POINT_TYPES
  - POINT_EXCHANGE_RATES → from/to POINT_TYPES.
- Unreviewed source categories, missing fields, absent parent categories, missing parent IDs, foreign-family parents, duplicate parent IDs, fractional/non-integer IDs and 2000-row overflow fail closed. NULL is accepted only where explicitly reviewed; missing column is never synthesized.
- `DisposablePortableFinancialSourceClosureTest`: seven negative and positive tests for scoped grouped balances, historical reward purchases and their original currency, null vs omitted task currency, foreign tenant, unapproved categories and numeric aliases.
- `DisposablePortableFinancialEdgeSchemaPostgresTest`: actual PostgreSQL 18 + all Flyway migrations, validates reviewed physical source columns and nullable semantics and required family-owned parent IDs.
- This validates **source foreign-parent graph only**, not finance semantically replayable status. Ledger source_type/source_id polymorphic closure, amount/type semantics, unsupported REFUND and group/reputation invariants are **still owned by Backend D**, and must be signed off before production archive semantic closure.
- Member identity parent graph comes from earlier B-A-04e; two source validators must ultimately agree in one sealed MVCC snapshot. Current test-only modules do not implement that production integration.

## QA commands
```bash
./mvnw -B -ntp -Dtest=DisposablePortableFinancialSourceClosureTest,DisposablePortableFinancialEdgeSchemaPostgresTest,DisposablePortableMemberReferenceClosureTest,DisposablePortableMemberExceptionClosurePostgresTest,DisposablePortableMemberSourceCatalogPostgresTest test
```
Require exact-SHA non-skipped Java 21/Maven/PostgreSQL18 result and Surefire counts; full four-job CI required by Release. PostgreSQL tests use disposable containers only. No production writes, Restore execution, reset unlock or file purge.

## Architecture/consumer gate
- Open: A4-01…09 precise signed vNext role/member wire version, hidden category, privacy/tombstone/IMP; Backend B conditional logical ACK is not final serialization schema approval.
- Backend D must explicitly sign SOURCE_TYPE and immutable historical purchase/refund/rate graph; Backend B must ACK TaskInstance and source-to-target dependency interface.
- Do not mark the v1 23-category exporter as self-contained; MEMBER_EXCEPTION_PERIODS, source roles, FAMILY_MEMBERS and their historical graph are not in existing v1 allowlist.
- Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.

## Next B-A-04g
1. Exact new CI execution and defect repair.
2. Enumerate polymorphic source-type historical FK graph against Backend D, but no independent policy invention.
3. Prepare signed vNext schema implementation only after recorded Architecture/consumer ACK, otherwise continue test-only closure.
