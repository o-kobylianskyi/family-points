# Designer → Architecture / QA — current starter-pack baseline correction

**Date:** 2026-10-27. **Type:** source-based QA coordination; **not** runtime deployment. Current catalog: `src/main/resources/templates/catalog-v1.json`.

## Correction
Architecture handoff `ARCHITECTURE_CHAT_HANDOFF_20261027.md` still says 65 entries outside packs. That is **superseded** by Designer's complete recomputation after pack curation commit `657554843d`: **142 catalog codes; 99 in at least one of 6 packs; 43 outside packs (23 TASK, 20 REWARD)**. No template removal; outside-pack codes remain individually discoverable. Exact per-code matrix: `docs/design/economy/STARTER_PACK_COVERAGE_CURRENT_20261027.json` commit `e0e3ff63`. Pack lengths: 32/32/36/33/16/20.

## QA
- TPL-03A must use latest pack membership, not prior early_school==mixed_age (old 32 shared) assumption.
- For early_school+mixed_age current intersection **21**, union **44**. Private import should never create duplicates, activate tasks/rewards, or alter ledger.
- Cross-check latest deployed backend SHA before checking HTTP count, and use explicitly authorized disposable workspace for mutations; no family reset permitted from this note.
- Designer ECONOMY_FINAL_STATIC_AUDIT_20261027.md confirms 110 task and 32 reward suggestions, 23 non-null optional amounts, 119 null, zero missing/orphan codes.
- 18 approved SVG in Backend source; Web partially integrated, Android on task/reward screens needs real device/emulator proof.

## Owner next
Architecture/QA acknowledge revised 43 count in their status/checklists; Backend/Web/Android/QA independently complete current BETA P0 reset contract and targeted E2E. Designer does not alter seed amounts or family data.
