# FamilyPoints — Priority assignment, 2026-10-26

User direction: economy v0.7 PRELIMINARILY APPROVED for DEV and BETA tuning; **BKL-002 selective family reset REQUIRED before NEXT BETA** to test starter filling. This supersedes earlier parked backlog. No actual reset of any existing family approved.

## P0 critical path
**Backend:** own BKL-002 safe server contract and implementation: preview affected categories/counts, protected identity and other-workspace boundaries, authorized explicit confirmation, atomic and idempotent reset/audit, setup-required state, Postgres tests. Confirm current exact-source CI; repair failed track first.
**Web:** after Backend contract add family-admin reset wizard and explicit destructive confirmation, then onboarding with 142 suggested TASK/REWARD templates/6 packs; select zero/one/many, import to private library, edit then separately activate. Keep stale async family data isolated.
**QA:** verify full reset→wizard→starter selection→private import→activation on a specifically authorized disposable DEV family; cross-tenant negatives, roles, repeat reset, ledger integrity, 4 currencies and TIME duration. Other existing families not to be reset.

## P1 concurrent
**Android:** emulator test revised template dialog («Читати повністю», theme), single **new** template import vs duplicate-only; after reset invalidate account-scoped cached state and offline queue so replay cannot reintroduce cleared data. Integrate approved 18 category icons after Media API stable.
**Designer + Backend:** implement preliminary economy v0.7 as optional editable recommendations based on difficulty independently from time. null price != 0/free, no retroactive balances, ledger or family template overwrite. Numeric calibration only after beta tests.
**Release:** publish next BETA after P0 reset and onboarding works, pinned all-green CI, scoped smoke, compatible schema/Web/APK and manifest check. Do not wait for exhaustive aesthetics QA; never run unapproved real-family reset.

## P2 parked after next BETA
Adult corrections BKL-003/005; timed reward countdown + 5/2/1 minute signals BKL-008; installed Android app as reward BKL-009; device management BKL-010; adult calendar BKL-011.

Every chat reports code SHA, actual tests and remaining blocker to its own log. This file coordinates through GitHub, not chat-to-chat messaging. Status: ASSIGNED, not delivered.
