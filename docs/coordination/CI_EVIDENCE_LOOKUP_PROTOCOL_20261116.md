# FamilyPoints — mandatory CI evidence lookup and reporting protocol

**Owner:** all tracks (Architecture, Backend, Web, Android, QA, Designer, Release/DevOps). **Decision:** 2026-11-16; Designer evidence handoff and Release follow-up reconciled. **Source:** [Designer verification](requests/DESIGNER_TO_ARCHITECT_CI_EVIDENCE_20261116.md); issues #19 / #20. Applies to BKL-002 #15, V49 #18 and release assessment.

## The observed failure mode
- The GitHub connector `fetch_commit_workflow_runs` returns PR-triggered runs only. A main push-based run can exist even when it reports `[]`. `get_commit_combined_status` can also return empty despite successful Actions checks. **Neither empty response means FAILED, CANCELLED, NOT_RUN or successful.** Never label a run missing or CI unverified solely because those two helpers return empty.
- GitHub Actions `SUCCESS` for a docs-only or component-scoped push can still have **all heavy Maven/Android/Web/Compose steps SKIPPED**. It is scoped status, not full release evidence.
- Rapid pushes were previously cancelled by shared per-ref concurrency; Release replaced with per-run concurrency `cancel-in-progress:false` and added per-commit reporting. This increases runner load, and cannot alone establish actual run completion.

## Mandatory evidence resolution
1. Verify and record **candidate SHA** separately from live **current HEAD SHA**. Never silently transfer a PASS across newer commits.
2. Search actual `push` / `workflow_dispatch` Actions **repository branch runs** and/or reuse real run IDs from journals/issues/Actions URL. If list unavailable, use previously recorded run ID. Query jobs via `fetch_workflow_run_jobs(repo_full_name,run_id)`; read steps/logs/XML on ambiguities.
3. Record `runId, runURL, event, candidateSHA, currentHEAD, completedAt, job conclusions, selected scope and real test STEP conclusions`. Count `skipped` as SKIPPED regardless of job display SUCCESS.
4. Distinguish:
   - `FULL_PASS_AT_SHA`: four components actually ran and their required test/build/Compose steps succeeded on the *same* SHA; where present, the `Full CI / exact-SHA evidence` job/artifact must also be checked.
   - `SCOPED_PASS_AT_SHA`: selected relevant component steps succeeded; others intentionally skipped. Never promote to full release approval.
   - `FAIL_AT_SHA`: actual required job/step failed with log evidence.
   - `CANCELLED_AT_SHA`: workflow genuinely cancelled, not a test failure.
   - `IN_PROGRESS`: not completed.
   - `LOOKUP_BLOCKED`: available access cannot retrieve a run; explain which method failed and what would resolve it.
   - `UNVERIFIED`: incomplete or absent evidence; do not invent status.
5. Preserve immutable historical evidence and state the tested scope. Never claim newly added attestation/report jobs ran in a historical workflow.
6. When real proof is found, append run ID, SHA, event, selected/actual steps and acceptance scope to Release journal; other tracks link it instead of repeating inconclusive lookups. QA and Release final signoffs are separate from code CI.
7. **No operator requirement to install/authenticate `gh`.** Connected GitHub tools plus documented run links suffice. No PROD/BETA probing as part of routine CI lookup.

## Verified baseline and newest observations
- Designer's independent four-job full CI: [run 43934619348](https://github.com/o-kobylianskyi/family-points/actions/runs/43934619348) at SHA `3c0cf01416cfca2b4f9b71b9af1c40822db8b302`, all actual Maven, Android, Web regression/lint, Compose steps PASS. This is **FULL_PASS_AT_SHA** for that historical candidate only; predates the newer attestation job.
- Android P0: [run 43893026715](https://github.com/o-kobylianskyi/family-points/actions/runs/43893026715), SHA `09da72316257c4ce17a34caf57f8a7864605582f`; 98/98 JVM, critical 3/3 PASS in inspected XML per Android journal.
- New docs-only push [run 43942204198](https://github.com/o-kobylianskyi/family-points/actions/runs/43942204198), SHA `98ec26013`, overall SUCCESS; `CI / per-commit outcome report` SUCCESS and upload step SUCCESS; **all four heavy component test steps SKIPPED**, full attestation SKIPPED. This is **SCOPED/no-code-change reporting success**, NOT FULL_PASS.
- Similarly inspected run IDs 43942161232, 43942029174 and 43941980370; all report job success with four heavy steps skipped and attestation skipped. These concrete run IDs demonstrate visibility despite prior empty PR-only lookup.
- Current HEAD changes often during parallel work. Always refresh before a release decision. A confirmed old green run never overrides new failing or unverified code.

## Cross-track obligations
- **Designer/Android:** use historical exact-SHA Android green as P0 test evidence; latest Android source changes still need current-SHA checks. Do not claim old PASS as latest.
- **Backend:** gate each QA packet on relevant real Maven/PG execution; a green docs-only CI is NOT proof that restore/ledger tests ran.
- **Web:** same for Node/lint versus real V49 authenticated browser E2E.
- **QA:** use precise PASS/FAIL/SKIP evidence; never infer test success from workflow badge alone.
- **Release:** inspect two consecutive post-concurrency-change push runs and publish status with actual run ID. Verify a full run with four non-skipped jobs for release; do not conflate it with BKL-002 owner/restore acceptance.
- **Architecture:** keep BETA HOLD while P0 backend #15 or real V49 #18 are incomplete.

No deployment, destructive family operation or real-data QA authorized by this document.
