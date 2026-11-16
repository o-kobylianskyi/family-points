# FamilyPoints Chat Capacity pilot — kickoff control snapshot

Date: 2026-11-16 (Europe/Berlin). Source: GitHub main inspection and user-provided Web hard-limit screenshot. **Status: initialized, not yet accepted by other role chats.** This is measurement only, not a reliable token % instrument.

| Role | Capacity log | Rolling handoff | Current acceptance |
|---|---|---|---|
| Architecture | NOT_CREATED | NOT_CREATED | PENDING next pickup |
| Backend | NOT_CREATED | NOT_CREATED | PENDING next pickup; BKL-002 P0 has priority |
| Web | `chat-capacity/WEB.md` created; one historical hard-limit event | `handoffs/WEB_LATEST.md` created as a transitional pointer + `20261116_WEB_CONTINUE_AFTER_LIMIT.md` substantive recovery | NEEDS NEW WEB CHAT OWNER ACK + self-contained rolling refresh |
| Android | NOT_CREATED | NOT_CREATED | PENDING next pickup |
| QA | NOT_CREATED | NOT_CREATED | PENDING next pickup |
| Designer | NOT_CREATED | NOT_CREATED | PENDING next pickup |
| Release/DevOps | NOT_CREATED | NOT_CREATED | PENDING next pickup |

The cross-track task is `requests/CHAT_CAPACITY_ALL_TRACKS_20261116.md` and root `AGENTS.md` includes it. Status here refers to file existence on GitHub at inspection, not to whether a chat was running or could read notifications. Never invent turn counts, characters or remaining context percentage. No user-facing full message logs in repository.

## Separate P0 observed CI failure
- Run [43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960) at `19adfe1b7`: Java Maven/Testcontainers **460 tests, 1 failure, 1 error, 0 skips** (458 pass). Reward obligation fixture expected count 1 vs 2; cross-database category restore test NullPointer in line 105. Actual Web, Android and Compose steps SUCCESS; full CI attestation FAIL due to Backend.
- Escalated in GitHub issue #15; Backend must diagnose and fix without disabling safety checks; QA retests exact patch. This is not a CI lookup bug.
- Later docs-only CI SUCCESS doesn't retroactively turn failed Maven tests green. Preserve BETA HOLD and HTTP423.

Next check: see whether roles create both files and update their latest handoffs per substantial batch; watch successful exact-SHA retest in GitHub Actions.
