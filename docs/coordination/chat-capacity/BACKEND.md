# Backend — chat capacity observability (temporary pilot)

**Protocol:** docs/coordination/CHAT_CAPACITY_OBSERVABILITY_20261116.md.
No trusted model-accessible lifetime token count or context-percentage API. Never claim precise usage or 50/75/85% thresholds without independently measured evidence. Conversation content, secrets and private user data are not logged here.

DateTime CET | Track | ChatGeneration | VisibleUserTurns | VisibleAssistantTurns | Visibility | MeasuredTextChars | MeasurementMethod | WorkBatchesKnown | LatestHandoffSHA | Event | Notes
--- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---
2026-11-16 evening | Backend | BACKEND-current (generation unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | a28dc8cb625aa4e8bfebf64934eea14e93a8e1b5 | CHECKPOINT | First role pickup; task from Architecture P1. P0 CI repair on two failing reward archive/category tests; exact context length unknown.

**Known hard-limit event:** none reported for current Backend chat. **Percent of capacity used:** unknown, unavailable; no measured evidence. **Continuity:** maintain docs/coordination/handoffs/BACKEND_LATEST.md after each substantial batch. No invented totals.
