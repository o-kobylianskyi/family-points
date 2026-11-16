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
