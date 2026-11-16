# BKL002-QA-20261116-12 — P0 repair for two RED Maven CI tests (run 43947907960)

**State:** READY_FOR_QA, NEEDS_RETEST. **Source owner:** Backend. **Independent QA:** Issue #15. **Previous confirmed run:** commit 19adfe1b7, workflow 43947907960, Java/Testcontainers 460 tests, 1 assertion failure, 1 NPE, 0 skipped. Four-component release gate FAILED (Android/Web/Compose actual PASS). These repairs are NOT YET proven green. Exact main SHA must be pinned before/after QA run.

## Root causes and repair
1. `WorkspaceRewardObligationArchiveClosurePostgresTest` at prior line 189 expected 1 reward category despite fixture creating both **Blocked media** and **Media rewards** within source family. Correct archive family-scoped expected count is **2**, not 1; test now compares source SQL family category count ==2, commit `839e9f8148ae642464e4f4d6eabe68d182d15b4a`. This preserves full export semantics, never weakens tenant checks.
2. `DisposableRewardCategoryRestoreExecutor` called `Map.of(...).containsValue(null)`; immutable maps reject null search argument, throwing NPE before remap. Replaced with `definitionIds.values().stream().anyMatch(Objects::isNull)`, commit `e1e4e2b16281b18ea9faa15a27c19489623de6d7`. This maps invalid explicit null target IDs to `CATEGORY_DEFINITION_REMAP_INCOMPLETE` SecurityException instead of NPE.
3. The same executor required category names in unsorted order, while authenticated inspector returns sorted alphabetical categories. Fixed exact 3-category scope order in `e1e4e2b`; otherwise NPE fix would be followed by an avoidable `CATEGORY_REPLAY_SCOPE_UNSUPPORTED` denial for valid source archive.
4. Explicit regression `9ccfe2e4dd73f0d7f11721566afd10dd9eca9ba0`: user-provided mutable definition mapping with null target value must fail SecurityException and leave target category count zero *before* a legal map replays successfully.

## QA targeted command (Java 21 / isolated disposable PostgreSQL 18)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableRewardCategoryCrossDatabasePostgresTest,WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePortableArchiveForeignRelationTest,DisposableCrossDatabasePortableRestorePostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## Acceptance
- CI-RED-01: two exact formerly failing tests PASS without skipped cases; no NPE or false category count.
- CI-RED-02: reward category archive includes both source categories, not only those attached to a particular reward; cross-family categories still forbidden.
- CI-RED-03: null target map yields explicit SecurityException; zero destination writes.
- CI-RED-04: authentic 3-category archive satisfies canonical alphabetical inspector manifest, test-only native category remapping commits all expected links, no neighbor change.
- CI-RED-05: double replay, wrong family and ciphertext tamper still denied; complete Maven targeted regression counters PASS/FAIL/ERROR/SKIP by same pinned SHA.
- CI-RED-06: full four-component exact-SHA CI must independently run all actual components non-skipped before Release can claim a green gate. A subsequent docs-only SUCCESS is NOT acceptance.

No production restore/owner proof/VERIFIED or destructive execute. **Reset Execute HTTP423 · BETA HOLD.**
