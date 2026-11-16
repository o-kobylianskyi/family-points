# FamilyPoints — next-chat handoff template


## Mandatory early handoff for context-capacity risk (2026-11-16)

See [root AGENTS.md](../../AGENTS.md) for the cross-chat rule. Warn at **estimated** 50%, 75%, 85%, 90%, 95%, then every 5%; no authoritative usage counter is exposed to the chat. Never fabricate a precise percentage or claim uninterrupted monitoring. At approximate 75%, start the durable handoff; at approximate 85%, finalize/commit it and recommend a new chat. If context becomes unreliable before that, transfer immediately.

For continuation of the **same track**, publish under `docs/coordination/handoffs/YYYYMMDD_<track>_CONTINUE.md` or append track journal with a stable link. A good handoff is self-contained, versioned and uses this checklist:

- [ ] Role/chat and exact next-chat kickoff text.
- [ ] Verified repo `o-kobylianskyi/family-points`, `main` SHA/date and other tracks' concurrent changes.
- [ ] Goal, current critical path, owner and approved scope.
- [ ] Completed code/commits vs drafts/proposals vs known broken or blocked work.
- [ ] Real test command, environment, results PASS/FAIL/SKIP, CI run URL and whether jobs were **executed or selected/skipped**.
- [ ] Open GitHub issues, XTR/IMP/ADR dependencies and roles awaiting acknowledgment.
- [ ] BETA HOLD, BKL-002 destructive scope, data-loss and permission safeguards.
- [ ] Next 3-5 prioritized actions and known first step to resume without repeated discovery.
- [ ] No passwords, tokens, signing keys, child data, private QA credentials or unapproved reset instructions.

**Ready-to-paste new-chat opener:**

```text
Продовжуємо FamilyPoints, напрям: <TRACK>.
Прочитай AGENTS.md та docs/coordination/handoffs/<FILE>.md в GitHub
o-kobylianskyi/family-points; перевір актуальний origin/main SHA, рольовий
журнал, останні CI та відкриті задачі. Віднови роботу з пріоритетів у handoff.
Дотримуйся правила орієнтовних попереджень про контекст 50/75/85/90/95%,
не видавай їх за точний системний лічильник. Нічого не деплой і не запускай
руйнівні тести без окремого дозволу.
```

## Handoff to a different work direction (XTR)

This is different from continuing the **same** track in a new conversation. When work belongs to another track, the initiating chat saves one self-contained request file under `docs/coordination/requests/XTR-YYYYMMDD-<origin>-NN.md` based on [TASK_TEMPLATE.md](requests/TASK_TEMPLATE.md), and links it to the user.

The receiving chat verifies current Git HEAD, reads the specification and related IMP/ADR references, acknowledges or blocks it, and executes only its assigned ownership. For multi-track work, create linked requests with dependency order. Never assume two live chats share context or that saving a request sends a notification.


Transfer a track by supplying this text **after** committing its journal. New chat must fetch Git, not trust the summary as a live state.

```text
Project: FamilyPoints
Track: Architecture / Planning | Backend | Web | Android | Release / DevOps
Repository: o-kobylianskyi/family-points
Current working branch: <verified branch>
Last verified remote commit SHA and date: <SHA, date>
Task / scope: <what is being done>

FIRST: read AGENTS.md and docs/coordination/README.md, PROJECT_STATE.md,
IMPACT_REGISTER.md, ARCHITECTURE_DECISIONS.md, CHANGE_PROTOCOL.md and
docs/coordination/logs/<my-track>.md. Then inspect root historical
journals/checklists, current source, and commits since last reviewed SHA,
including changes made by other tracks.

Completed / exact commit and files: ...
Tests, CI, server, device verification ACTUALLY done: ...
Blockers and risks: ...
Cross-track impacts (IMP IDs; reviewed vs still pending): ...
Decisions (ADR IDs): ...
Next actions by priority: ...

END: update track log, impacted shared docs/checklist, commit, and hand off
the actual SHA. Do not deploy unless Release has approved its gate.
```
