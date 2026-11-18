# Backend C C-C-11 — Verified-owner enrollment evidence & V2 consumer QA

**2026-11-18. STATUS:** source committed, consumer interoperability converging, owner enrollment still **NOT A PRODUCTION AUTHORIZATION**. Never infer original owner from oldest/admin/email/archived member.

## Verified independent CI evidence (pinned older SHA)

Actually inspected backend job **134698875830** in Actions [#44274406405](https://github.com/o-kobylianskyi/family-points/actions/runs/44274406405):
- `DisposableOwnerPermitStorePostgresTest`: **6/6 PASS**.
- `DisposableOriginalOwnerBindingPostgresTest`: **6/6 PASS**.
- `WorkspaceResetProofConcurrencyPostgresTest`: **6/6 PASS**.
- `WorkspaceRestorePermitScopeV2Test`: **7/7 PASS**.
- `WorkspaceRestoreSelectionClosureV2Test`: **5/5 PASS**.
- Combined **30/30** selected methods PASS at that run's immutable SHA, not a claim of full suite success.
- Entire Java Maven **684 tests, 0 failed, 1 error, 0 skipped, BUILD FAILURE**, solely `WorkspaceBackupRegistryPresenceRacePostgresTest.registryEvidenceChangesDuringCiphertextReadAndNextAuditRefusesStaleDigest` (`DataIntegrityViolationException` inserting `workspace_protective_backups`) assigned to Backend A/QA. Web, Android, Compose jobs SUCCESS; exact-SHA full attestation FAILED. Later A/QA fixture changes require a new exact-SHA full run.

## B-C interoperability

Backend B committed `DisposableRestoreScopeV2ConsumerTest` @ `590040d2`, independently pinning three **C-C-10 fixed binary** SHA-256 vectors for mapping, selection and overall V2 scope. This is evidence of B's source-level consumer adoption of C's canonical v2 hash; **not final signed B ACK of complete DTO, manifest and atomic receipt/approval interface**. Old B-B-07 pipe-delimited hashes must never be silently reinterpreted as C V2 credential scopes. Full shared IMP still required.

## C-C-11 additions

1. `security/OriginalOwnerEnrollmentEvidence.java` @ `ac4cb72f`: pure immutable candidate evidence type distinguishing `AUTHENTICATED_REGISTRATION` with authenticated creator event from `LEGACY_INDEPENDENT_VERIFICATION` with independently reviewed account recovery case. Exact workspace/global-account/member IDs, reviewer reference, timestamp; never accepts inferred admin, emailed name, role or archived source ID as evidence; strict freshness + active membership and exact account-member link. `permitsEnrollment` receives independent **trustedEvidenceVerified** result; untrusted caller cannot turn that boolean into true owner authorization. No Spring bean, DB writer or endpoint. 8 negative/positive test methods `OriginalOwnerEnrollmentEvidenceTest` @ `a2dd81d5`.
2. `DisposableOriginalOwnerBindingPostgresTest` enhanced @ `a2582a35`: **2 extra PG18 methods** verify malformed persisted cross-family owner binding remains ineligible at resolver, and competing two-thread original-owner enrollment has at most one row under unique family PK. QA-only schema intentionally demonstrates that production needs composite tenant/FK protection too.
3. Existing C V2 binary vector tests have independently matched a Python binary encoder of exact int32/int64/UUID/hash bytes as a second implementation. B source now includes same vector checks, but exact new SHA CI is required.

## Required engineering implementation protocol

- **Registration transaction:** authenticated account from Spring Security context; create family, original-member row and account-workspace membership, plus owner-binding and non-repudiable audit record in ONE transaction. Database unique original family binding, composite tenant/owner member FK and immutability; never grant to a manager accidentally.
- **Legacy enrollment:** no automatic backfill; require independent evidence verification, reviewer audit and proof freshness. Owner subject must bind account and exact active family member. Denied by default if evidence missing or ambiguous.
- **Owner transfers:** retain creator provenance, separate audited current authorized owner entity/process; do not rewrite creator.
- **Permit issuance:** genuine password or provider fresh auth, distinct action RESET/RESTORE, nonce/secret only hashed at rest, <=5 minute expiry, server clock and one-use DB grant. RESTORE binds canonical signed source manifest/root + reviewed selection/member pairing, mapping digest, source and target epochs.
- **Atomic consume:** target family row lock then grant/operation rows, replay + epoch + receipt in one target transaction; failure rolls back grant and data. Recovery after lost POST must GET immutable receipt instead of reissuing authorization.
- **IMP:** Architecture assigns exact Flyway/version, writer and reviewer source owners; B/A/D ACK signed manifest and category/finance closure before incompatible code or real deployment.

## QA command

```powershell
git pull --ff-only origin main
git rev-parse HEAD
./mvnw -Dtest=OriginalOwnerEnrollmentEvidenceTest,DisposableOriginalOwnerBindingPostgresTest,DisposableOwnerPermitStorePostgresTest,WorkspaceRestorePermitCanonicalVectorsTest,WorkspaceRestorePermitScopeV2Test,WorkspaceRestoreSelectionClosureV2Test,WorkspaceResetProofConcurrencyPostgresTest test
```

Run Maven directly if wrapper unavailable, then full **non-skipped** [full-ci] four components on pinned SHA. Return class-by-class counts, first actual stack and SHA. The two new owner DB tests and eight new pure policy tests are **NOT_RUN** by Backend C.

## Safety status
`POST /workspaces/{workspaceId}/data-reset/execute` remains HTTP 423; Restore production remains unavailable; BETA HOLD. No real original owner enrollment, owner recovery, family Reset/Restore, schema migration or deployment executed.
