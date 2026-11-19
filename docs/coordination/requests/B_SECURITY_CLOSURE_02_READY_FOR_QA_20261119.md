# B-SECURITY-CLOSURE-02 — READY_FOR_QA (2026-11-19)

**Producer:** Backend B. **Status:** `B_SECURITY_CLOSURE_02_READY_FOR_QA`; NOT independently QA-accepted, NOT BETA release approved. Per Architecture [Issue #15 #7011348258](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7011348258) and current B assignment.

**Candidate:** [Draft PR #41](https://github.com/o-kobylianskyi/family-points/pull/41), branch `backend-b/b-security-closure-02`, immutable source SHA **`f1d722b6ad698d09ee661112a2edaf34b1f61f68`**, based on independently QA-accepted source-scoped PR39 head `e6b65b85580a8fbb946853e03e8356b20abdef05`. PR39/main/Release/Retention/Web/Android were not modified; no merge, tag or deployment.

## Four changed paths

1. `src/main/java/com/olehkobylianskyi/familypoints/service/GoogleTokenVerifier.java`: exact Google production JWKS `https://www.googleapis.com/oauth2/v3/certs` retained as hardcoded private constant, public constructor now explicitly Spring `@Autowired`, forwards to **package-private** clientId/JWKS-URI constructor solely for local test. No external JWKS configuration parameter, public API, trust fallback, bypass or logging of JWT secrets. Existing Nimbus decoder+JwtTimestampValidator and issuer/audience/azp/subject checks execute unchanged.
2. `src/test/java/com/olehkobylianskyi/familypoints/service/GoogleTokenVerifierLocalJwksCryptoTest.java`: runtime RSA-2048 key generated per test (no checked-in key); local loopback HTTP JWK; real signed RS256 JWT using the **production GoogleTokenVerifier** rather than mocked decoder. Accepts Google issuer variants, correct audience and multi-audience correct azp. Rejects same-kid wrong RSA signature, unknown kid, wrong issuer, missing intended audience, wrong/missing multi-audience azp, blank and oversized subjects, expired and future-not-before tokens. **This is local cryptographic proof, NOT externally issued Google token/live provider login.**
3. `src/test/java/com/olehkobylianskyi/familypoints/service/WorkspacePortableUnsupportedSourcePostgresTest.java`: genuine disposable migrated PostgreSQL 18 with nonempty out-of-contract `reputation_transactions` bound to source family; production v2 `exportWithHistory` rejects before sealing. Separate disposable target family retains zero receipts/permits/business changes. No broad 23-category Restore enabled.
4. `src/test/java/com/olehkobylianskyi/familypoints/security/WorkspaceRestorePermitExactBoundaryTest.java`: deterministic nanosecond-precise five-minute scope with true just-before and false at/after expiration and >5min rejected; **does NOT substitute full real-DB clock elapsed five minutes / PG18 application consumption TTL**. Existing PG18 stored-expiry and race tests from PR39 remain present but use a shifted persisted expiry timestamp.

## Verified evidence
[GitHub Actions #4669](https://github.com/o-kobylianskyi/family-points/actions/runs/44546321751), immutable source SHA `f1d722b6ad698d09ee661112a2edaf34b1f61f68`: all **7/7 CI jobs SUCCESS** (Java21/Maven/PostgreSQL18 Testcontainers, Web/Android/Compose, non-skipped exact SHA attestation); raw Maven report **797 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. Three new suites independently observed in raw Java job:
- `WorkspaceRestorePermitExactBoundaryTest`: 1/1 PASS
- `WorkspacePortableUnsupportedSourcePostgresTest`: 1/1 PASS (2.660 sec)
- `GoogleTokenVerifierLocalJwksCryptoTest`: 1/1 PASS (0.587 sec)
No duplicate full CI needed.

## QA assignment
Freeze exact source `f1d722b6`; inspect PR39→PR41 changed-file/constructor seam and production-pin invariant; verify local JWK host bound to loopback and private RSA key only ephemeral; independently inspect positive/negative JWT cases, PG18 source unsupported-category rejection, receipt/permit untouched, scope 5min boundary. Do not represent this as real Google account success, full PostgreSQL wall-clock TTL, authenticated browser proof, or a release authorization.

**Open final gates:** externally provider-signed Google ID-token/live account and linked-identity operator E2E UNVERIFIED; actual PostgreSQL five-minute wall-clock expired-permit consumption exactly at boundary UNVERIFIED; WEB-01/WEB-02 authenticated disposable Chromium UNVERIFIED. Current restore stays bounded to approved eight-category source; unsupported populated source fails closed. BETA host PG17/nonproduction infrastructure release gates separately owned by Release.

**Safety:** PR Draft/unmerged; `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD. No real families/production writes or deploy.
