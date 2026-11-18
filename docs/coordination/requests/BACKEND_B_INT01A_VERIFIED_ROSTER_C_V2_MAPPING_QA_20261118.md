# Backend B — IMP-01a verified offline roster → C binary mapping, exact-SHA CI request

**Date:** 2026-11-18. **Status:** source committed, new full CI required. TEST/DISPOSABLE only; no live family/reset, no production Restore activation.

## Real observed red → targeted owner correction
Actual [full CI #44289077924](https://github.com/o-kobylianskyi/family-points/actions/runs/44289077924) on `cb397d6219744effe6d2f16998ed4f7c1d6ebe42`: **Maven 749 tests, 0 failures, 1 error, 0 skips, BUILD FAILURE**; Web/Android/Compose all PASS, exact full attestation RED. The only error `PortableMemberAncestryV2Test.actualCategoryJsonAndDependencyVectorsMatchIndependentPython` was `PORTABLE_V2_ROOT_MISMATCH`: test called `computeRootWithoutTrust` which intentionally computes on a defensive copy but did not set `source.put("manifestRootDigest",root)` before checking original. QA independently fixed test in `cd98bf246e9c9614a712ebfd069c2ef5ee6a3c6c` and triggered [full-ci]. Do not revert QA's concurrent correction; preserve independent root verification.

**Actually PASSED in the above red full run** (raw Maven job `134749365348`): `WorkspacePortableAncestryV2SourceLossPostgresTest` **1/1 PG18 PASS** (real source container destroyed before offline inspection), `PortableManifestRootV2Test` **3/3 PASS**, `DisposablePortableFinancialSourceClosureTest` **9/9 PASS**. These are positive specific class results even though overall release CI is red. Independently recomputed three SHA-256 vectors using Python canonical struct.pack big-endian, sorted JSON/dep strings; exact expected hashes matched.

## New production code increments, without enabling restore
- `ProtectiveBackupImportInspector.AncestryV2Inspection` @`7952e376`: immutable **verified original role ID + member ID** lists, populated only after FPB1 decode, AES-256-GCM open, root/category/member→role/epoch/source-digest verification. Contains NO names, account authentication, credentials, parent role assignment or access grants. Existing v1 inspector stays unchanged, v2 `restorable=false`.
- `AncestryV2Inspection.canonicalMappingDigestForReviewedIds` @`a8a4e3c2`: exact checked original roster cardinality/keys and invokes C-owned `WorkspaceRestorePermitScopeV2.canonicalMappingDigest` directly, not B old text hashes. Verifies original historic IDs only; does NOT attest target membership or owner-reviewed mapping credentials and does not mint RESTORE permits.
- `WorkspacePortableAncestryV2SourceLossPostgresTest` @`a06d4d3e`/`d9923bc7`: after truly stopped source PG18, archived source member IDs returned immutable/positive/sorted; valid reviewed map equals C canonical digest, incomplete mapping rejected, duplicate target IDs rejected; wrong key/scope/tampering still fail closed. Source loss/test uses only disposable fixture family IDs.
- No schema/Flyway, app controller, production engine, external owner action or transport API added. This is one **real producer→offline verified source roster→C binary mapping** boundary, and no more.

## Independent test commands
Pin exact SHA **at or after `d9923bc79a49c824a8292c97c0e2d1539050f8db`** containing QA fix `cd98bf2`, JDK21 and Docker PostgreSQL18:
```powershell
mvn -Dtest=PortableManifestRootV2Test,PortableMemberAncestryV2Test,WorkspacePortableAncestryV2SourceLossPostgresTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest test
```
Then perform **one actually executed non-skipped full four-component [full-ci]** at exactly same immutable SHA (Maven PG18 backend, Web, Android, Compose, Testcontainers2 preflight, exact-SHA attestation and per-commit outcome). Report run ID/SHA/Maven tests/fails/errors/skips, named classes and first error stack. A green scoped job with Maven or exact attestation skipped does NOT count.

## Security/architecture blockers intentionally unchanged
IMP-01a first source subset exports only `FAMILY_ROLES`/`FAMILY_MEMBERS`, with active/tombstoned provenance and AES-root proof. Full selected task/reward/ledger/group/media/REFUND closure and 17 unreviewed FK decisions remain pending. C V2 mapping digest is merely a scoped checksum, **not** true owner enrollment/fresh reauthentication/one-use RESTORE grant. Native transactional target restore/finance/epoch/receipt absent. No full vNext restorability: `restorable=false`, `replaySupported=false`, RESET Execute HTTP423, BETA HOLD.

## Next stage
After QA pin, extend this same approved `family-points-export-v2` source schema in *sequential checkpoints* to actual TaskInstance+finance parent history and authenticated category content/closure; no competing manifest/hash variant and no activation before A4 FK dispositions and D finance invariants.
