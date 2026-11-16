# BKL002-QA-20261116-11 — Independent PostgreSQL reward-category native replay (test-only)

**Status:** READY_FOR_QA. **Developer execution:** NOT_RUN; GitHub connector is source-only and has no Maven/Docker runner. **Owner:** Backend implementation; QA independent test acceptance; Issue #15. Only disposable test databases, no real family data.

## Scope delivered
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableRewardCategoryRestoreExecutor.java` commit `20e564c`: **test-only** AES-authenticated portable V50 category/definition/link extraction from source workspace and bounded target replay within `TransactionTemplate` for exactly `REWARD_CATEGORIES`, `REWARD_DEFINITIONS`, `REWARD_DEFINITION_CATEGORIES`.
- Requires explicit, injective complete source reward-definition ID→target reward-definition ID map. All mappings are validated against target family while locked; target epoch must be zero, target categories and mapped definition links empty. Source categories and reward definitions must belong to source workspace. Duplicate, foreign or dangling pairs fail before insert. New category PKs and association FKs reconstructed; no ID-preserving assumptions.
- Validates restored category link count, keeps unrelated family intact; second replay rejected. No production HTTP/JWT/owner proof/ledger, no `VERIFIED`, no application native restorer. Explicit target reward definition pre-existing is a **test fixture**, not a full restore path.
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableRewardCategoryCrossDatabasePostgresTest.java` commits `4ac22f7`, `67cf369`: two independent PostgreSQL 18 containers migrated via Flyway; source RewardDefinition + Category + link exported AES, neighbor category/reward+association retained in target, fresh target definition mapped and new category/association restored. Wrong-family mapping, replay twice, corrupted encrypted payload blocked. Source unchanged.

## Run on QA workstation / independent CI (Java 21, Docker)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableRewardCategoryCrossDatabasePostgresTest,WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest,DisposableCrossDatabasePortableRestorePostgresTest" test
git rev-parse HEAD
```

## QA acceptance criteria
- CAT-RESTORE-01: source PostgreSQL A is physically separate from target B; snapshot and AES file authenticated, correct source tenant scope, source graph 1/1/1.
- CAT-RESTORE-02: source reward-definition ID mapped explicitly to already reconstructed target definition; new reward-category ID differs from old, restored link points to remapped target definition and category.
- CAT-RESTORE-03: neighbor reward definition, category and association retained unchanged, no leak into target family.
- CAT-RESTORE-04: second invocation fails with no new categories/links. Foreign target mapping fails with no writes. Wrong AES/tampered ciphertext fails before target modifications.
- CAT-RESTORE-05: README/restore gates unchanged: encrypted source archive alone never yields `VERIFIED`, `restorable=false` in generic import inspector and financial `replaySupported=false`.
- CAT-RESTORE-06: report real pre/post SHA, counts, failures/errors/skipped and env evidence to Issue #15; fail-closed on any ambiguity. No user-directed real test needed.

## Risks and follow-up
- Source category label and metadata are replayed; production restoration of definitions, purchases, requirements, obligations, blocked sets and financial postings still incomplete.
- Test-only executor cannot be invoked by production service; full-family restore, root owner binding, recent reauth, durable VERIFIED/restore receipt and Reset Execute remain disabled. BETA HOLD, HTTP423.
- Previously accepted QA 27/27 at SHA 419efb1 covers packages #01-03 only; newer work remains NOT_RUN pending QA.
