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

| 2026-11-18 | Web | WEB-02 | 21 | unknown | PARTIAL_VISIBLE | unknown | NONE | 21 | 5e51df1b | CHECKPOINT | V49 Chromium fixture blocks service workers for reliable route interception; CI pending. |

| 2026-11-18 | Web | WEB-02 | 22 | unknown | PARTIAL_VISIBLE | unknown | NONE | 22 | 91cb38c3 | CHECKPOINT | Browser mock guard fails closed for any unexpected post-login writes; 6 simulated routes PASS. |

| 2026-11-18 | Web | WEB-02 | 23 | unknown | PARTIAL_VISIBLE | unknown | NONE | 23 | 112bd51c | CHECKPOINT | Three persistent native Node tests added for V49 mock route safety; CI exact run pending. |

| 2026-11-18 | Web | WEB-02 | 24 | unknown | PARTIAL_VISIBLE | unknown | NONE | 24 | ffab2b54 | CHECKPOINT | Fixed missing-markers cleanup race, V8 regression 15/15 PASS; CI/Chromium pending. |

| 2026-11-18 | Web | WEB-02 | 25 | unknown | PARTIAL_VISIBLE | unknown | NONE | 25 | 63466faa | CHECKPOINT | Added third V49 mocked Chromium case: missing both markers blocks completion; CI pending. |

| 2026-11-18 | Web | WEB-02 | 26 | unknown | PARTIAL_VISIBLE | unknown | NONE | 26 | 50267201 | CHECKPOINT | Added fourth Chromium positive recovery case for original session marker remaining, browser pending. |

| 2026-11-18 | Web | WEB-02 | 27 | unknown | PARTIAL_VISIBLE | unknown | NONE | 27 | 53ba2468 | CHECKPOINT | Fifth V49 Chromium case verifies positive recovery with shared marker only; browser pending. |

| 2026-11-18 | Web | WEB-02 | 28 | unknown | PARTIAL_VISIBLE | unknown | NONE | 28 | 44bf33fe | QA_BLOCKED | Pinned 5-case Chromium request issue #18 comment 6976230912; no source mutation, browser unverified. |

| 2026-11-18 | Web | WEB-02 | 30 | unknown | PARTIAL_VISIBLE | unknown | NONE | 30 | 8766fec9 | WEB_CI_PASS_QA_BLOCKED | Zero lint warnings / 37 native Node PASS at 05dea312, Chromium still NOT_RUN. |

| 2026-11-18 | Web | WEB-02 | 31 | unknown | PARTIAL_VISIBLE | unknown | NONE | 31 | 843fe643 | SOURCE_PATCH_CI_PENDING | Align username fallback remount to backup request identity; native Node test added; QA Chromium blocked. |

| 2026-11-18 | Web | WEB-02 | 32 | unknown | PARTIAL_VISIBLE | unknown | NONE | 32 | 6cc24e98 | WEB-01_READY_FOR_QA | Pinned 39/39 Node and 6/6 V49 syntax CI #44366630311, Chromium 5 NOT_RUN; Issue #15 comment 6987315284, next WEB-02; BETA HOLD. |

| 2026-11-18 | Web | WEB-02 | 33 | unknown | PARTIAL_VISIBLE | unknown | NONE | 33 | 6dfbc802 | WEB-02_READY_FOR_QA | Pinned Web scoped CI 40/40 Node and 6 V49 syntax PASS; Issue #15 6991053477; Chromium NOT_RUN; next WEB-03 only confirmed defects; BETA HOLD. |


| 2026-11-21 | Web | WEB-02 | 34 | unknown | PARTIAL_VISIBLE | unknown | NONE | 34 | 5c50a89f | WEB_POINTS_SOURCE_CI_PASS | User local Points page 4-item report; 44/44 Node PASS, 6 V49 syntax, 0 lint warnings; localhost browser NOT_RUN; backend negative-spend contract unchanged. |
