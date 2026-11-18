# Backend B — next large independent package: v2 historical-data privacy gate

**2026-11-18 · Execution status: production-source implementation committed, runtime/CI acceptance PENDING.**

## Sequencing decision
- Owner requested to proceed to next large package now and defer further QA-075 repairs until full prior CI evidence is obtained. The QA-075 source-tenant guard and nonempty PG18 fixture remain unchanged; Draft PR #21 stays independent and unmerged.
- Do **not** open a duplicate [full-ci] trigger solely because this package landed. QA-076/077 and Release confirm `fetch_commit_workflow_runs` did not provide a current run ID on `a311f289` or newer; latest independently evidenced full four-component green is 749/749 @`89b0f8e1`, predating both history and privacy changes.

## Delivered independent privacy boundary
- `src/main/java/com/olehkobylianskyi/familypoints/service/PortableV2HistoryDataPolicy.java` @`8bfbb8d6`: new bounded, stateless **fail-closed** archive content policy for ALL six selected historical source categories (`POINT_TYPES`, `TASK_DEFINITIONS`, `REWARD_DEFINITIONS`, `TASK_EXECUTIONS`, `REWARD_PURCHASES`, `POINT_LEDGER`). Rejects explicit private account/login/credential/secret/token/provider identity columns, including nested JSON; rejects invalid root row shapes, excessively deep nested metadata, large arrays and long free-text blobs. Does not silently redact original source rows.
- `PortableV2HistoryGraph.attest` @`52fb7df2` invokes the policy **before any category/root digest**, both from real PostgreSQL `capture()` and from already AES-GCM-authenticated offline JSON verification. Thus exporter and inspector agree on the same exclusions without another format/hash implementation. Existing original 2-category roster and legacy v1 inspection are unaffected.
- `PortableV2HistoryDataPolicyTest` @`ea77ff8a`: legitimate finance/task history passes; 6×7 secret-bearing negative combinations, nested secret, malformed row, oversized text, absent category fail closed.
- Extended the active `PortableV2HistoryGraphTest` @`adab63ec`: 4 historical categories directly reject `provider_subject` inserted into an otherwise valid canonical v2 graph **before** obtaining a manifest root, and original valid eight-category digest remains supported.

## Deliberate non-goals and independent acceptance
- This is an initial **high-risk-field denylist**, not the final reviewed physical-column allowlist for all tables. Business descriptions can themselves contain personal information and remain encrypted; this patch does not claim cryptographic key recipient approval or full data minimization. A separate finite schema/privacy audit remains necessary before enabling actual Restore.
- This package does not modify the QA-075 PG18 test, event enum/parent maps, original-owner reauthentication, permit, target Restore transaction, HTTP endpoints, migrations or filesystem retention.
- QA after earlier full CI can run: `mvn -Dtest=PortableV2HistoryDataPolicyTest,PortableV2HistoryGraphTest,WorkspacePortableAncestryV2SourceLossPostgresTest test` and then one **non-skipped full-four** Maven PG18 / Web / Android / Compose / exact-SHA run on a single immutable SHA. Report F/E/S, source-loss and privacy class outcomes, first stack, then close only proven gates.
- Keep `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, **BETA HOLD**.