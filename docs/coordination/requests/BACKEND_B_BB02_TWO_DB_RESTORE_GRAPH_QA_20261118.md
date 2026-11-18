# Backend B / B-B-02 — two-database FK/ID replay and rollback acceptance

Date: 2026-11-18 CET. **Test-only** two isolated PostgreSQL 18 containers using actual Flyway migrations; never touch real user families or deployed databases.

## Implemented source
- `DisposableTwoDatabaseRestoreGraphExecutor.java` @`c2921656`, `ee39aef7`, `36966d27`: new TEST-only two-independent-DB graph consumer. It accepts exact caller-supplied source-member→target-member mapping and requires both original and target roster membership/role/type via existing `DisposableMemberMappingPreflight`. It reads selected original member group and nested work-node graph, rejects missing parent, absent/foreign source IDs, duplicate selection and cycles, and inserts into target in parent-first order.
- All target writes execute within ONE `TransactionTemplate`: target `families.data_epoch FOR UPDATE` preflight, empty target group/work-node conflict check and revalidation, one group and two parent-child work nodes. Injected late errors after group, parent node or child node roll back all target rows.
- `DisposableTwoDatabaseRestoreGraphPostgresTest.java` @`4c9cf1de`, `129e9648`: source/target PG18 migrated separately; third family in target already has unrelated group and node. Tests missing selected parent, duplicate source selection, unknown IDs, foreign destination member -> 0 target writes; three crash stages -> 0 rows then new executor retries complete source-to-target map with correctly rebuilt child.parent_node_id; duplicate replay denied; stale epoch denied; concurrent two replay calls => exactly one target graph insertion and untouched neighbor.
- No archive format or original category altered. This explicitly consumes a LIVE source database; it is NOT portable after original loss. No real owner reauthentication or permits minted; C owns owner security grant, B must consume an approved C grant atomically when integrated.

## Independent CI verification evidence
- Actual older four-component [Actions #44264218493](https://github.com/o-kobylianskyi/family-points/actions/runs/44264218493) at `ef222689`: all four heavy jobs green, Maven 506/0/0/0; 98 Release cases successful (Release evidence).
- More recent [Actions #44265994374](https://github.com/o-kobylianskyi/family-points/actions/runs/44265994374) on QA `a90546b8`: fetched actual Maven job 134670618386, **507/0/0/0 BUILD SUCCESS** and Web/Android/Compose/attestation/outcome success, all jobs non-skipped.
- These green runs **PRECEDE B-B-02 code**; no exact-SHA independent PASS for the new class is asserted.

## QA required commands and evidence
On immutable current SHA, JDK21 and Docker PostgreSQL18:
```powershell
mvn -Dtest=DisposableTwoDatabaseRestoreGraphPostgresTest,DisposableMultiExchangeNativeRestorePostgresTest,DisposableDurableRestoreApprovalStorePostgresTest test
```
Then full 4-component [full-ci] trigger on exact SHA, verify actual non-skipped Maven/Web/Android/Compose and all 98+ Release safety cases; record SHA, run ID, counts, errors/skips, first stack if failed. No manual real-family reset/restore is authorized.

## Scope and blockers
- Test-only bounded GROUP + two-level WORK_NODES graph is NOT the full 23-category portable V50 restore engine. No media/ledger replay/owner authentication in this new helper.
- Source membership verification requires both original and target DBs online; no signed V50 FAMILY_MEMBERS category yet. Source read and target write are not a distributed transaction; no live source mutation is attempted during test. Production design needs signed immutable A artifact.
- Backend C owns actual owner authorization and durable operation grant; Backend B will consume only approved C grant inside target transaction. Backend D owns independent finance accounting closure; Backend A owns archive schema and source integrity.
- Do NOT set `restorable` or `replaySupported` true, do not enable Reset Execute; HTTP423 / BETA HOLD.
