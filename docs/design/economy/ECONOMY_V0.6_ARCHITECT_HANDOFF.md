# FamilyPoints — Economy v0.6: Architecture approval handoff

**Status: SUBMITTED FOR REVIEW / NO ARCHITECT APPROVAL RECORDED.** 2026-10-26. Owner: Designer. Scope: DEV planning only; never deploy into BETA by this document.

## Content readiness
- 110 TASK code-level rows based on v0.4: **11** with provisional optional-point amount, **99** with `null` (not zero or prohibition).
- 32 REWARD rows based on v0.5: **12** with provisional optional-point price, **20** with `null` (not free or gated).
- Localized task/reward names remain maintained in global `catalog-v1.json`; do not copy stable tenant-specific IDs or ledger entries into Designer artifacts.
- **NOT APPROVED:** Difficulty assignments, coefficients, prices, category exceptions and whether family activities belong in purchasable rewards require Architecture/Product decisions.

## Required decisions
| ID | Decision | Proposed option | Owner | Priority |
|---|---|---|---|---|
| ECO-001 | Складність незалежно від часу | 4 рівні EASY/MODERATE/HARD/ADVANCED; час лише додатковий критерій | Architecture | P0 |
| ECO-002 | Рекомендація vs автоматична формула | За замовчуванням лише editable suggestion; зарахування за підтвердженим виконанням | Architecture / Product | P0 |
| ECO-003 | Безкоштовність базових потреб | NULL не означає 0, покупку чи заборону; базові потреби та сімейне спілкування не монетизувати | Architecture / UX | P0 |
| ECO-004 | Пенальті | 0 за замовчуванням; ручна явна дія дорослого; окрема репутація | Architecture | P0 |
| ECO-005 | Повторні нарахування | Idempotent occurrence+beneficiary+event; atomic ledger; offline replay | Backend | P0 |
| ECO-006 | Багаторівневі завдання | Не дублювати бонуси parent+children за замовчуванням | Backend / Product | P0 |
| ECO-007 | Валюти | POINTS як базова рекомендація, COPPER/SILVER/GOLD без несанкціонованих курсів | Architecture | P1 |
| ECO-008 | Часові нагороди | Покупка не запускає таймер, діють батьківські/OS ліміти | Backend / Android | P0 |
| ECO-009 | Предметні нагороди | Ціна тільки для окремої сім'ї й після підтвердження батьками | Product / Web | P1 |
| ECO-010 | Вікова справедливість | Вік — рекомендація, без гендерних та вікових автоматичних множників | Product / QA | P1 |
| ECO-011 | Ліміти повторних робіт | Окремий occurrence, cooldown і ліміти з можливістю налаштування | Backend | P1 |
| ECO-012 | Імпорт та версія | Глобальний каталог не мутує приватні шаблони, баланс і ledger; explicit preview+apply | Backend / Web | P0 |

## Acceptance matrix

| Track | Testable acceptance | Status |
|---|---|---|
| Designer | All 110 TASK and 32 REWARD stable codes accounted for and traceable to global catalog | Prepared |
| Architecture | ECO-001…ECO-012 formally resolved, including null semantics, fairness and recurrence | Open |
| Backend | DTO supports editable suggestions without implicit ledger writes | Open |
| Backend | Replayed and concurrent earning command yields one ledger mutation per allowed occurrence/beneficiary | Open |
| Backend | Purchases are atomic, idempotent, auditable and cannot spend same balance twice | Open |
| Backend | Family scoped currency IDs, no cross-tenant references | Open |
| Web | Optional selection → preview → edit → explicit activation; no zero-price by null | Open |
| Android | Offline replay safe; screen-time entitlement respects device and family rules | Open |
| QA | Repeat/rollback, double-tap, offline, timezone/recurrence, parent-child grouping regression suite | Open |
| Release | Migration/deployment signoff is separate from design signoff | Open |

## Known editorial risks
1. Bonuses on routine tasks may incentivize repetition or duplicate larger chore rewards.
2. Different physical/cognitive accessibility needs imply individualized suggested difficulty; labels aren't absolute.
3. 20 unpriced rewards include ordinary family connection; null must be a distinct proposal state, not an executable purchase price.
4. Three earnings scenarios used in v0.5 are illustrative only; production policy requires user testing and controls.
5. Complexity must not depend on minutes alone; responsibility, safety, self-directedness and effort need explicit review.

## Deliverables & next action
- See `economy_v0.6_handoff.json` for decision tracker and per-code traceability.
- Architecture should resolve the 12 decisions **before** assigning schema/DTO changes.
- On approval, Designer may publish a revised approved preset into a **new versioned resource**; do not edit live `catalog-v1.json` until Backend contract is approved and tested.
- No workspaces, balances, transactions, existing task definitions, rewards, or releases have been touched.
