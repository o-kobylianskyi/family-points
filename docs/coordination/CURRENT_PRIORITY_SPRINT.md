# FamilyPoints — Coordinated priority sprint 2026-10-26

**Coordinator:** Architecture. **Baseline observed:** `6fe964f725d7369c1777b76693ddfa7895b37cd7`. **CI:** [run 40389295693](https://github.com/o-kobylianskyi/family-points/actions/runs/40389295693) failed Java 207 tests: `WorkspaceTemplateControllerTest.designerTimeRewardDurationMapsToCreationDto` expected 30 minutes, actual 0; Web, Android, Compose passed. Recheck latest main/CI when accepting; don't assume stale failure remains.

## Priority order
- **P0 / NOW / Backend (owner):** fix designer TIME reward duration field 30→0, add regression for nested seed and creation DTO, green CI. QA verify and Web/Android consume corrected contract.
- **P0 / NOW / QA:** targeted test of template import/draft/activation, exact-once, family isolation; BKL-007 standard currencies backfill verified selectively, finish missing cases.
- **P1 / after green CI / Backend+Web+Android:** stabilize 142 seeded templates (110 TASK, 32 REWARD), six packs, localized descriptions, private library selection and editable creation; actual DEV UI/phone smoke; avoid inventing template activation successes without live tests.
- **P1 / after candidate / Release:** pin SHA, deploy synchronized Backend/Web seed V39/V40 and matching compatible Android candidate, smoke then publish next BETA. No full-QA gate, no automatic destructive reset.
- **P1 / Designer:** content QA for 142 curated seeds, 6 packs, TIME duration and translations; check 65 unassigned-to-pack templates as design choice, not automatic defect; prepare content corrections.
- **P2 / parked:** BKL-002 reset, BKL-003/005 audit corrections, BKL-008 5/2/1 countdown signals, BKL-009 package-linked launch, BKL-010 managed device access, BKL-011 events, unless user explicitly changes priorities.

## Work rules
1. Exactly one P0 code producer for the current red CI failure: Backend. Other teams may inspect and prepare targeted verification but avoid competing modifications to same files.
2. QA is tester, not a gate that blocks publishing until all tests; critical privacy/ledger/auth/startup bugs do block.
3. Designer seed content is distinct from importing data into user-family workspaces. Do not auto-create historical executions, purchases or ledgers.
4. Each owner writes commit/CI/result to its existing log, one coherent change package per batch; Architecture refreshes this sprint plan after each completed critical package.
5. STOP: no broad feature expansion or data deletion; user tolerates disposable synthetic data but hasn't ordered reset.
