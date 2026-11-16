# Backend — chat capacity observability (temporary pilot)

**Protocol:** docs/coordination/CHAT_CAPACITY_OBSERVABILITY_20261116.md.
No trusted model-accessible lifetime token count or context-percentage API. Never claim precise usage or 50/75/85% thresholds without independently measured evidence. Conversation content, secrets and private user data are not logged here.

DateTime CET | Track | ChatGeneration | VisibleUserTurns | VisibleAssistantTurns | Visibility | MeasuredTextChars | MeasurementMethod | WorkBatchesKnown | LatestHandoffSHA | Event | Notes
--- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---
2026-11-16 evening | Backend | BACKEND-current (generation unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | b2a77a8b1ff17f124d0578c1cd92ec00f7e93412 | CHECKPOINT | First role pickup; task from Architecture P1. P0 CI repair on two failing reward archive/category tests; exact context length unknown.

2026-11-16 later | Backend | BACKEND-current (generation unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | ab4eec812d8222b05db994da1970b3f2008bc8bc | CHECKPOINT | Source-only native requirement replay #13, two-PG test and QA handoff; total context capacity not exposed.\n
2026-11-16 22:41+ CET | Backend | BACKEND-current (generation unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | aa12bd082e8cec82ee9f6ca045eb276f197f01b5 | CHECKPOINT | Native obligation replay QA #14 source/test changes and rollback fixture; context usage percentage unmeasurable.\n
2026-11-16 late CET | Backend | BACKEND-current (generation unknown) | unknown | unknown | PARTIAL_VISIBLE | unknown | NONE | unknown | 79b44b1fc72926b790020237f93fdff7522b8e6c | CHECKPOINT | Manual point ledger QA #15, actual CI scope failure repair; no valid remaining-token percentage observable.\n
**Known hard-limit event:** none reported for current Backend chat. **Percent of capacity used:** unknown, unavailable; no measured evidence. **Continuity:** maintain docs/coordination/handoffs/BACKEND_LATEST.md after each substantial batch. No invented totals.
