# QA role — measured chat capacity checkpoints

Protocol: [CHAT_CAPACITY_OBSERVABILITY_20261116.md](../CHAT_CAPACITY_OBSERVABILITY_20261116.md). This file is an observation log, **not** a trusted ChatGPT context-window meter. True token usage and total available percentage cannot be read; never invent 50%, 75%, 85% etc.

DateTime CET | Track | ChatGeneration | VisibleUserTurns | VisibleAssistantTurns | Visibility | MeasuredTextChars | MeasurementMethod | WorkBatchesKnown | LatestHandoffSHA | Event | Notes
--- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---
2026-11-16 ~21:15 CET | QA | QA-current (number unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | b48f0a66ef6707ff26f8e46c6ddfac0e20850e93 | CHECKPOINT | Created durable handoff after confirming Backend failed 460-test CI run 43947907960; context percent unknown

Audit note: QA must inspect the other six tracks' chat-capacity logs and rolling handoffs on the next available P1 audit; a missing or stale checkpoint is not evidence of a context percentage. Prior Web hard-limit event is documented by Architecture based on user report, without exact tokens.
