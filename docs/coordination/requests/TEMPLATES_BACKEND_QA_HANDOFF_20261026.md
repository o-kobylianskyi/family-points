# FamilyPoints — Backend templates QA handoff (2026-10-26)

**Status:** READY FOR TARGETED DEV QA, not RELEASED.
**Baseline:** `e05677c5df06e3ed07e0e31e6042b2017b832f5b`.
**CI:** https://github.com/o-kobylianskyi/family-points/actions/runs/40390915206 — SUCCESS (Java/Testcontainers, Web/Vite, Android debug, Docker Compose).
**Release:** no VPS/BETA deployment proven by this handoff. Verify DEV's deployed/build SHA and Flyway V39/V40 separately before manual tests.

## P0 resolved
- Designer global TIME reward `definition.durationMinutes` now maps to `creation-draft.request.defaultDurationMinutes`.
- `designerTimeRewardDurationMapsToCreationDto` expects 30, plus precedence test when an explicit `defaultDurationMinutes` exists; the CI above passes.

## Test inventory
- Global catalog `GET /template-catalog`: version 1, 110 TASK + 32 REWARD, six packs, uk/de/en/ru titles and descriptions.
- Initial catalog deliberately has no assigned reward amounts, reward prices or currency codes (null). This is a designer economics follow-up, **not** an importer defect. Do not create arbitrary prices in QA.
- Private library `/workspaces/{workspaceId}/templates`: list, create, update, delete, `imports/preview`, `imports`, `from-tasks/{taskId}`, `from-rewards/{rewardId}`, `{templateId}/instantiate-preview`, `{templateId}/creation-draft`.
- Explicit activation `POST .../{templateId}/activate-task` / `activate-reward` requires full valid client create DTO and `Idempotency-Key` UUID. Existing Task/Reward services create actual definitions.
- Flyway `V39` private template library and `V40` activation claims.

## QA acceptance cases — run against matching DEV Backend SHA
| Case | Action | Expected |
|---|---|---|
| TPL-01 | GET global catalog | 142 distinct template codes and six starter packs, language texts present |
| TPL-02 | Preview subset of 2–3 catalog codes | Only selected codes, counts of toImport/alreadyImported |
| TPL-03 | Import subset once and repeat | Private definitions only, no duplicate imports, no new active tasks/rewards, purchases or ledger changes |
| TPL-04 | Import empty selection | No changes; valid empty result |
| TPL-05 | Private template CRUD in family A; access same ID through family B | Family B does not see/edit/delete/activate A's template |
| TPL-06 | Save existing task/reward as private template | Definition fields retained, no old member IDs or histories copied; weekly reward availability currently unsupported |
| TPL-07 | Imported TASK creation draft | Nesting `definition` read; recurrence and amount fields mapped; member/date require user review |
| TPL-08 | Imported TIME REWARD creation draft | `durationMinutes:30` becomes `defaultDurationMinutes:30`; price/currency remain unset when catalog null |
| TPL-09 | Currency code maps to point type | Only active currency in current family resolves; foreign/inactive code leaves null |
| TPL-10 | Activate TASK valid assignment and dates | One new definition, no pre-created purchase/ledger; validate all assignment policies |
| TPL-11 | Activate REWARD with valid price/currency | One reward; existing reward validator applies |
| TPL-12 | Repeat activation with same Idempotency-Key UUID | HTTP 409; no second active definition |
| TPL-13 | Invalid/incomplete activate request | 4xx; no activation claim committed, retriable after fixing DTO |
| TPL-14 | Cross-family, wrong-kind, or foreign currency | 4xx/403/404; no cross-family data leak |
| TPL-15 | Restart backend after import | No duplicate private templates, selected definitions preserved |

## Known limitations / don't overclaim
- Imported global template titles are presently saved to the private library in Ukrainian; end-user locale selection is a separate UI integration.
- `creation-draft` does not instantiate records. Actual activation requires a complete request and separate user confirmation.
- Current activation validation guards required fields and family currencies but **does not enforce all request fields against saved template defaults**; user edits are currently allowed.
- Idempotency uses conflict-on-repeat, not prior response replay.
- No server-driven EVENT creation, designer economy, full offline client acceptance or released BETA proof.
- Global catalog is a read-only suggestion list; import must not modify existing user tasks, rewards or ledgers.

## Hand-off
QA: execute TPL-01..15 on DEV, report case IDs with request/response and observed DB counts; avoid real-data mutations unless using a designated test family.
Web / Android: integrate `creation-draft` with editable forms and make UUID reuse explicit; don't promise one-click activation.
Release: only when QA finds no critical privacy/ledger defects, pin exact server/web/android SHA, validate V39/V40 migration backup/smoke, publish through release-owned workflow.
