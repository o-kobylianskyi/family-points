# FamilyPoints — Release → QA: final operator-assisted acceptance and evidence handoff

- **Status:** READY_FOR_PICKUP
- **Created:** 2026-11-16
- **Initiating track:** R (Release / DevOps)
- **Receiving / accountable track:** QA (Testing)
- **Priority:** P0 — BETA release gate; no real checks with Release chat
- **Depends on:** Architect P0 dispatch `DISPATCH_20261116_P0_BETA_RECOVERY.md`, Backend BKL-002 #15, Web V49 #18, Android P0 #19; Designer→QA `DESIGNER_TO_QA_REAL_V49_ACCEPTANCE_20261116.md`
- **Impact/ADR:** ADR-FAMILY-RESET-002; no schema/API change requested

## User-approved ownership boundary
The operator will conduct **all real/manual tests only with QA**, not with Release, Android, Web or Designer chats. QA should supply the operator with executable, exact-step real-test instructions when readiness prerequisites are met. Release continues CI automation, artifact/signature integrity, evidence aggregation, safe deploy preparation and handoff; Release must not ask the operator to run real-family tests or independently treat existing PROD/BETA as a QA sandbox.

## QA work to own
1. **Now: read-only automated/source and authenticated QA preflight** on pinned exact SHA. Check the existing V49 Designer QA handoff (do not duplicate/relax its detailed scenarios), test catalog 142 templates / 6 packs, 7 explicit setup steps, role and tenant isolation, optional offline/Android UI preflight. Report actual results vs BLOCKED/SKIP; prior local 47/47 Playwright and Android P0 98/98 evidence belong to their exact older SHAs.
2. **Operator-assisted V49 real E2E**, only after QA confirms an **explicitly approved disposable DEV/QA family**, correct API/client version and credentials. QA provides per-step procedure and captures exact IDs/HTTP methods, request UUIDs, revision/epoch, 7-step state, import/receipt/activation and fail-closed negative scenarios. Never replay a destructive or uncertain POST automatically.
3. **BKL-002 backup/restore/reset real acceptance** must remain BLOCKED until Backend provides a safe owner-bound implementation, protective encrypted snapshot + verified restoration, appropriate independent tests, and Architecture/QA signoff, **followed by explicit user authorization specific to an isolated disposable family**. Existing family, PROD/BETA, live DB and permanent data are excluded. Do not tell operator to run HTTP execute/reset/restore based solely on this request.
4. **Android real handset final acceptance** and Web real browser final smoke to be coordinated through QA only after matching signed APK / client and isolated server SHA are available; record signature and deployment source SHA separately from UI screenshots.
5. For final Release handoff, QA must report `case ID | tested exact SHA | isolated environment | action | expected | actual | HTTP UUID/count | PASS/FAIL/SKIP/BLOCKED | redacted proof`, include backend row/ledger invariants and per-case blockers. Route defects to owners and link issue #15/#18/#19; explicitly declare whether QA acceptance is still BLOCKED.

## Release CI requirements and boundaries
- Release reads four actual non-skipped jobs on **the same candidate SHA** (Maven, Android, Web, Compose). Historical verified full CI run #43934619348 at `3c0cf01416cfca2b4f9b71b9af1c40822db8b302` and independent Android P0 #43893026715 at `09da72316257c4ce17a34caf57f8a7864605582f` are accepted as historic proofs only.
- Current `.github/workflows/ci.yml` includes `full-ci-attestation` and `push-ci-report` for more discoverable per-SHA evidence; no claim of current HEAD full-pass without run/job verification.
- **No BETA deployment or post-deploy public HTTP smoke without separate specific approval**; CI and DEV/QA stay separate from PROD. Release manager `prepare` is only an UNVERIFIED local evidence draft.

## Acceptance from QA
- [ ] QA acknowledges this ownership routing in `docs/coordination/logs/qa.md`.
- [ ] QA supplies operator-ready real test procedure only after prerequisites; records case-level PASS/FAIL/BLOCKED.
- [ ] QA explicitly tests data isolation/safety on disposable instance; no live-family reset.
- [ ] QA provides signed version/source SHA and evidence package for Release to inspect.
- [ ] Release independently accepts matching CI, QA and recovery evidence before requesting deployment approval.

## Response
- **QA acknowledgement:** pending next QA session
- **Test evidence:** pending
- **Release acceptance:** BETA HOLD; no real-test request sent to operator by Release
