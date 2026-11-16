# FamilyPoints Web — rolling latest handoff

**Initial recovery baseline:** [2026-11-16 Web hard-limit recovery](20261116_WEB_CONTINUE_AFTER_LIMIT.md) (committed `6d84766f`). **Status:** transitional pointer, not a claim that new Web chat has reviewed or updated the handoff.

Web role on next active pickup MUST replace this pointer with its own self-contained latest state, current verified `main` SHA, exact test/CI evidence, next priority and a new-chat opener. Also append measurement checkpoint to [Web capacity log](../chat-capacity/WEB.md). Follow [capacity pilot](../CHAT_CAPACITY_OBSERVABILITY_20261116.md).

Until that pickup, use the recovery handoff, `docs/coordination/logs/web.md`, Designer/QA acceptance packs, and GitHub issue #18. Real V49 acceptance QA-owned; no BKL-002 destructive execute, no PROD deployment.
