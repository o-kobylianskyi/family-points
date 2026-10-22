# FamilyPoints — cross-track technical requests (XTR)

This directory is the durable handoff queue between the five direction-specific development chats, **not** an implementation backlog replacing each track's journal or the cross-track impact register.

## Rules
1. A chat plans its own direction autonomously. When another track must implement part of the work, the initiating chat creates a complete, self-contained technical assignment here.
2. Filename/ID: `XTR-YYYYMMDD-<origin>-NN.md`; origin codes: `P` Architecture, `B` Backend, `W` Web, `A` Android, `R` Release. Avoid collisions by checking existing IDs before creating; never overwrite someone else's request.
3. **One accountable receiving direction** per XTR. For multi-track changes, create related XTRs and mark prerequisite links; Architecture only when a shared architecture decision is required.
4. Each request has owner, current status, implementation scope, reference SHA, dependency IDs, acceptance criteria and verification requirements. Use [TASK_TEMPLATE.md](TASK_TEMPLATE.md).
5. Initiator commits the XTR, logs it in the originating track journal, and provides the user a link/clear target-chat handoff. The receiving chat must read Git on pickup; **there is no automatic inter-chat notification**.
6. Receiver marks `ACKNOWLEDGED` or `NEEDS_CLARIFICATION` / `BLOCKED`; later `IN_PROGRESS`, `READY_FOR_REVIEW`, then `DONE` after evidenced acceptance. Acknowledge does not mean implemented, tested or deployed.
7. Use `IMP-XXXX` as well when contract changes affect other tracks, including API, DTO, permissions, DB, Android sync, and deployment. An XTR is about accountability/action; an IMP is about integration impact.
8. Preserve history and link implementing commits and test evidence. In concurrent editing, work on topic branches/PRs or re-read HEAD; do not force push.

## 2026-10-23 — Media execution packet (initiated by Architecture P)
- **B:** [XTR-20261023-P-01](XTR-20261023-P-01.md) — finish existing Backend media PR #5, object-level security, catalog, API DTO, V35/V36 compatibility. Existing XTR-20261022-W-01 retains its original requirements.
- **W:** [XTR-20261023-P-02](XTR-20261023-P-02.md) — static approved text-free illustrations can be completed immediately; later private catalog/upload UI after Backend consumer approval.
- **A:** [XTR-20261023-P-03](XTR-20261023-P-03.md) — native Compose preview/picker/cache; maintain current styling priority and separate APK safety gate.
- **R:** [XTR-20261023-P-04](XTR-20261023-P-04.md) — read-only backup/schema/rollout audit; actual deployments require separate explicit approval.

Read each request's own latest status and the cross-track register: this section is a routing index, **not** an auto-delivery or evidence of completed work.
