# QA role — measured chat capacity checkpoints

Protocol: [CHAT_CAPACITY_OBSERVABILITY_20261116.md](../CHAT_CAPACITY_OBSERVABILITY_20261116.md). This file is an observation log, **not** a trusted ChatGPT context-window meter. True token usage and total available percentage cannot be read; never invent 50%, 75%, 85% etc.

DateTime CET | Track | ChatGeneration | VisibleUserTurns | VisibleAssistantTurns | Visibility | MeasuredTextChars | MeasurementMethod | WorkBatchesKnown | LatestHandoffSHA | Event | Notes
--- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---
2026-11-16 ~21:15 CET | QA | QA-current (number unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | b48f0a66ef6707ff26f8e46c6ddfac0e20850e93 | CHECKPOINT | Created durable handoff after confirming Backend failed 460-test CI run 43947907960; context percent unknown

Audit note: QA must inspect the other six tracks' chat-capacity logs and rolling handoffs on the next available P1 audit; a missing or stale checkpoint is not evidence of a context percentage. Prior Web hard-limit event is documented by Architecture based on user report, without exact tokens.

## Initial cross-track read-only audit (2026-11-16, current accessible GitHub state)
- WEB: capacity log EXISTS, rolling WEB_LATEST EXISTS.
- DESIGNER: capacity log EXISTS, rolling DESIGNER_LATEST EXISTS.
- ARCHITECTURE, BACKEND, ANDROID, RELEASE: both expected paths NOT_FOUND as of this audit. This records a pickup/participation gap, **not** a refusal, context count or evidence that chats have read the coordination task.
- QA: QA capacity log and QA_LATEST created in commits `6962029` and `b48f0a6`.
- Next P1 audit checks these missing files after other tracks have their next pickup; do not block current Backend P0 CI regression remediation.

2026-11-16 evening CET | QA | QA-current (number unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 66e5e5dfba5c385c751abb030219c65fcc307e6b | CHECKPOINT | Backend CI repair packet #12 reviewed; no independent current-SHA PASS observed; context percentage unknown

2026-11-16 evening CET | QA | QA-current (number unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | cf1590183a372bb073d373f63b637ac15693db0b | CHECKPOINT | QA-013: packet 12 exact-SHA CI unconfirmed; inspected requirement cross-DB source and updated automated QA inventory to 18 classes; true context percentage unknown

2026-11-16 ~22:41 CET | QA | QA-current (generation unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | e4c5ecf44bc17daa982727efc7bb5533c7a33c45 | CHECKPOINT | QA-014: real CI 43953661904 and 43953986968 FAIL; Java actual 460/461 cases with one failure, Compose parser red; operator manual tests deferred; actual context percentage unavailable

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 76acb61987b2c63ac99d5bdeaaafebcecc4114df | CHECKPOINT | QA-015 two P0 defects still present in source; accepted Backend #14 into 19-class automatic queue and Web two-tab synthetic test, no manual live tests; context percentage unknown

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | eb52be667ed6d643a564f8a362a39765d25c64dc | CHECKPOINT | QA-017: packet 15 reviewed; release SHA PowerShell parser still red in source; no post-fix all-component CI; real tests deferred; capacity unknown

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 0adf386e7e1e08358e787ccb7c322c660b6309aa | CHECKPOINT | QA-018: authenticated POINT_TYPES archive reviewed in code; Backend #16 requested; no newest exact SHA Maven/Compose PASS verified; manual tests deferred; context percentage unknown

2026-11-16 night CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 04deda45e8914ab0a89ff7ffa08d10e89e1ec328 | CHECKPOINT | QA-019 formally read backend packet #16 and negative inspector test, source only no CI PASS; Release PS1 parser still incomplete; live PRE-BETA deferred; true context percentage unknown

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | QA-020 rolling handoff | CHECKPOINT | Confirmed release PowerShell parser malformed, mandatory compose CI step, latest source CI lookup blocked; context percentage unknown

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 69f63a512c73a2d0253aa10856c8e871b982d1f0 | CHECKPOINT | QA-021 source audited BKL002 #17 TASK ancestry read-only verifier, 21 unique tests queued, no post-fix exact SHA CI run, release PS1 parser still malformed; actual chat percent unknown

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | dd7acf62e006ade3dbc32869f4120d471cf87e63 | CHECKPOINT | QA-022 verified Release PowerShell SHA regex, clean-worktree fences and removal duplicated block in source; post-repair CI not independently observed; 21 BKL002 synthetic classes pending, manual campaign deferred; actual chat percentage unknown

2026-11-16 23:47 CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 5a188c41182949f21dd83c1829040df89c30a53f | CHECKPOINT | QA-023 Backend purchase ancestry #18 source reviewed; 21 classes, new-source Maven/CI not independently verified; Release diagnostics improved, real PREBETA deferred; actual context percent unknown

2026-11-16 23:47+ CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 80d9b51e26c3f26aeafdd7c235da15aade8aeee3 | CHECKPOINT | QA-024 exchange two-leg read-only PG proof source reviewed, one new regression class 22 total, new run exact SHA not verified, real PRE-BETA deferred; capacity percentage unavailable

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 07473888c47f0246975609854e8bba37bd3cdd82 | CHECKPOINT | QA-025 found exchange request snapshot ratio gap in source auditor, routed issue15; release strict SHA JSON checker sources reviewed 18 tests NOT_RUN, latest full CI unverified; actual chat percentage unavailable

2026-11-16 late CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | bc35f1acfc173f277a749cf076c6c1c332a2c0ab | CHECKPOINT | QA-026 Backend signed exchange rate/request graph #20 reviewed, 23 BKL002 distinct QA classes queued, source DB ratio gap routed to owner, post-fix exact SHA CI still lookup blocked, no real tests, chat percent unknown

2026-11-17 08:43 CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 264c17d695cabe9307f8b6908e34887d2e30bd16 | CHECKPOINT | QA-027 reviewed Backend #21 original rate fix #22 2-PG atomic rollback, 25 classes pending actual Maven; Release 26/26 isolated copies only, latest cloud full CI unavailable, actual context percentage unknown

2026-11-17 morning CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 56da9b3c9059464628284f7e0b52994cb853dd06 | CHECKPOINT | QA-028 backend BKL002 #23 multi-exchange rollback source reviewed, 26 unique classes queued, actual Testcontainers PASS absent and post-fix full GitHub CI lookup blocked; user real campaign deferred, actual context percent unknown

2026-11-17 morning CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | bf79fd1dec94cc44f9bcbd4cb1dfac18be298517 | CHECKPOINT | QA-029 independently verified GH Actions #44044271962 @8c397d0 exact four non-skipped jobs 479 Maven zero fail/skip and Release 55 safety cases, source BKL002 #24 semantic SHA, BETA HOLD, actual chat percentage unknown

2026-11-17 09:50 CET | QA | QA-current | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 6e16b1c5afdd90509ecf1b843bfdcf39d4902057 | CHECKPOINT | QA-030 source-reviewed post-green #25 two cross-category negatives; latest full four-component pass pinned 8c397d0 predates new test; V49 Playwright two-tab not included; 26 unique backend classes; no real E2E, actual context occupancy unavailable
