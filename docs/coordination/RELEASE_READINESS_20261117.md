# FamilyPoints — Release readiness matrix (2026-11-17)

**Decision: BETA HOLD.** A successful CI run proves build/test execution on a specific immutable source SHA; it does not approve a release, restore, reset, database migration or publication.

## Evidence anchored to immutable release-candidate source

- **Run:** https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962
- **Tested SHA:** `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a` (GitHub Actions artifact metadata and full-CI job log).
- **Results at this SHA:** Backend Maven/PostgreSQL **479 tests / 0 failures / 0 errors / 0 skipped**; Android Gradle debug/JVM build/test steps SUCCESS; Web Node regression, lint, localization, Vite production build SUCCESS; Compose + PowerShell parser SUCCESS.
- **Release-specific CI checks executed at this SHA:** CI classifier **12/12 PASS**, APK manifest **12/12 PASS**, backup fake-Docker **5/5 PASS**, deploy fake-host **8/8 PASS**, exact-SHA release-evidence validator **18/18 PASS** = **55/55** on one run. The 12 classifier tests also passed in outcome-report job.
- **CI artifacts:** backend reports ID `14311270829`; Android test reports ID `14310607580`; full CI evidence ID `14311211691`; per-push result ID `14311161945`. All run-linked metadata points to the same source SHA. Full CI attestation and outcome job SUCCESS; this is **non-deployment CI evidence**.
- **Known limitation:** Source changed on `main` after `8c397d0` (Backend handoff documents test-only packages #25–27). CI success cannot be transferred to these newer commits. Pin a new candidate and repeat full non-skipped run after final source freeze.

## Cross-track matrix

| Area | Implementation / evidence | Remaining gate |
| --- | --- | --- |
| Backend BKL-002 | Test-only safe PostgreSQL replay proofs developed; 479-test baseline PASS on SHA `8c397d0` | Later #25–27 changes need independent current-SHA QA; production authenticated portable restore still `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423; protective recovery, owner reauth, integrity/retention and full financial scope NOT accepted |
| Web Onboarding V49 | Seven-step setup, catalogue 142 templates/6 packs and UUID receipt-recovery source; historic Web job PASS at tested SHA | Real authenticated/disposable-family seven-step E2E, isolation, lost response, two-tab & epoch races, exactly-once writes, persistence and full QA signoff remain OPEN; later Web patches need pinned test evidence |
| Android | Android P0 run #43893026715 independently PASS 98/98 JVM, three critical tests; four-component Android job SUCCESS #44044271962 | Latest candidate continuity, APK signing/cert fingerprint and physical device/operator QA before publication |
| Release/DevOps | 55/55 synthetic safety tests + PowerShell parse PASS in full run #44044271962, CI attestation+JSON artifacts uploaded | New immutable candidate source SHA after updates; separate real isolated backup/restore rehearsal, approved recovery/rollback plan and signature verification before authorized deployment |
| QA | Operator collects all real tests in one pre-BETA campaign; historic local synthetic Playwright evidence | Consolidated real disposable-family test campaign, BKL-002 recovery acceptance, source/test evidence linked to candidate SHA, cross-tenant and device checks |
| Architecture | Tracks BETA HOLD and coordinates multiple owners | Final acceptance after Backend/QA/Web/Android gates and explicit operator release authorization |

## Required next sequence (not release authorization)

1. Backend and Web finish their remaining P0 source changes and provide independent QA evidence. Keep production reset/restore disabled until architectural safety acceptance.
2. QA consolidates its one real-world campaign, records results and performs only separately authorized disposable-family operations.
3. Architecture pins a release candidate SHA. Release verifies all four **non-skipped** jobs and all 55 Release CI checks for precisely that SHA, plus the two source-linked CI evidence artifacts.
4. After independent backup/restore rehearsal and APK signature verification, obtain **explicit user approval** for a specified version, target environment and risk. Only then use the separately reviewed deploy procedure. No default authorization.

Owner: Release / DevOps. Last independent full CI evidence: #44044271962 on SHA `8c397d0`. **BETA HOLD**.

## Update — 2026-11-18 / REL-RELEASE-19 (new full CI baseline)

**New verified immutable baseline:** [FamilyPoints CI #44264218493](https://github.com/o-kobylianskyi/family-points/actions/runs/44264218493), SHA `ef222689d8e928ee008dbcbd04dce0c51f2e86ad`. Actual non-skipped jobs and steps: **Backend Maven/PostgreSQL 506 tests / 0 failures / 0 errors / 0 skipped**, Web Node/i18n/lint/Vite SUCCESS, Android Gradle/JVM SUCCESS, Compose and **98 unique Release Safety cases PASS**. Full exact-SHA attestation and per-commit report SUCCESS. Prior successful runs: #44264107962 @ `eda8b9f...` with Maven 503/0/0/0 and #44264165604 @ `aef18c4...` with Maven 505/0/0/0.

**Independently downloaded artifact bytes** for #44264218493 and matched complete ZIP SHA-256 values to independently fetched GitHub API artifact metadata:
- `ci-push-result` ID `14410245239`, SHA-256 `ae2ee806df7d65837a24d0e43f543c176ec1d3d6cb4f6cffd09ad595cf4af579`, archive 450 bytes, exactly `ci-push-result.json`.
- `full-ci-evidence` ID `14409343835`, SHA-256 `76a50b348c45468ba3d14dfb14aebd6fc5d7b803c407126375427bd96d0f0665`, archive 373 bytes, exactly `full-ci-evidence.json`.

Both parsed reports agree on full SHA, GitHub Actions run URL, four SUCCESS jobs, `fullCiVerified=true`, and `qaAccepted=false`, `releaseApproved=false`, `deploymentExecuted=false`. GitHub API artifact metadata links both IDs to that source SHA and run. This is authenticated provider-source evidence for the **specific tested commit**, not independent QA acceptance or PROD/BETA authorization.

**HEAD drift:** at matrix refresh, `main` was `965d6896491c7e20a23f779131f1b211c8f3b9f1`, **30 commits newer** than `ef222689`, including further Backend split and Web V49 work. Current HEAD has no inherited green. Backend BKL002 production restore `restorable=false`, `replaySupported=false`, Reset Execute HTTP423; real feature-enabled V49, Android physical-device, portable restore/owner-auth and recovery signoff remain outstanding.

**Gate decision: BETA HOLD.** The former two failing `DisposableLedgerSourceGraphVerifierTest` checks are resolved at `ef222689`, but all later work needs its own pinned CI + QA acceptance.

## Update — 2026-11-18 / REL-RELEASE-20: post-Backend-ABC full CI
**New verified post-split baseline**: [#44265994374](https://github.com/o-kobylianskyi/family-points/actions/runs/44265994374) at `a90546b81db38d966d4bb86e742ce25aea9d9c1d` — Java/PostgreSQL Maven **507/0/0/0**, actual Web Node, Android Gradle, Compose checks all SUCCESS, 98 unique Release synthetic cases PASS, full exact-SHA attestation + per-push report SUCCESS. [#44265904431](https://github.com/o-kobylianskyi/family-points/actions/runs/44265904431) at `e4068243ef7fe6830f87bfa66d79abc8cbc636e0` separately also 507/0/0/0 and all four non-skipped SUCCESS. Run #44265676220 at `dec2404...` failed Java compilation; it is superseded only for **the later tested SHAs**, not assumed resolved for moving HEAD.

For #44265994374 independent GitHub API verified artifacts were downloaded, **actual ZIP bytes hashed**, exact provider digests matched, each archive contained one expected JSON reporting same GitHub run and exact full candidate SHA, four SUCCESS, `qaAccepted=false`, `releaseApproved=false`, `deploymentExecuted=false`:
- `full-ci-evidence` ID **14411231168**, 374 bytes SHA256 `54d7bc62aa62c7476d69991d2166de59eaf4fb3e33b97874f09cbc86971833b6`
- `ci-push-result` ID **14410232598**, 451 bytes SHA256 `b73bb61a8fda6b504e144489f17524737ea357f4c72be84b2e022c89134e57c8`

At check `main` had progressed to `a52a2777bed858a0f66a9e8cfaea90ac6e926f82`, **11 commits past** the tested SHA; next runs #44266795831 and #44266831713 were still queued/in-progress. Do not inherit their status or treat docs-only scoped-green runs as full CI.

**Backend architecture split confirmed in handoffs**: A owns portable archive exporter, original signed `FAMILY_MEMBERS` producer contract still unapproved; B owns exact V50 restore-engine and FK graph, consumer ACK pending; C owns secure Reset auth/epoch, no production unlock; real QA V49 and Android device acceptance remain pending. **BETA HOLD, production restore/reset disabled** (`restorable=false`, `replaySupported=false`, Reset Execute HTTP423), and no rollout authorization.
