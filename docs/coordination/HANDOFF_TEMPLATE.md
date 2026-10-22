# FamilyPoints — next-chat handoff template

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
