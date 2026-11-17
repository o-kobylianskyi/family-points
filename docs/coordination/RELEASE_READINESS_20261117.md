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
