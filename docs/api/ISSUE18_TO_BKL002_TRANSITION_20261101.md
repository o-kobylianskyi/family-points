# Issue #18 handoff → BKL-002 / P0

## Onboarding (Issue #18)
Code baseline at transition: full Spring/PostgreSQL+Web+Android+Compose CI all success on eaf359e7 (run 41296671660). Implemented V46 wizard state, explicit WorkNode GROUP, V47 private import receipts, V48 active TASK/REWARD receipts, V49 finalize-import, actual 142 catalog PostgreSQL E2E and security filter tests. Setup remains opt-in via feature flag (default false); no historical-family auto-enrollment and no automatic active task/reward creation.

**Not signed off:** QA has recorded 29 passing browser checks and three Test/Reset environment blockers; full real JWT manager/child 403 integration and Web/Android first-family user journey are not signed off. Do not close Issue #18, enable first-run flag in BETA, or claim all UI workflows complete without owners' acceptance. Other-track fixes may land concurrently and require fresh pinned CI.

## BKL-002 next P0
1. New manager-only GET `/workspaces/{id}/data-reset/dependency-audit`: real PostgreSQL FK per column, delete action and protected-CASCADE exposure. Read-only, no-store; never executable.
2. Resolve selective deletion targets and FK closure against V49 schema, preserving families, accounts, membership, other workspaces, point/reputation transactions, balances and global catalog. Confirm whether definitions still referenced by ledgers can be deleted; reject destructive plans until policy approved.
3. Complete fresh reauth for both local-password and Google-only accounts, one-time challenge consumption and actor binding.
4. Build atomic scoped execute with family lock, dataEpoch increment, audit receipt and repeat/replay; cover concurrent writes/other families and injected failure with disposable PostgreSQL.
5. Independently accept Web/Android stale-offline protection, QA disposable family end-to-end and Release pinned CI/backups before any BETA enablement.

**Do not interpret schema introspection as permission to delete.** Existing execute remains HTTP 423; do not run reset, publish destructive endpoint, enable flag, or deploy against real families before approval.
