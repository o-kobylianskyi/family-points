# QA handoff — Backend integration / Flyway V35→V36→V37

**Track:** Testing / QA. **Priority:** P0 release gate.
**Candidate:** `integration/backend-beta-v35-v37` / draft PR #13.
**Deployment:** NOT on BETA. Run in isolated local/CI test environment; do not point destructive tests at production or BETA.
**Source order:** main reward ledger V35 → Calendar V36 → Media V37.

## Automated release gates

- Java/Maven full suite and PostgreSQL Testcontainers on exact candidate SHA. Check `PopulatedDatabaseUpgradeTest`: provision through V35; insert legacy workspace, parent, currency, 15-point MANUAL ledger entry and 20-point reward; migrate V36/V37; assert original rows and defaults remain, the new tables exist, history is correct and rerun is no-op.
- Verify `FamilyPointsApplicationTests` private image two-family HTTP isolation, direct SQL cross-workspace FK prohibition, image clear/revoke, binding optimistic version, and full ledger enum CHECK.
- Verify `ApprovedRewardPurchaseTest`: 20-point charge once, no double debit on retry, 409 for bought/cancelled/unapproved, ownership guards, error path. Check full transaction rollback separately when testing a real database.
- Run Web/Vite, Android/Gradle and Docker Compose CI. Do not report BETA-ready while Android fails, even when Backend CI passes.

## Manual / E2E verification after isolated deployment

1. With separate authorized accounts A/B in different families, create a 20-point reward requiring approval. Child A requests; parent A approves; child A receives. Expect exactly one `SPEND` amount -20 with `source_type=REWARD_PURCHASE`, request `PURCHASED`, session retained. Retrying gives HTTP 409 without new ledger records.
2. Attempt pending/cancelled/other-member request. Expect no debit; declined retry returns 409 where defined; unauthorized cross-family endpoint remains blocked.
3. Query `GET /workspaces/{id}/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD&type=ALL` for 1 day, 62 days and 63 days; inspect boundary handling, task deadlines versus reward windows, timezone and DST, memberId authorization and 2500-event cap. Query `GET/PUT /workspaces/{id}/calendar/time-zone`, verify workspace manager restrictions.
4. Query reward `GET/PUT /workspaces/{id}/rewards/{rewardId}/availability`; test ALWAYS, bounded date range and weekly windows with an authorized manager. Purchase outside window must be rejected before any debit.
5. Query `GET /workspaces/{id}/media/manifest` and `POST /workspaces/{id}/media/references` for member/group/task refs; upload permitted JPEG/PNG, bind catalog and uploaded avatar/cover, verify authorized variant reads. Another family must never retrieve an asset by copied UUID. Clear cover and confirm DEFAULT returns existing Soft Family Web hero; old private asset URL must return 404.
6. Test optimistic stale expectedVersion on Media bind/clear, invalid UUID, unsupported upload MIME/size, 100-reference request limit, no unauthorized partial batch. Verify uploaded media `Cache-Control: private, no-store` and catalog previews `private, max-age=86400`.
7. Run a signed Android app scenario after Backend deploy only if Release approves: Google/local login, child reward receiving and no spurious navigation to Login on HTTP 500/409, offline queue/sync isolation, workspace switching.

## Acceptance / reporting

Record build SHA, CI run links, environment, actual HTTP status and anonymized ledger counts. File concrete defects in `docs/coordination/requests/` with reproduction, expected/actual and affected component. Mark QA PASS only after automated full CI is green, isolated E2E passes, Release approves populated BETA clone migration, and Web/Android consumer ACKs. **Do not merge/deploy PR #13 merely because individual tests pass.**
