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


## Intake checkpoint — 2026-11-16: Designer real V49 handoff and Backend QA #06–07

### Verified new manual-test source
- Designer's authoritative [operator-assisted V49 acceptance instructions](../coordination/requests/DESIGNER_TO_QA_REAL_V49_ACCEPTANCE_20261116.md), published in commit `d7c811575`, **collected in full by reference**, without executing Phase B/C. Source requires three phases:
  - **A read-only:** exact SHA, V49 capability/GET seven-step state (empty/partial allowed), catalog 142 entries and six packs, 4 languages/theme/mobile, navigation does not write, Android parity, family/role denial, epoch/logout cleanup.
  - **B explicit mutating onboarding:** only after separate operator approval of a *new disposable* workspace: seven PATCH step decisions, two-pack TASK/REWARD draft selection, side-effect-free review, exactly one original-UUID finalize private import, persisted server COMPLETED/revision, reload/two-tab/recovery, separate explicit activation. Setup/import must not alter ledger or auto-create active definitions.
  - **C isolation and persistence:** two family scopes, owner/child/foreign credentials, invalid revision/epoch, malformed receipt, concurrency, lost response without repeat POST; unchanged pre-existing family state and balance.
- Evidence fields: candidate SHA/build, masked workspace, setup epoch/revision, browser/device, HTTP methods/count/UUID, expected/actual, PASS/FAIL/SKIP, sanitized proof. **No data-reset/execute or existing family writes authorized by this handoff.**
- Mapping: existing `REAL-INIT-002` is now `COLLECTED + BLOCKED_DEPENDENCY`; add child case IDs `REAL-V49-A01..A06`, `REAL-V49-B01..B08`, `REAL-V49-C01..C03` preserving full instructions at source path.
- Subsequent Web `cb5c44f47c` step-decision double-submit guard and stale error/preview corrections should receive independent synthetic regression; not a reason to trigger real tests now.

### Additional Backend source gaps requiring future real campaign coverage
- `BKL002-QA-20261116-06`: reward obligations + requirements currently not exported with linked purchase/definition: exporter must fail closed, native replay unsupported. See [packet](../coordination/requests/BKL002_QA_PACKET_20261116_REWARD_OBLIGATION_CLOSURE.md).
- `BKL002-QA-20261116-07`: `reward_definition_categories` linking graph cannot silently disappear; archive source should reject unexportable category links and inventory unsupported category. See [packet](../coordination/requests/BKL002_QA_PACKET_20261116_REWARD_CATEGORY_LINK_CLOSURE.md).
- New P0 source tests #04–#07 are **not** automatically covered by prior `27/27 PASS @ 419efb1`; retain async QA execution queue. Real campaign will require complete rewards history/obligation/category/currency/ledger mapping and no silent loss on backup→restore once full supported production API exists.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-V49-A01..A06 | Designer/Web/Android | Authenticated read-only preflight, states, permissions, catalog, locales/themes and zero implicit writes | selected safe environment and recent SHA | COLLECTED |
| REAL-V49-B01..B08 | Designer/Web/Backend | Approved isolated fresh-family 7-step write/review/finalize/private import/activation and receipt recovery | enabled V49 Backend, explicit operator approval and isolated family | BLOCKED_DEPENDENCY |
| REAL-V49-C01..C03 | QA/Web/Backend | Two-family ACL, conflicting epoch/revision/tabs, no ledger change or duplicate writes | isolated multi-role fixture | BLOCKED_DEPENDENCY |
| REAL-BKL002-LEDGER-01 | Backend/QA | Full replay across purchases, obligations, requirements, category links, historical source IDs and currencies, including exclusion/rollback | supported actual native restore engine and durable VERIFIED | BLOCKED_DEPENDENCY |

**Do not mistake acceptance of automatic synthetic V49 finalize 2/2 or bounded group ledger 27/27 for real campaign PASS.**


## Intake checkpoint — Release final QA ownership and subsequent development deltas (2026-11-16)

- **Release→QA handoff accepted:** [RELEASE_TO_QA_FINAL_ACCEPTANCE_20261116.md](../coordination/requests/RELEASE_TO_QA_FINAL_ACCEPTANCE_20261116.md) (commit `aeec43d4`). Release explicitly delegates ALL final manual/real testing to QA+operator, including true V49 on an authorized new disposable family, BKL002 owner-protected backup/reset/restore only after independent gates and separate approval, Android physical-handset final acceptance, browser smoke, role/tenant evidence. Release retains CI/artifact signing/deploy readiness, NOT authority to execute real tests.
- Expected operator instructions will be **one coordinated campaign** after Backend readiness, not ad hoc test requests from Release/Android/Web/Designer. Required evidence includes case ID, separately pinned backend/server and web/APK SHAs, APK signature, workspace masked ID, epoch/revision, HTTP UUID/method counts, balances/FK verification, actual vs expected and redacted artifacts.
- **New Web V49 step decision safety:** `cb5c44f`, `f0ea9b8`, `abe645a` prevent duplicate `PATCH /setup/steps` on repeated button presses/previously decided step; append manual cross-tab rapid-click/reload case `REAL-V49-DECISION-01`, require one request only, no hidden retry, revision exact +1 on verified decision. Not run with real server; collect for campaign.
- **Backend newer graph coverage:** `GROUP_POINT_LEDGER`, reward obligations, requirements and category associations remain explicit source/restore dependencies. Latest source `e7572ad..7ede590..687a1df` adds guarded portable reward requirements and an encrypted source-archive test. This **does not** prove production native requirements restore; add case `REAL-BKL002-REQUIREMENTS-01` to confirm complete parent/tenant association and exact restored graph once supported; no real test now.
- **Pending auto QA:** BKL002 #04–#07 and new reward requirements portable regression require new pinned-SHA isolated PostgreSQL runs, separate from real campaign. Last accepted `27/27 PASS @419efb1` is not current-HEAD proof.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-REL-001 | Release/QA | Device signed APK install/upgrade, matching server & Web source, SHA/signature parity, safe rollback & observability | release-candidate artifacts, full non-skipped CI, QA-only operator campaign | BLOCKED_DEPENDENCY |
| REAL-REL-002 | Release/QA | Safe isolated DEV post-deploy smoke, no production writes and evidence bundle handoff | approved disposable environment and separately authorized staging deploy | BLOCKED_DEPENDENCY |
| REAL-V49-DECISION-01 | Web/Designer/QA | Rapid double-click, repeated and cross-tab step decisions: no duplicate PATCH or implicit repeat | feature-enabled disposable V49 server | BLOCKED_DEPENDENCY |
| REAL-BKL002-REQUIREMENTS-01 | Backend/QA | Complete reward requirement parent/category/member/currency graph survives encrypted backup+restore with FK closure | production safe native complete replay + verified snapshot | BLOCKED_DEPENDENCY |

**Ownership acknowledged**; no manual live acceptance has been executed.


## Intake checkpoint — Web owner real V49 handoff + Backend QA #08

- Read canonical [Web→QA V49 handoff](../coordination/requests/WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md), commit `433855b`. It supplements Designer's three-phase V49 campaign, does **not** authorize execution. Preserve exact server empty `steps:{}`→7 visible PENDING, 142 templates/6 packs, four locales, one PATCH/action, same revision/epoch, review `toImport/alreadyImported`, confirmation/finalize original UUID, durable receipt, private template-only import and unchanged active rewards/tasks/ledger. Lost-PATCH and lost-finalize recovery GET-only, no hidden duplicate POST; cross-tab conflicting markers, wrong revision/epoch, repeated click and owner/child/foreign denial require fail-closed.
- The Web synthetic `onboarding-v49-finalize-recovery-synthetic.spec.js` grew from two validated cases to **four**: valid receipt+stale, valid receipt+COMPLETED (prior 2 PASS) plus **wrong final revision** and **wrong epoch** (new cases NOT_RUN). Use latest pinned SHA when automation executes; keep synthetic vs enabled-backend distinction.
- Read Backend [BKL002 QA #08](../coordination/requests/BKL002_QA_PACKET_20261116_REWARD_REQUIREMENTS_PORTABLE.md), commit `ec72d56`: `REWARD_REQUIREMENTS` now optionally authenticated/exportable with same-family one-of owning definition/request and optional task dependency, but `restorable=false`, native replay unsupported. All P0 runtime acceptance #04–#08 remains waiting current-SHA consolidated isolated Testcontainers. Existing `27/27 PASS @419efb1` does not cover #08.
- **Manual campaign mapping:** `REAL-V49-FAULT-01` wrong final revision / epoch / conflicting second tab original-UUID receipt, `REAL-V49-REVIEW-01` toImport/alreadyImported split with zero unexpected activation/ledger writes, `REAL-BKL002-REQUIREMENTS-02` request-origin one-of owner with optional linked task and foreign-tenant denial after actual safe native implementation. All are COLLECTION ONLY; no operator instructions until readiness.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-V49-FAULT-01 | Web→QA | Lost PATCH/finalize response, wrong revision/epoch and conflicting tab markers; GET-only reconciliation, original UUID | approved isolated enabled-backend V49 | BLOCKED_DEPENDENCY |
| REAL-V49-REVIEW-01 | Web→QA | Review subset, alreadyImported/toImport, explicit final import yields only private templates and unchanged financial rows | approved isolated enabled-backend V49 | BLOCKED_DEPENDENCY |
| REAL-BKL002-REQUIREMENTS-02 | Backend→QA | Request-vs-definition requirement owner mapping, optional task parent and foreign-tenant safeguards on real restore | full safeguarded native restore and VERIFIED approval | BLOCKED_DEPENDENCY |

All these cases are additional acceptance criteria in the **single planned final QA campaign**, not prompts to execute now.


## Intake checkpoint — 2026-11-16: Reward category graph, V49 epoch fencing, CI report tests

- Backend source commits `e6a91fbd` and `7ff21788` now add optional encrypted `REWARD_CATEGORIES` + `REWARD_DEFINITION_CATEGORIES` association graph to scoped portable export, category whitelist and financial closure classification. An association requires same-family category plus archived owning reward definition and category; nonportable relations must **fail closed**. This is source-level progress, **not** native replay/VERIFIED or accepted test run. Preserve future manual case `REAL-BKL002-CATEGORY-01`: after a fully approved disposable restore, verify reward definition↔category associations and no cross-family leakage, missing-parent rejection, exact before/after counts. Await Backend formal QA packet and independent automated regression.
- Web code `2717222f`, `442a4581`, `c583c598`: step PATCH responses must keep the original family/epoch and expected revision transition; stale epoch/revision responses must be rejected, not used to mutate UI state. Add `REAL-V49-EPOCH-01`: safe new disposable family session switch/epoch change during setup; verify no stale step decision appears committed or hidden repeat PATCH. This joins already collected V49 multi-tab and lost-response scenarios. No real execution now.
- Release source `753f9293`, `19ecc693`, `0d8244a4` includes a fail-closed CI outcome classifier with 12 automated local regressions, and source claims **12/12 local PASS** as recorded in Release journal. This is CI-tooling verification, **not** a four-job latest-SHA cloud CI run, nor a human-real QA gate. Add `REAL-REL-EVIDENCE-01` final evidence check: ensure true all-four non-SKIPPED jobs + same candidate SHA + signed APK and server version, distinguish docs-only scoped green from full green; Release owns automation.
- Pending Backend automated QA previously #04–#08; source additions since `419efb1` are not included in historical 27/27. Continue async exact-SHA synthetic Testcontainers acceptance; do not request live operator testing or claim package #09 until formal handoff is found.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-BKL002-CATEGORY-01 | Backend→QA | Reward-category mapping + association FK preserved on secure disposable archive/restore; foreign-family/category/missing-parent negative | completed production-safe native restore, VERIFIED + explicit disposable workspace approval | BLOCKED_DEPENDENCY |
| REAL-V49-EPOCH-01 | Web→QA | Reject stale epoch/revision step-PATCH results during workspace change; no duplicate PATCH | enabled V49 isolated server and operator approval | BLOCKED_DEPENDENCY |
| REAL-REL-EVIDENCE-01 | Release→QA | Verify genuine all-component executed exact-SHA CI and artifact/API/APK integrity, not scoped-green substitute | release candidate, run IDs, signing evidence | BLOCKED_DEPENDENCY |


## Intake checkpoint — 2026-11-16: Backend QA #09 and Web V49 UX/race additions

- Received formal [BKL002-QA-20261116-09](../coordination/requests/BKL002_QA_PACKET_20261116_REWARD_CATEGORY_PORTABLE.md), source `b461d516`. Encrypted portable `REWARD_CATEGORIES` and `REWARD_DEFINITION_CATEGORIES` with composite PK, mandatory definition/category parents when linked, cross-family association denial, duplicate pair inspector denial; `restorable=false`, no native replay. Automated Testcontainers cases CAT-ARC-01..06 **NOT_RUN by producer**, pending consolidated QA #04–#09.
- Read latest Web `99a3741`: new synthetic `qa/e2e/tests/onboarding-v49-epoch-race-synthetic.spec.js` protects a one-time PATCH followed by cross-epoch response and GET-only reconciliation, not executed yet. `4c1cda5`, `025fe58`, `5c969dc` fix/check partial filtered deselection: selecting a few matching TASK/REWARD codes then pressing deselect must remove only selected matches without wiping choices outside the filter; an empty-result filter shows clear zero-results UX. Add real acceptance `REAL-V49-FILTER-01` to inspect filtered partial deselect, reload persistence, multi-pack and four-locale mobile rendering; no user execution now.
- Web `46148d8` adds a read-only retry when setup/catalog fetch unavailable; add `REAL-V49-RETRY-01`: failed GET may be manually retried with GET only, no setup/selection/finalize mutation. Remains separate from lost-PATCH response scenario.
- Architecture safety: older `27/27 PASS@419efb1` still valid only at prior SHA; newest #09 category graph and Web UI changes do not inherit it. Keep manual campaign `COLLECT_ONLY` and do not activate production BKL002.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-V49-FILTER-01 | Web/Designer | Partial filtered deselect, unrelated selected codes intact, zero-match feedback, 4 languages/narrow viewport | approved disposable V49 state | BLOCKED_DEPENDENCY |
| REAL-V49-RETRY-01 | Web/Designer | Failed setup/catalog GET with explicit GET-only retry; zero implicit writes | approved isolated V49 UI and controlled read error | BLOCKED_DEPENDENCY |


## Intake checkpoint — 2026-11-16: Backend QA #10 portable reward obligations / V49 bootstrap retry

- Received [BKL002-QA-20261116-10](../coordination/requests/BKL002_QA_PACKET_20261116_REWARD_OBLIGATIONS_PORTABLE.md), source commit `266b8792`, **READY_FOR_QA / Backend developer NOT_RUN**. New optional encrypted categories `REWARD_OBLIGATIONS`, `REWARD_OBLIGATION_BLOCKED_CATEGORIES`, `REWARD_OBLIGATION_BLOCKED_REWARDS`; ownership scoped to family; purchase/optional requirement parent graph, category/reward blocked-target foreign FK, duplicate composite link rejection, source and neighbor preservation. Category source graph is exportable but **native replay remains unsupported**, inspector always `restorable=false`. QA automated scope #04–10 consolidated, exact-SHA still NOT_RUN.
- Added manual-pre-BETA `REAL-BKL002-OBLIGATION-01` (future approved disposable replay): purchase, obligation, optional requirement, blocked category/reward associations and ledger conservation must persist together; archive missing any mandatory target must fail closed, foreign family unaffected. **No real test until Backend native replay and VERIFIED are independently accepted**.
- Web new source `758dfe49` tests synthetic failed-bootstrap GET manual recovery to seven pending steps with zero writes. Add `REAL-V49-BOOTSTRAP-01` for single future enabled-server new-family read failure and explicit GET-only recovery, without implicit setup/activation/finalize POST. Automated new test is source committed, not independent Playwright PASS.
- Release/architecture gates unchanged: four actually executed component jobs on pinned candidate SHA, guarded owner-authorized protected backup and rollback proof, manual execution only with QA in newly approved disposable family, BETA HOLD and HTTP423 reset/execute.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-BKL002-OBLIGATION-01 | Backend/QA | Obligation + blocked category/reward + optional requirement + purchase FK and ledger survive guarded encrypted replay; foreign graph denied | fully native disposable restore + independently evidenced VERIFIED and approval | BLOCKED_DEPENDENCY |
| REAL-V49-BOOTSTRAP-01 | Web/QA | Failed initial setup/catalog GET then explicit read-only retry renders seven PENDING and causes no writes | enabled V49 disposable setup / controlled GET fault | BLOCKED_DEPENDENCY |


## Intake checkpoint — 2026-11-16: Release BETA backup atomicity and isolated QA

- Release `8a16916d8e06` changed `scripts/beta/backup.sh`: `umask 077` and file 0600, same-directory hidden `.partial` artifact, cleanup trap on failure, `pg_dump -Fc` must succeed and be nonempty, `pg_restore --list` must read archive TOC, final backup published by atomic no-overwrite hard link `ln`. Explicitly **TOC check ≠ successful data restore**, and backup does not prove off-host protected copy or BKL-002's family-scoped `VERIFIED`.
- Release `b64c2619abd0` added five fake-Docker, disposable-fixture tests (`scripts/beta/test_backup_safety.py`): success/private mode, partial dump failing, invalid archive, existing final filename, absent `.env`. These are committed SOURCE tests; **not claimed independently run** by QA. They must run in Release automation and have exact SHA/test counts.
- Capture `REAL-REL-BACKUP-01` for later authorized disposable infrastructure: actual produced backup readable, separately restored into isolated DB and row-count/FK/ledger checked; private permissions, retention/off-host encryption, no overwrite or partial leftover on simulated failure. No public BETA/PROD database action now.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-REL-BACKUP-01 | Release/QA | Disposable BETA-style pg_dump atomic archive, failed-job cleanup, secret file permission, independent restore into disposable PG and data integrity | approved isolated env, release-candidate SHA, backup retention and rollback plan | BLOCKED_DEPENDENCY |

**Important:** Release's pg_dump full-database operational backup is a distinct mechanism from BKL-002's tenant-scoped protected `.fpbackup` archive. Both must be independently accepted and must not be conflated.

## Intake checkpoint — V49 two-tab finalize identity and cross-track new CI blockers

- Web→QA handoff [WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md](../coordination/requests/WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md) updated in `bed509893f65`: `ownsSetupFinalizeMarkers` checks both session and shared marker identity immediately before finalize. Collect `REAL-V49-TABS-02`: two tabs with competing UUID; matching UUID but different account/family/epoch/revision/selected codes; marker missing/corrupt/storage denial; marker changed between staging and dispatch; success only one finalize POST on exact scope. Cross-tab storage coordination is best effort, Backend idempotency authoritative. New synthetic tests source `c731a06b`, not real server PASS.
- Release/Android signed APK source integrity pipeline has confirmed source PowerShell parser failure at `scripts/android/build-release-apk.ps1:19` in runs `43953661904`/`43953986968`. Add `REAL-REL-APK-02` to future candidate evidence review: exact pinned SHA manifest, signing fingerprint and rejected invalid manifest, clean build; **only after Release fixes parser and verifies synthetic CI**, no publication now.
- Backend latest two full Maven runs FAIL on test-only reward category native replay scope before null-map denial, 460/461 tests and one failure. This is an automatic P0 defect routed to Backend, not a reason to execute real backup/reset.

| ID | Producer | Scenario / source | Dependencies | Status |
|---|---|---|---|---|
| REAL-V49-TABS-02 | Web/QA | Two-tab competing finalize UUID, staged/changed marker, same UUID foreign scope, missing markers, zero unsafe POST | approved isolated V49 environment and Backend receipt enforcement | BLOCKED_DEPENDENCY |
| REAL-REL-APK-02 | Release/Android/QA | Signed APK fingerprint/sourceSha/manifest identity with parser and safety regression evidence, no live publication | repaired release scripts, pinned signed artifact and exact-SHA full CI | BLOCKED_DEPENDENCY |

## QA-015 intake — obligation transaction rollback and two-tab V49 synthetic coverage

- Backend formal #14 [OBLIGATION_NATIVE_REPLAY](../coordination/requests/BKL002_QA_PACKET_20261116_OBLIGATION_NATIVE_REPLAY.md) adds test-only two-independent-PostgreSQL obligations and blocked category/reward replay. Record extra real-campaign prerequisite `REAL-BKL002-OBLIGATION-ROLLBACK-01`: when full production-safe restore and disposable authorization exist, prove failure at row 2 rolls back row 1 and composite blocked target links, while preserving neighbor family, currency and ledger identity. **BLOCKED** by production native replay, identity/financial closure and VERIFIED.
- Web source `fe64edb` adds two synthetic browser scenarios for cross-tab V49 original UUID preservation and competing receipt marker. Maintain queued `REAL-V49-TABS-02`; synthetic test not yet independently executed, cannot substitute for actual enabled-backend finalization test. Await exact-SHA browser PASS/FAIL.
- Release `build-release-apk.ps1:19` source still syntactically broken as inspected at QA-015. Do not attempt APK publish or BETA deploy. CI newer than 43953986968 not verified green.

## QA-016 intake — manual ledger reconciliation is not yet production evidence
- New Backend **test-only** point ledger source→target replay: `DisposablePointLedgerRestoreExecutor` `1d9fb146`, `DisposablePointLedgerCrossDatabasePostgresTest` `39b699c3`. Collect `REAL-BKL002-POINT-LEDGER-01` after full native approved restore: original member/currency identity, point transaction historical source type/id binding, exact per-currency balances, no phantom/double posts and unchanged foreign family. Current source uses fixture currency codes not authenticated archive codes; this gap must be closed before manual acceptance. BLOCKED_DEPENDENCY, no real data touched.

## QA-018 intake — authenticated denomination proof

- Backend portable backup now includes `POINT_TYPES` in the encrypted source archive (source commits `e6f4bde`..`a567b4b`), and the test-only manual ledger replay validates expected and target point code against the authenticated source code. Keep `REAL-BKL002-POINT-LEDGER-01` blocked until full production native polymorphic ledger restore, member+currency mapping and independent durability/financial verification; *do not* repeat the old finding that source code has no archive currency metadata for the modified manual ledger scope.
- Add future `REAL-BKL002-DENOMINATION-01`: approved disposable family only, multiple source point types with different numeric IDs, same-code target mapping and explicit mismatch; per-member/per-type balance integrity, reward purchase/request/group ledger denomination parent closure and foreign-family denial. This is a final campaign gate only, not permission to run now.

## QA-021 intake — authenticated ledger ancestry (TASK vs MANUAL)

- New BKL002 source-only read-only verifier `DisposableLedgerSourceGraphVerifier` and tests (Backend packet [#17](../coordination/requests/BKL002_QA_PACKET_20261116_LEDGER_SOURCE_GRAPH.md)) can validate POINT_LEDGER MANUAL source_id=null and TASK source_id→signed TASK_EXECUTIONS row belonging to same historical member, with POINT_TYPES source codes under AES. Unsupported refund/reward purchase/exchange paths remain blocked; no production restore or target source mapping.
- Collect `REAL-BKL002-LEDGER-ANCESTRY-01` only for *future approved final disposable PRE-BETA environment*: TASK award entry must preserve exact source task execution identity, historical participant, point currency and amount after a verified full restore; a task owned by a different participant, missing source task, duplicate ledger ID, refunded/unsupported ancestry or cross-tenant history must not replay or change balances. Prerequisites: production-safe polymorphic remap, full sealed financial graph, durable VERIFIED, owner approval and pinned green all-four CI. CURRENT STATUS BLOCKED_DEPENDENCY, no real tests now.

## QA-023 intake — reward purchase ledger economics

- Backend packet [BKL002 #18](../coordination/requests/BKL002_QA_PACKET_20261116_LEDGER_PURCHASE_ANCESTRY.md) adds read-only signed source ancestry for REWARD_PURCHASE historical POINT_LEDGER. Preserve for future real campaign `REAL-BKL002-PURCHASE-LEDGER-01`: after independently approved end-to-end financial restore, purchase ID maps to exactly one purchase ledger spend with same member, currency, original positive price and negative SPEND amount; block wrong owner/currency, price drift, duplicate purchase/ledger, refund/exchange unsupported ancestry without changing balances. Prerequisites remain full native purchase+point source_id map, owner-bound archive, financial reconciliation, durable VERIFIED and explicitly approved disposable workspace, all-gates green. **BLOCKED_DEPENDENCY** now; source verifier in src/test is read-only and cannot substitute for live restoration.
