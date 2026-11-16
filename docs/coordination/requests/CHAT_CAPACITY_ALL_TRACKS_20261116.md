# TEMP-CHATCAP-20261116 — Chat length measurement and durable handoff (all roles)

**Status:** READY_FOR_PICKUP (per role). **Priority:** Coordination P1, nonblocking for P0 BKL-002. **Decision:** user, 2026-11-16. **Owner:** Architecture coordinates; each role owns its own data. **Docs:** [measurement protocol](../CHAT_CAPACITY_OBSERVABILITY_20261116.md), [root rules](../../../AGENTS.md).

## Role-specific tasks
| Track | Own capacity log | Own rolling handoff | Next-pickup task |
|---|---|---|---|
| Architecture / Planning | `chat-capacity/ARCHITECTURE.md` | `handoffs/ARCHITECTURE_LATEST.md` | Set accepted measurement schema, check all roles' participation, consolidate only measured observations |
| Backend | `chat-capacity/BACKEND.md` | `handoffs/BACKEND_LATEST.md` | Record visible workload/counts if available; checkpoint each substantial implementation/QA packet; keep BKL-002 work P0 |
| Web | `chat-capacity/WEB.md` | `handoffs/WEB_LATEST.md` | New chat reads emergency handoff after hard limit, verifies current SHA, replaces transitional pointer with self-contained continuation; log every substantial batch |
| Android | `chat-capacity/ANDROID.md` | `handoffs/ANDROID_LATEST.md` | Log Android-specific long build/test output as measured only; keep current V49/QA and P0 stability work |
| Designer | `chat-capacity/DESIGNER.md` | `handoffs/DESIGNER_LATEST.md` | Log long image/design/audit turn counts and unknown volume correctly; continue V49 accessibility/test handoffs |
| QA / Testing | `chat-capacity/QA.md` | `handoffs/QA_LATEST.md` | Log own size + audit every track once per pickup, record evidence gaps without inventing percentages |
| Release / DevOps | `chat-capacity/RELEASE.md` | `handoffs/RELEASE_LATEST.md` | Log long Actions and infrastructure checks; optional read-only report aggregation; preserve CI/PROD rules |

## First acceptance
- [ ] Role acknowledged and created both files, **without inventing exact token limits**.
- [ ] Each substantive work batch appends one row with provenance and real rolling handoff SHA.
- [ ] Any chat hard-limit (like Web) recorded immediately by replacement/coordinator, with actual user report.
- [ ] QA/Architecture flags any handoff older than the latest substantive role batch, and routes an update.
- [ ] After several real completed conversations, compare empirical lower-bound sizes, then *propose* safe turnover policy. Without an actual complete export, remain `unknown`.
- [ ] Do not print false 50/75/85% warnings; only label estimates if data is available. Not a replacement for rolling handoff.

**No automatic role chat activation:** this request is read on next pickup. Do not pause or block BKL-002 P0 for this pilot. Never store content or identifying/sensitive metadata from conversations.
