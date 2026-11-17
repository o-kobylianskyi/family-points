# BKL-002 Backend QA packet #25 — Financial history cross-category negative checks

Date: 2026-11-17 CET. Track: Backend. TEST ONLY; disposable PostgreSQL 18 / two isolated databases.

## Code under test
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableMultiExchangeNativeRestorePostgresTest.java`
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableExchangeHistoryIntegrityVerifier.java`
- New commit: `bcac5bdf24c295e1fb96d11e97ed761f6a8c2977`.

## New assertions
1. After valid two-request replay (10 POINTS→2 TOKENS and 20 POINTS→4 TOKENS), swap target `point_exchange_rates.from_point_type_id` / `to_point_type_id` within the same target family. `verify()` must throw `SecurityException`; restoring original rate endpoints must restore identical semantic SHA-256.
2. Alter historical target `point_exchange_requests.from_amount` and `to_amount` by +10/+2 without changing corresponding ledger. `verify()` must throw `SecurityException`. Restore original request values.
3. Existing checks remain: extra +1/-1 zero-sum pair rejected; drift in denomination code, timestamp, transaction description rejected; bad mapping rejected; neighbor untouched; late SQL credit failure rolls back all exchanges.

## Independent exact-SHA execution
Run with Docker/PostgreSQL 18 available:
```powershell
mvn -Dtest=DisposableMultiExchangeNativeRestorePostgresTest test
```
Report SHA, full Maven summary, errors/failures/skips, target and source row counts if any failure. Expand to related #24 12-class QA bundle, then four-component CI as gating evidence; do not infer green from the source-only commit.

## Safety
No real workspace, no production endpoint or archival verification promotion. `restorable=false`, `replaySupported=false`; Reset Execute remains HTTP 423; BETA HOLD pending architect, QA, Release and owner authorization.
