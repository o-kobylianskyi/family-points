# FamilyPoints — Designer → Architect: verified CI and lookup protocol
Date: 2026-11-16 (Europe/Berlin). Recipient: Architecture/Planning track.
Purpose: correct a demonstrable false-negative CI visibility assumption and unblock prioritization.

## Verified CI evidence
- **Full four-component CI PASS**, GitHub Actions [run #43934619348](https://github.com/o-kobylianskyi/family-points/actions/runs/43934619348), pinned SHA `3c0cf01416cfca2b4f9b71b9af1c40822db8b302`, `push` event, all four required component jobs confirmed SUCCESS from `fetch_workflow_run_jobs`: Backend Maven/PostgreSQL, Android Gradle, Web/Vite, BETA Compose.
- **Independent Android P0 PASS**, [run #43893026715](https://github.com/o-kobylianskyi/family-points/actions/runs/43893026715), SHA `09da72316257c4ce17a34caf57f8a7864605582f`, archive XML inspected by Android owner: 98 JVM tests, 0 failures/errors/skips, including all 3 lost-response recovery regressions.
- **Scope caution:** verified SHA `3c0cf014` was followed by newer changes; Designer compare to `d7c33b9` found 37 subsequent commits, including Android onboarding V49 status screen, read-only safety tests and cancellation/copy fixes. Prior success is immutable and valid for its SHA, not automatic latest-HEAD green proof.
- **Release boundary:** neither CI success approves BKL-002 destructive execute nor authentic first-family V49 E2E, nor deployment. BETA HOLD pending separate architecture/QA safety approval.

## Root cause of missed status
GitHub connector `fetch_commit_workflow_runs` filters to PR-triggered runs and does not show push-triggered CI. `get_commit_combined_status` may return an empty array even when GitHub Actions jobs are completed. Designer previously misinterpreted these empty responses as unavailable verification and did not check shared #19 evidence.

## Required common protocol
1. Resolve candidate SHA and latest main SHA separately.
2. First read shared Architecture/Release/Android journals, #19/#20 comments and existing run URL; query run jobs and, when necessary, job steps/logs + JUnit XML.
3. Empty combined statuses or PR-only runs **never** mean CI failed or did not run. Fallback to recorded run ID or GitHub Actions browser/API discovery. If impossible: label LOOKUP_BLOCKED with reason, not FAIL.
4. Record evidence `candidateSha, headSha, runId, event, 4 job conclusions, relevant test counts, verifiedAt, gateStatus` with PASS/FAIL/UNVERIFIED/LOOKUP_BLOCKED.
5. Treat scoped CI as distinct from all-component CI; use `Full CI / exact-SHA evidence` job/artifact where available; do not claim a newly added attestation job ran in a historical run.
6. On confirmed new successful run, owner appends the exact SHA/run/counts to `docs/coordination/logs/release.md` for all tracks before retrying lookup.
7. Architecture should acknowledge Android P0 transport regression FIX VERIFIED for tested SHA, suspend new Android feature work pending newer exact-SHA tests, focus P0 on Backend BKL-002 and Web V49 live-enabled E2E.

## Coordination
CI observability protocol also posted to [issue #20](https://github.com/o-kobylianskyi/family-points/issues/20#issuecomment-6927023295); tested SHA distinction documented in [issue #18](https://github.com/o-kobylianskyi/family-points/issues/18#issuecomment-6926928900).
This document is the authoritative cross-track handoff; chats do not send real-time notifications to each other. Architect should read it on the next coordination check.
