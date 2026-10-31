# FamilyPoints — Templates API handoff (Backend → Web / Android / QA)
Date: 2026-10-26. Scope: DEV, not a BETA deployment.
Status: **BACKEND CI PASS; DEV HTTP / cross-family QA PENDING — activation requires further hardening**.

## Contract currently implemented
All routes are prefixed with `/workspaces/{workspaceId}/templates`; private-library endpoints require `canManageWorkspace(workspaceId)`. The global catalog endpoint is `GET /template-catalog`.

| Method | Suffix | Purpose |
| --- | --- | --- |
| GET | / | List only this workspace's private template definitions |
| POST | / | Save a user-edited private template (`title`, `kind`, `definitionJson` string containing a JSON object) |
| PUT | /{templateId} | Update private definition |
| DELETE | /{templateId} | Remove private definition |
| POST | /imports/preview | Validate `selectedCodes`, report `alreadyImported` / `toImport` |
| POST | /imports | Copy selected global catalog definitions to private library; **does not create tasks or rewards** |
| POST | /from-tasks/{taskId} | Save scoped task definition as private template |
| POST | /from-rewards/{rewardId} | Save scoped reward definition as private template (complex weekly windows unsupported) |
| GET | /{templateId}/instantiate-preview | Read template kind/title/raw definition and selection indicators |
| GET | /{templateId}/creation-draft | Prepare form's `request` JSON; resolves active point type IDs **only within this family** |
| POST | /{templateId}/activate-task | Create task through existing controller; requires CREATE_TASKS, matching TASK kind, and `Idempotency-Key: <UUID>` |
| POST | /{templateId}/activate-reward | Create reward through existing controller; requires MANAGE_REWARDS, matching REWARD kind, and `Idempotency-Key: <UUID>` |

## Critical safety and UX requirements
1. **Import is not activation.** Onboarding shows checkboxes for individual catalog codes; zero selection valid; selected templates are copied to the family's private library only.
2. **Draft is not activation.** Present `creation-draft.request` as an editable form. Resolve any null point type/member/date before final submit. A draft alone changes no database records.
3. **Activation endpoints currently accept a complete creation request supplied by the client.** Backend checks template scope and kind but **does not yet enforce that submitted attributes match the selected template**. Do not expose as a one-click trusted import operation. Existing Task/Reward validators still apply.
4. **Idempotency-Key:** generate a fresh UUID per intended activation and reuse for a transport retry, not for separate intended creations. Current repeated request returns HTTP 409, not the prior response. Client should refresh current definitions rather than blindly creating with a fresh key.
5. **Currency:** portable template codes (`currencyCode`, `rewardCurrencyCode`, `penaltyCurrencyCode`) resolve to active point types scoped by `family_id`. Unknown or inactive codes leave IDs null. Do not accept another family's IDs.
6. **Events:** EVENT may exist in the private template library, but `creation-draft` does not support EVENT activation yet. Do not present it as an executable TASK.
7. **Localization/content:** catalog-v1.json currently contains **142 localized entries (110 TASK, 32 REWARD) and 6 starter packs**. Designer economics (recommended rewards/prices/currency codes) remains unset; do not invent values. 65 entries are not included in starter packs but remain individually available.

## QA checklist (DEV)
- Cross-workspace list/read/update/delete/activation should not reveal or mutate another family's template.
- Import preview selected subset and empty list; repeat import and verify no duplicates/active tasks/ledger activity.
- Build task/reward draft from saved source, verify only local active currencies are mapped, no member IDs or historical data reused.
- Activate with missing template, mismatched kind, missing permissions, malformed UUID, repeated operation UUID; confirm correct error statuses.
- Activate with invalid task date/assignment/reward currency; ensure existing validators reject and idempotency claim rolls back.
- Verify rollback and duplicate prevention on PostgreSQL Testcontainers; DEV manual checks only after CI green.

## Not done (do not mark complete)
- Enforcing template-to-activation payload consistency and comprehensive activation validation.
- Full retry-safe replay of original response (today 409).
- Full weekly availability windows; EVENT model/recurrence and designer economy recommendations.
- End-to-end Web/Android/QA acceptance and release authorization.

## Verified baseline (2026-10-26)
- CI success: https://github.com/o-kobylianskyi/family-points/actions/runs/40390915206 at `e05677c5df06e3ed07e0e31e6042b2017b832f5b` (Java/PostgreSQL, Web, Android debug, Docker Compose).
- Imported designer `definition.durationMinutes` maps into `creation-draft.request.defaultDurationMinutes`; regression test covers 30 minutes and precedence of an explicitly present `defaultDurationMinutes`.
- QA confirmed DEV Flyway V39/V40 tables exist but `fp_dev_mcp` does not have direct SELECT on those private tables. Perform privacy scenarios through authenticated DEV API; do **not** grant unrestricted cross-family DB read access as a shortcut.
- Read-only source/CI checks do not establish DEV runtime smoke, family isolation at HTTP level, or BETA release. See `TEMPLATES_BACKEND_QA_HANDOFF_20261026.md` for TPL-01..TPL-15.
