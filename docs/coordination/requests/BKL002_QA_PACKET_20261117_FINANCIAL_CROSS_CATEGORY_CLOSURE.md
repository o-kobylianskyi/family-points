# BKL-002 Backend QA #31–33 — cross-category signed financial inventory

2026-11-17 CET. Test-only, archived V50 signed financial source graph. **No production restore or mutation.**

## Scope and exact commits
- `c23e1bfda864a8721d6948d1e55821d5377504a8`: extend `DisposableLedgerSourceGraphVerifier.Result` with immutable `signedMemberCurrencyBalances` and `ledgerRows`; calculate using `Math.addExact` per archived original member ID and currency ID, independently of TASK, REWARD_PURCHASE and MANUAL ancestry validations.
- `c23e1bf` also validates that signed archived `POINT_TYPES` codes are unique, nonblank, in source family and their IDs are positive.
- `5185fe948b59efa51fb138d561b396da6e2da8f8`: two-member signed balance partition and duplicate-currency-code/zero-ID AES negative fixtures.
- `2da8d2e3fe035b16fbc5afa05a8d1b6531388fc5`: mixed TASK/EARN +5, REWARD_PURCHASE/SPEND −4 and second member MANUAL +7, expected balances member 4 POINTS +1; member 5 POINTS +7, one row per supported category and original source ancestry.
- No new production interface; method signatures of existing `Result` accessors retained, with additional fields appended. Temporary redundant state added/removed in `b7f1048`/`5712e61`; effective helper source final at `5712e61`.

## Independent pinned QA
```powershell
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then previous QA #24 12-class tests, all relevant PostgreSQL 18/Testcontainers and four-component CI. **Report SHA**, run URL, Maven tests/failures/errors/skips and real Docker availability. Newly authored classes/fixtures NOT independently run by Backend source connector. Previously green 479 tests at `8c397d0` precede these commits.

## Required checks
1. Per-member/per-currency immutable signed balance map must not merge distinct original members.
2. Mixed TASK +5, purchase −4 for same member yields +1; separate manual +7 remains isolated.
3. Duplicated archived POINT_TYPES.code must fail; zero denomination ID must fail.
4. All prior TASK signed semantics, purchase parent closure, REFUND unsupported, 20/21 bounded exchange replay and real SQL rollback remain intact.
5. No legacy source_type guess, no tenant bypass, no production VERIFIED, no reset/execute unlock.

**Release boundary:** `restorable=false`, `replaySupported=false`, HTTP423 reset Execute, BETA HOLD.
