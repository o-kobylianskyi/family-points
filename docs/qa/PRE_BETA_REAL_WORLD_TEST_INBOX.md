# FamilyPoints — PRE-BETA Real-World QA Inbox / Cross-Track Acceptance

**Owner:** QA. **Decision:** product owner, 2026-11-16. **Phase:** COLLECT_ONLY. **Release:** BETA HOLD.  
**Rule:** Collect pending real/manual/device/browser scenarios from Backend, Web, Android, Designer, Architect, Release and DevOps continuously, **but do not ask the operator to execute them one by one now**. When Backend BKL-002 reaches independent release-candidate acceptance, organize a single large, coordinated, real-world PRE-BETA acceptance campaign covering every track.

## Intake protocol
For each incoming item preserve:
- Stable test ID and producer/track, source document/issue/commit/SHA and date.
- Prerequisites, disposable test account/family and device/browser/server configuration; required sample fixture or evidence (no actual credentials in GitHub).
- Step-by-step real test instructions, expected results, negative/recovery/permissions and cross-client consistency checks.
- Dependencies (Backend APIs/Restore/VERIFIED/CI/Onboarding) and destructive-risk classification.
- Status: `COLLECTED`, `DEDUPED`, `BLOCKED_DEPENDENCY`, `READY_FOR_CAMPAIGN`, `RUNNING`, `PASS`, `FAIL`, `BLOCKED_ENV`, `DEFERRED_WITH_APPROVAL`.
- Final evidence fields: exact source SHA/build/APK, environment, observed behavior, screenshots/sanitized logs, defect issue and retest SHA.

**During COLLECT_ONLY:** continue automated unit/integration/Playwright smoke QA and report independent findings to owner tracks asynchronously. Do not imply a real-user scenario has been performed merely because a synthetic test passed. Consolidate duplicate scenarios without losing owner-specific acceptance checks. Store incoming operator instructions verbatim or link them.

## Campaign launch gates
1. Backend BKL-002 secure owner-bound encrypted protective backup/export, complete scoped FK/ledger/currency/obligation reconciliation and restore, real independently evidenced `VERIFIED`, 30-day retention and guarded atomic FULL/SELECTIVE workflow are developed and independently accepted for release-candidate scope; production `reset/execute` remains HTTP 423 until explicit safety approval.
2. Exact-SHA fully executed Backend/Web/Android/Compose CI and final Release candidate artifacts established.
3. Web V49 real enabled-backend seven-step onboarding on isolated/new disposable family, no implicit writes, proper original-UUID receipts and read-only recovery; Android matching API/epoch behavior.
4. Operator separately approves campaign environment and any potentially destructive operation on **new disposable QA families only**. Never touch existing Test/Test2, actual household or production data without separate explicit permission. No deployment based on a test plan.

## Campaign workstreams (intake buckets)
| Track | End-to-end coverage to collect | Phase |
|---|---|---|
| Backend BKL-002 | backup/export/import/preview, selective/full safe reset on authorized disposable fixture, native restore full FK + ledger + currencies, idempotency, rollback, expiry, wrong key and tenant checks | COLLECT_ONLY |
| Web / V49 | seven-step real onboarding, selections/drafts/142 templates/6 packs, review/finalize, lost response, 4 locales, mobile widths, accessible errors, cross-tab | COLLECT_ONLY |
| Android | install/upgrade, login/offline/resume, initial family creation and V49 state, tasks/rewards/media, phone instrumentation and cross-device sync, UUID/epoch/tenant | COLLECT_ONLY |
| Designer | visual/theme/icon/category semantics, language QA, catalog grouping and responsive/mobile consistency | COLLECT_ONLY |
| DevOps / Release | environment backup and recovery, pinned build/CI, artifacts/APK signing, safe staging deploy and rollback, logs/monitoring | COLLECT_ONLY |
| Cross-track | two adults/child role restrictions, two QA families, concurrency, offline → online, ledger integrity, task/reward flows and real UX | COLLECT_ONLY |

## Evidence already known (automated, NOT manual acceptance)
- Backend consolidated BKL002 #01–03: 13 classes, 27 PASS / 0 FAIL/ERROR/SKIP on `419efb1fdcab849480e790449c791c86ce004695`; later #04–#06 remain separate.
- Web V49 synthetic lost-finalize 2/2 PASS operator reported; exact checkout SHA absent, real enabled-server not accepted.
- Android Linux P0 98/98 at `09da723`; later Android V49 onboarding source needs newer-SHA acceptance.
- One four-component full CI green at earlier `3c0cf014`, not an automatic release-candidate proof.

## Intake log

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-INIT-001 | QA/Architect | Authorized isolated disposable family full BKL002 backup/reset/restore and non-destructive preview | secure Backend execute + VERIFIED + Release signoff | BLOCKED_DEPENDENCY |
| REAL-INIT-002 | Web/QA | Feature-enabled real V49 seven-step onboarding, skip and explicit finalize receipts | Backend enabled setup + safe disposable family | BLOCKED_DEPENDENCY |
| REAL-INIT-003 | Android/Designer | Real phone V49 first-family state, offline and retry behavior | matching APK + Backend V49 + device | COLLECTED |
| REAL-INIT-004 | QA/Release | Cross-client regression across all tracks on pinned release SHA | release-candidate CI and representative fixture | BLOCKED_DEPENDENCY |

**Intake is additive:** append incoming owner instructions and new scenarios here or link them, without converting COLLECTED into PASS. Campaign will be assembled, deduplicated, safety-reviewed, then separately scheduled by user approval.
