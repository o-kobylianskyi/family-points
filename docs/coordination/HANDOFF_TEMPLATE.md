# FamilyPoints — next-chat handoff template

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
