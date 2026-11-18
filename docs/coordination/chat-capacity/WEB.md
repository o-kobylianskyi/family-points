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
| 2026-11-17 | Web | WEB-02 | 8 | unknown | PARTIAL_VISIBLE | unknown | NONE | 8 | b447c05d | CHECKPOINT | Corrected source-level regex escaping in two-tab Playwright; browser execution still unknown. |

| 2026-11-17 | Web | WEB-02 | 9 | unknown | PARTIAL_VISIBLE | unknown | NONE | 9 | 4aff7b9a | CHECKPOINT | Source syntax-only PASS, Chromium NOT_RUN, context size unknown. |
| 2026-11-17 | Web | WEB-02 | 10 | unknown | PARTIAL_VISIBLE | unknown | NONE | 10 | 75f8773c | CHECKPOINT | New V49 missing-scope guard; source-level 10/10 V8 PASS, full runner unknown. |
| 2026-11-17 | Web | WEB-02 | 11 | unknown | PARTIAL_VISIBLE | unknown | NONE | 11 | 0a23150d | CHECKPOINT | 14 actual JS safety test callbacks PASS V8; native Node and browser gates pending. |
| 2026-11-17 | Web | WEB-02 | 12 | unknown | PARTIAL_VISIBLE | unknown | NONE | 12 | 9cb86342 | CHECKPOINT | V49 state scope tightened, V8 source-backed 14/14 PASS; Chromium and new CI pending. |
| 2026-11-18 | Web | WEB-02 | 13 | unknown | PARTIAL_VISIBLE | unknown | NONE | 13 | d5134ed2 | CHECKPOINT | Review state scope+IN_PROGRESS guard, V8 source backed 14 PASS; Chromium pending. |

| 2026-11-18 | Web | WEB-02 | 14 | unknown | PARTIAL_VISIBLE | unknown | NONE | 14 | d2c7fe0c | CHECKPOINT | Added syntax check to Web CI tests, execution pending. |

| 2026-11-18 | Web | WEB-02 | 15 | unknown | PARTIAL_VISIBLE | unknown | NONE | 15 | b04dd58e | CHECKPOINT | Verified actual GitHub Web CI syntax 6/6 and Node 33/33; Chromium not executed. |

| 2026-11-18 | Web | WEB-02 | 16 | unknown | PARTIAL_VISIBLE | unknown | NONE | 16 | f5ee630b | CHECKPOINT | Added original UUID assertions to both V49 receipt mocks; V8 syntax PASS, Chromium pending. |
| 2026-11-18 | Web | WEB-02 | 17 | unknown | PARTIAL_VISIBLE | unknown | NONE | 17 | 3375599b | CHECKPOINT | Prior exact SHA Web CI 6 syntax+33 Node PASS; added 404/foreign marker persistence assertions. |

| 2026-11-18 | Web | WEB-02 | 18 | unknown | PARTIAL_VISIBLE | unknown | NONE | 18 | 23cf078e | CHECKPOINT | Moved route-handler assertions into test for deterministic receipt UUID failures; queued CI. |

| 2026-11-18 | Web | WEB-02 | 19 | unknown | PARTIAL_VISIBLE | unknown | NONE | 19 | 8489a22b | CHECKPOINT | QA route guard rejects unexpected workspace writes; 5 simulated outcomes PASS; browser pending. |

| 2026-11-18 | Web | WEB-02 | 20 | unknown | PARTIAL_VISIBLE | unknown | NONE | 20 | b75939ad | CHECKPOINT | V49 browser fixture asserts finalize revision and epoch headers, CI pending. |
