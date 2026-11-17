# FamilyPoints — BETA candidate evidence template (2026-11-17)

**Decision: BETA HOLD. THIS FILE IS NOT DEPLOYMENT AUTHORIZATION.**

## Immutable source and CI proof

| Gate | Latest verifiable source | Required at release freeze |
|---|---|---|
| Candidate tag / source SHA | NOT PINNED; no approved tag | Annotated tag peeled to exact full SHA, clean tree |
| Historical full CI | [Run #44044271962](https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962) @ `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a` | Re-run after freeze, do not transfer older green |
| Java/PostgreSQL | Historical 479 tests, zero failures/errors/skips | Exact candidate SHA non-skipped Maven PASS, full report |
| Web | Historical i18n/regression/lint/Vite PASS | Exact candidate SHA non-skipped PASS; real Web V49 separately |
| Android | Historical Gradle debug/JVM PASS | Exact candidate SHA non-skipped PASS; device/signature separately |
| Compose | Historical PowerShell parser/config PASS | Exact candidate SHA non-skipped PASS |
| 55 Release safety tests | Historical 12 classifier + 12 APK + 5 backup + 8 deploy + 18 evidence PASS | Exact candidate SHA 55/55 |
| CI evidence artifacts | Full CI artifact 14311211691, push-outcome 14311161945 @ same historical run | Two authenticated GitHub artifacts from one exact candidate run, full attestation PASS |

At this source audit, branch `main` was observed as `c0af393dce0a8c8a700b1ae53f1f0e53d309234d`: **41 commits newer** than prior green `8c397d0`. Changes include PostgreSQL BKL002 test-only replay/integrity and Web V49 two-tab synthetic specs. The historical result does not cover later commits.

Before any future authorized tagged release, verify the **existing annotated tag**, clean checkout and downloaded full-CI artifacts against one identical candidate SHA using the read-only tool:

```powershell
python scripts/beta/verify_release_tag.py --repo-root . --tag vX.Y.Z-beta.N --candidate-sha <full-40-hex-sha> --push-report ci-push-result.json --full-evidence full-ci-evidence.json
```

This checker will reject lightweight/missing tags, altered/dirty checkout, tag-to-HEAD mismatch, different CI SHA or run ID, and scoped/failed CI. The check **does not fetch remote tags** or prove GitHub ownership of a local tag or evidence JSON. Independently inspect the remote annotated tag, actual GitHub job steps and artifact metadata. Even a consistent tag/CI report is not QA acceptance or permission to deploy.

Verify downloaded JSON internal consistency with `python scripts/beta/verify_release_evidence.py --candidate-sha <40hex> --push-report ci-push-result.json --full-evidence full-ci-evidence.json`. This does **not** authenticate files, validate job execution, or grant release authorization; independently inspect actual GitHub Actions job steps and artifact metadata.

## Product, safety and independent QA

| Area | Status | Required proof |
|---|---|---|
| Backend BKL-002 | **BLOCKED** | Owner-bound encrypted portable recovery, financial/reward/task graph completeness, atomic native restore, durable VERIFIED and independent recovery, source/target integrity |
| Production reset/restore | **DENIED** | Current `restorable=false`, `replaySupported=false`, Reset Execute HTTP423. Keep blocked pending safety acceptance |
| Web V49 | **QA PENDING** | Authenticated feature-enabled disposable-family 7-step setup, exactly-once finalize, receipt, persistence, cross-tab/epoch/lost-response and role isolation |
| Android | **QA PENDING** | Physical-device acceptance, signing fingerprint, APK manifest/hash/version and offline sync checks |
| Backup/recovery | **QA PENDING** | Isolated restored database, integrity, off-host protection and tested recovery plan; archive TOC alone is insufficient |
| QA campaign | **NOT SIGNED OFF** | One consolidated approved operator-assisted real campaign, independent synthetic reports on pinned source |
| Architecture | **NOT APPROVED** | Final review of migration, restore and rollback compatibility |
| User approval | **NOT GRANTED** | Explicit decision naming tag, host, risk and timing after all gates are verified |

## Procedure and decision

No candidate tag or deployment is created by this template. Backend/Web first complete P0; QA owns real scenarios; Architecture pins immutable SHA; Release checks all four non-skipped component jobs, 55 synthetic checks and authenticated same-run JSON artifacts; authorized isolated restore and APK signing follow; only then may user explicitly approve a particular rollout. A CI-green build is not a substitute for any of these.

**BETA HOLD. No PROD/BETA changes, migration, reset, APK publication or deployment approved.**
