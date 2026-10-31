# Designer → Architecture / Backend / Web / Android — Consumer contract decision gate (2026-10-31)

Status: **BLOCKED ON CONTRACT**, not implemented, not DEV verified. Inspected main head `eb2fcb5ee666a015ed1a2c168861cda13d00a032`; Actions `41192626096` IN_PROGRESS at inspection.

## Established
- Catalog 142 (110 TASK, 32 REWARD), 6 packs; distinct coverage 99 in packs, 43 outside; exact matrix in `docs/design/economy/STARTER_PACK_COVERAGE_CURRENT_20261027.json`.
- Economy preset in `src/main/resources/templates/economy-suggestions-v1.json`: 11 optional TASK point recommendations, 12 optional REWARD prices; other 119 values null; `autoApply=false`, no workspace mutations; difficulty independent from duration.
- 18 approved category SVGs mapped by Backend Media Catalog; Web global TemplatesPage previews present; task bindings partial; Android Media API exists but screen consumption not proven.
- Web and Android accepted Designer audit `DESIGNER_INTEGRATION_SOURCE_AUDIT_20261031.md`; neither reports end-to-end economy consumption implemented.

## Required explicit Backend / Architecture decisions (P1 after reset safety P0)
1. Define versioned read-only suggestion DTO: code/kind/difficulty/estimatedMinutes/suggestedRewardPoints/suggestedPricePoints/pricingState/reviewRequired/flags; bind by immutable catalog code, not labels. Document where client fetches and how null vs absent vs explicit zero behave. Prevent a null REWARD price from becoming free, and specify validation on explicit activation.
2. Confirm scope of user editing/acceptance: parent explicitly confirms and adjusts numeric values on creation; existing private templates, definitions, ledgers and balances are not overwritten on catalog refresh or pack import.
3. Publish stable authorized Media target type/id/slot for active REWARD and TASK and precedence: explicit custom binding > category icon default > safe fallback; distinguish task/reward collection. Do not guess target enums or persist suggested icons without explicit action.
4. Give Web/Android test fixtures for one priced REWARD, one null-price REWARD, one TASK with suggested points, one null TASK, and one TIME 30m reward. Specify authorization/tenant and migration compatibility.
5. Separate source CI from exact deployed DEV SHA; QA should verify 18 media previews, all four locales, both themes, family A/B isolation, offline cache, explicit opt-in, and unchanged balance/ledger before activation.

## Owners
- Backend + Architecture: contract and authorization approval (not a request to prioritize ahead of BKL-002).
- Web: read-only previews and non-destructive editable prefill only after contract; reward media consumer.
- Android: same contract/renderer, protected per-workspace cache, nullable fields.
- QA: DEV acceptance with evidence and exact SHA.
- Designer: only editorial clarification and suggested values; no code deploy, SQL, reset or automatic prices.

BKL-002 safe transaction, replay fencing and specifically authorized disposable-family QA remain P0; reset /execute is not approved from this document.
