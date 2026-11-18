# Backend B / QA-075 — P0 tenant provenance production fix

**2026-11-18 · Production fix committed; newest CI not yet independently verified.**

## P0-1 addressed in production source
- Source: `PortableV2HistoryGraph.java` @ `76773efd27f6189396ad2e3e24791ac8af2d52d4` rejects omitted/null/numeric-string/wrong original `family_id` for `POINT_TYPES`, `TASK_DEFINITIONS`, `REWARD_DEFINITIONS`, before source sealing or canonical root acceptance.
- Regression: `PortableV2HistoryGraphTest.java` @ `0d7c7a40d3cade5ef41edb71882f7ca3ded2be77` tests all 12 bad variants and positive canonical eight-category manifest.
- Independent QA Draft PR #21 stays UNMERGED; QA can verify red-to-green against the committed source fix, then choose whether to integrate the independent acceptance file. Do not override QA branch.

## P0-2 remains OPEN
- Existing disposable PG18 source-loss test seals a historical archive containing **zero actual task/purchase/ledger rows**. Pure JSON populated source test is NOT nonempty real PG18 E2E proof.
- Needed: insert real point type, task/reward definitions, historical task execution, reward purchase and actual point transaction rows into disposable PostgreSQL 18 using *migrated schema and real runtime source_type values*; archive under one epoch, destroy original database, verify nonempty counts / actual row parent graph and C authenticated SourceArchive offline, wrong tenant/key/tampering denies.
- Verify physical required columns, nullable reward purchase parent and privacy allowlist before adding the test. This is not waived by passing 12 security regressions.

## One full non-skipped CI gate
`mvn -Dtest=PortableV2HistoryGraphTest,PortableManifestRootV2Test,PortableMemberAncestryV2Test,WorkspacePortableAncestryV2SourceLossPostgresTest test`
Then all-four Web/Android/Compose, Maven PG18, exact SHA attestation and original source-loss. Publish job URL, immutable SHA, tests/failure/errors/skips and first stack; previous 749/749 @89b0f8e1 predates this code.

RESTORE remains disabled: `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD. No live data, migrations or deploy.