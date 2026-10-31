# Designer — Архітекторський dispatch: audit v0.8

**Джерело:** `docs/coordination/DISPATCH_20261026_LATE.md`. **Статус: виконано як редакторський аудит; не погоджено як числова економіка.**

## Що перевірено
- 110 TASK / 32 REWARD з унікальними кодами, назвами й поясненням рекомендації на рівні кожного коду в супровідному JSON.
- 6 стартових наборів; 65 шаблонів не входять до жодного набору. Це **не** автоматична помилка: вони доступні для індивідуального вибору.
- Складність незалежна від часу; поле nullable для цін і балів не прирівнюється до 0.
- **Не робити** автоматичних виплат, списань, створення завдань чи зміни цін у чинному каталозі.

## Стартові набори
| Код | TASK | REWARD | Усього |
|---|---:|---:|---:|
| fp.pack.preschool | 22 | 10 | 32 |
| fp.pack.early_school | 22 | 10 | 32 |
| fp.pack.older_children | 22 | 10 | 32 |
| fp.pack.mixed_age | 22 | 10 | 32 |
| fp.pack.adult_personal | 16 | 0 | 16 |
| fp.pack.family_shared | 20 | 0 | 20 |

## Аналіз
Коди поза наборами: **65** (43 TASK, 22 REWARD). Не слід механічно включати всі 65 до onboarding, щоб не перетворити його на довгий список. Користувач повинен мати повний каталог і пошук, з можливістю пропустити будь-який шаблон.

Для TASK використано категорійні підстави та наявні ручні корекції v0.4; це не експертно доведені персональні рівні складності. Для REWARD використано попередні значення v0.5. **Числові значення ECO-001..ECO-012 Архітектором не затверджені**, як вказано в dispatch.

## Що потребує окремого погодження
1. Затвердити або замінити ставки складності, суми бонусів, ціни та поведінку null.
2. Погодити, чи різні види сімейної активності взагалі повинні бути REWARD з ціною.
3. Визначити правила включення позанабірних шаблонів в onboarding; рекомендований шлях — окремий каталог без автоматичного відбору.
4. Backend/QA — окрема перевірка імпорту, обмежень, idempotency, tenant isolation та відсутності зміни ledger.
5. Після рішення Архітектора — окремий затверджений версійний preset, а не редагування історичних приватних шаблонів.

**Реліз і DEV deployment цим аудитом не підтверджені.**

## 2026-10-26 — User preliminary approval of economy
**Decision by user:** economy proposal is **PRELIMINARILY APPROVED** as the working baseline for Architecture/Backend integration design. This supersedes the previous `review-only` gate for planning, **but is not a final numeric rate/price sign-off or authorization to mutate workspaces or balances**.
- Keep separate difficulty and duration; allow administrator editing of suggested amounts/prices.
- The v0.8 per-code mapping is the baseline for integration review, not active data migration.
- Architecture/Backend must validate DTO, null semantics, reward restrictions, exact-once ledger accrual and existing-family preservation before publishing a versioned preset.
- Do not automatically enable point rewards for basic needs, school, family contact, or adult schedules.
- Existing `catalog-v1.json`, private templates, wallets, ledger and BETA remain unchanged pending implementation and QA.
