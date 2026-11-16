# Web — chat capacity observation log

See [pilot protocol](../CHAT_CAPACITY_OBSERVABILITY_20261116.md). Measurements are never inferred from hidden context token usage.

| Date/time CET | Track | ChatGeneration | Visible user turns | Visible assistant turns | Visibility | Measured text chars | Method | Known batches | Latest handoff SHA | Event | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-16 ~21:11 | Web | WEB-old, exact generation unknown | unknown | unknown | NOT_AVAILABLE | unknown | USER_UI | unknown | 6d84766f | HARD_LIMIT_BANNER | User screenshot proves maximum conversation length; user reported no earlier warning; no known exact capacity. |

Next Web chat creates new generation and records measured checkpoints; this row is historical evidence only.
| 2026-11-16 ~21:25 | Web | WEB-02 | 1 | unknown | PARTIAL_VISIBLE | unknown | USER_UI | 1 | bc84f6ef | NEW_CHAT | New role pickup from user screenshot; source audit and self-contained handoff committed. No authoritative token percentage. |
| 2026-11-16 | Web | WEB-02 | 2 | unknown | PARTIAL_VISIBLE | unknown | NONE | 2 | 8c566da2 | CHECKPOINT | Source invariant test batch committed; accurate lifetime/context token count unavailable. |
| 2026-11-16 | Web | WEB-02 | 3 | unknown | PARTIAL_VISIBLE | unknown | NONE | 3 | 4c6277ba | CHECKPOINT | Cross-tab source and test handoff batch; context capacity unknown. |
| 2026-11-16 | Web | WEB-02 | 4 | unknown | PARTIAL_VISIBLE | unknown | NONE | 4 | 581ba39f | CHECKPOINT | Two-tab synthetic suite committed; hidden context utilization unknown. |
| 2026-11-16 | Web | WEB-02 | 5 | unknown | PARTIAL_VISIBLE | unknown | NONE | 5 | 2b7601ac | CHECKPOINT | Historical CI steps checked for three known runs; exact latest Playwright status unknown. |
| 2026-11-16 | Web | WEB-02 | 6 | unknown | PARTIAL_VISIBLE | unknown | NONE | 6 | cb2e2067 | CHECKPOINT | Readiness evidence audit committed; no exact context usage measurement. |
