# QA dispatch — 2026-11-02: BKL-002 ADR accepted, Issue18, Reward History P1

**Sources:** `docs/architecture/ADR-FAMILY-RESET-002.md`, `docs/coordination/requests/REWARD_HISTORY_IMPLEMENTATION_GATE_20261102.md`, `docs/api/REWARD_PURCHASE_HISTORY_V1.md`, RewardService/RewardController/RewardPurchaseRepository on latest inspected main.

## Evidence separation
- Most recent fully executed Web batch: operator **41 PASS / 1 SKIPPED / 0 FAIL**, prior to newly added Task/Reward 2 tests.
- Latest operator Web discovery: **44 total, 6 PASS, 38 SKIPPED**, likely missing QA credentials; it **does not** validate new tests. Restore proper QA environment without printing secrets, pin SHA.
- Backend media/source and disposable composition: targeted BUILD SUCCESS was reported by Backend; no full production Restore Execute or same-SHA four-job green release proof.
- ADR is ACCEPTED as contract only; **not implementation signoff**. No permanent family reset/restore or real data deletion.

## P0 — BKL-002 ADR-FAMILY-RESET-002 QA closure matrix
| Gate | Mandatory test | Safe expected result |
|---|---|---|
| R01 | Owner bound transactionally on fresh family, legacy owner unestablished | No heuristic assignment; OWNER_NOT_ESTABLISHED denies destructive |
| R02 | Non-owner MANAGE_WORKSPACE, group delegated admin, child | Preview according to role; execute unauthorized |
| R03 | Account in families A+B, concurrent membership/owner switch | Protected identity and B untouched under lock |
| R04 | FULL unsupported category | Entire transaction rejected RESET_CATEGORY_UNSUPPORTED, never partial FULL |
| R05 | Empty SELECTIVE, unsupported entity graph | EMPTY_RESET_SELECTION / RESET_DEPENDENCY_BLOCKED, no implicit cascade |
| R06 | ZERO_BALANCE_KEEP_HISTORY | Append inverse ledger entries; preserve immutable purchase IDs and referential history |
| R07 | CLEAR_ECONOMIC_HISTORY | Only closed FK graph; active entitlement blocked, no silent refund |
| R08 | SELECTIVE REPLACE_SELECTED | Only chosen categories change, all others byte-identical or DB digest identical |
| R09 | FULL from portable encrypted file | All **supported** family-local categories reconstructed atomically with FK/media integrity |
| R10 | Same operation UUID, lost HTTP response | Read receipt, no second apply; changed payload UUID collision denied |
| R11 | Wrong key, tampered archive, wrong family/schema, dangling polymorphic media | Rejected preflight, unchanged target/source/neighbor |
| R12 | Stale preview fingerprint/epoch and concurrent changes | Conflict; no write, no stale owner approval |
| R13 | Fresh reauthentication: local password, recent Google OIDC, unsupported Google flow | ≤5-min one-use bound proof, unavailable Google proof blocks |
| R14 | Exact family name confirmation, rename between preview/challenge | Server validates current normalized name, rejects stale proof |
| R15 | Protective pre-reset backup with media | Actual recoverability replay proven; corrupted/oversize snapshot prevents execute |
| R16 | File portability passphrase KDF and AES authentication | Argon2id/reviewed memory-hard KDF, unique salt, lost passphrase fails closed |
| R17 | Server-local encrypted artifact 30-day TTL + file deletion/quota/lease | Physical cleanup verified; downloaded copies unaffected |
| R18 | Post-commit epoch and Android offline queue | Old epoch operations quarantined; no blind replay; atomic receipts |
| R19 | Audit/owner/auth/global data retention | Never imported/reset, including shared external family membership |
| R20 | Rollback fault at FK/ledger/media/receipt phase | No partial inserts/deletes, epoch and neighboring family unchanged |

**Unsupported is valid FAIL-CLOSED:** generic MERGE, cross-family file migration, Delegated Owner, Device Owner wipe, unsupported schema converter. Current Reset Execute HTTP 423 stays locked until proven.

## P0 — Issue #18
- I01 import catalog preview matches confirmed codes and workspace, lost reply reconciles GET before retry, zero auto-activation.
- I02 parent may explicitly create member group vs WorkNode folder (separate endpoints), delegated permission scoped.
- I03 task/reward activation from imported private template uses durable receipt for successful/unknown HTTP, no repeated POST on refresh; account/family switch fences stale responses.
- I04 cross-family, child permissions, stale epoch, duplicate code, canceled modal and stale preview covered.
- I05 no writes merely by navigating or reloading; **two new tests exist but were skipped** in credentialless 44-case run. Synthetic fault-injection tests A01–A18 from prior QA P0 matrix pending.

## P1 — Reward purchase history v1 source audit and acceptance
Backend new endpoint `GET /workspaces/{workspaceId}/rewards/history`: items are persisted `RewardPurchaseResponse` only; `nextCursor`/hasMore, default 25, max100, keyset `purchasedAt DESC, id DESC`; optional memberId/status/date. `REDEEMED` != parent-confirmed delivery. Requests/obligations are NOT returned; don't mark full lifecycle complete.

| Gate | Test and assert |
|---|---|
| H01 | >11 and >25 purchases, consecutive pages no omitted or duplicated IDs; stable tie timestamp `id DESC` |
| H02 | limit 1,25,100 positive; 0,101 invalid; malformed/oversize cursor rejected |
| H03 | Cursor from another workspace/member cannot reveal unauthorized rows even if supplied |
| H04 | Ordinary member defaults to own; explicit sibling memberId refused; manager can filter within same workspace only |
| H05 | Cross-family manager/member denies foreign target IDs; authenticated workspace fencing; no timestamp leakage |
| H06 | status filter only persisted PENDING_APPROVAL/READY/REDEEMED/CANCELLED; date range inclusive; reversed invalid |
| H07 | Historical reward renamed/deleted: purchase snapshot price, title, duration and currency preserved |
| H08 | Read-only GET on Web/Android no reward purchase POST, ledger balance mutation, inferred DELIVERED/RECEIVED |
| H09 | Concurrent insert at page boundary; deterministic cursor and no duplicate within traversed pages |
| H10 | Android cached per account/workspace/member/dataEpoch; old-epoch history inaccessible after switch |
| H11 | Four locales UI, loading, empty, error, filters, Show more; no 10-row truncation |
| H12 | Future unified request/purchase/obligation lifecycle is SEPARATE API/ADR gate, not inferred from v1 |

**Source-review risk to test:** `RewardService.purchaseHistory` decodes cursor as `LocalDateTime|id` and `RewardPurchaseRepository.findHistoryPage` orders by time then id. Verify the query works with nullable params in actual PostgreSQL/Hibernate 7. Negative cursor date parsing and integer overflow must fail closed. Backend HTTP handler status mapping should be verified, not assumed.

## Execution and evidence
1. Backend disposable PG: run `WorkspacePortableMediaBindingPreflightPostgresTest`, `DisposableComposedNativeRestorePostgresTest`, `WorkspacePortableNativeCommitRoundtripPostgresTest` and dedicated history pagination/ACL tests once added.
2. Web: restore QA credentials *without exposing secrets*, run `npx playwright test` and capture summary; special focus Issue18 POST cases; two new Task/Reward route smoke tests were never executed in latest 44-case run.
3. Android: only JVM/SQLite/Gradle synthetic checks until user reauthorizes live/device checks.
4. Pin the candidate SHA; finish all four GitHub Actions jobs. Report PASS/FAIL/SKIP counts with exact source/consumer version.

**P0 release blocker** remains actual production FULL/SELECTIVE transaction/receipt/reauth/backup verified executor. **P1 reward history** cannot be called complete until server PG + Web + Android acceptance. Work against disposable DB, never Test/Test2/DEV/Beta/Prod destructive.
