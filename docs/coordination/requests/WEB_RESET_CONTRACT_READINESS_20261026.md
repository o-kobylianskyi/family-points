# Web — selective family reset integration contract / release gate (2026-10-26)

**Priority:** P0 per REVISED_BETA_SCOPE_20261026; supersedes older freeze. **Status:** waiting for Backend/Architecture concrete versioned API. **Environment:** DEV only. No reset operations authorized/executed.

## Required API before enabling destructive UI
1. **Permission and workspace ownership:** GET capability read for the current authenticated workspace; Backend must authorize reset independently of Web's MANAGE_WORKSPACE or ADMIN_OVERRIDE.
2. **Preview (dry-run):** returns previewId/nonce, workspaceId, workspaceDataGeneration, categories with counts, selectable vs mandatory dependency categories, blocks/retained data and expiry. The API must explicitly document empty selection behavior.
3. **Challenge/reauth:** Backend-authenticated fresh credential or equivalent security challenge; never store passwords/challenges in local storage, URL or logs.
4. **Execute:** send workspaceId, exact preview nonce/revision, explicitly accepted categories, idempotency UUID and fresh authorization; reject stale preview, changed category dependencies and cross-workspace mismatch. Return an audited operation result and fresh data generation.
5. **Post-reset:** preserve original global account, workspace ID, owner rights, other families and global catalog; invalidate client workspace-scoped caches/offline operations, reload profile, then open existing family setup/catalog-selection flow with zero selection allowed.
6. **Failure/replay semantics:** never retry destructive operations with a new key automatically. Handle pending, conflict, expired challenge and partial backend errors explicitly.

## Web UX staging
- Entry: Settings > Family administration > Reset (visible only after capability from Backend, not from speculative frontend permission check).
- Initially **no categories selected**. Explain defaults: preserve family owner/account, other families, global catalog and server audit. Every destructive category shows count, retained/dependency effects and warning.
- User explicitly selects categories; request dry-run preview; if Backend expands dependencies, require user to review again, not silently add clears.
- Require typed family name plus server-verified fresh auth; separate final confirmation, with no auto-run on navigating/setup.
- On successful response, clear family-scoped UI state and return to existing onboarding/142-template 6-pack catalog. Import copies to private library only, activation remains separate.
- Four locales UK/RU/DE/EN and both themes, narrow/mobile layouts; all failures localized and no raw Backend traces exposed.

## Release checklist / QA ownership
- Backend contract and integration tests first: account/owner and family B survives; no orphan ledger/FK records; server-enforced generation invalidates offline replay; scoped audit; idempotent repeat.
- Web: preview counts and category dependency consistency, zero-selected/reset cancel, negative permissions, nonce expiration, 2-family switch during preview/execute, keyboard/mobile, language switch, retry/reload, first-run empty/all/some templates.
- QA: perform destructive tests only on a disposable workspace with explicit permission; **no live/production family deletion**.
- Release: pin Backend/Web/APK compatible SHA and verify migration/backup and test fixture. No BETA publication implied.

## Current blocker
As inspected on current main, no Backend family-reset endpoint/controller was found. Do not add a fake active reset button or invent request payloads. Web component/API binding follows after Backend publishes the exact contract.
