# FamilyPoints — Designer v0.7: architecture decision gate and starter-pack coverage

**Date:** 2026-10-26. **Status:** REVIEW REQUEST; **NOT APPROVED**. Designer/Architecture coordination. No runtime/backend/schema changes.

## Verified catalog coverage
Current `src/main/resources/templates/catalog-v1.json` has **142 entries**, **6 starter packs**.
- Unique template codes referenced by packs: **77**.
- Templates not referenced by any starter pack: **65** (**43 TASK / 22 REWARD**).
- These entries remain available in global catalog browsing, but they will not appear under a pack until selection logic changes.
- **Do not silently insert them into packs.** The unlisted subset includes private/age-sensitive, optional material gifts, family activities and other cases needing UX review.

## Items outside starter packs (needs Product/Designer review)
### TASK (43)
- `fp.task.hygiene.004` — Прийняти душ
- `fp.task.routine.008` — Вчасно встати
- `fp.task.routine.012` — Виконати ранковий чекліст
- `fp.task.chores.016` — Витерти пил
- `fp.task.chores.020` — Допомогти накрити стіл
- `fp.task.chores.024` — Допомогти скласти покупки
- `fp.task.school.026` — Підготуватися до контрольної
- `fp.task.school.028` — Повторити нові слова
- `fp.task.school.032` — Розв'язати логічні задачі
- `fp.task.reading.034` — Переказати прочитане
- `fp.task.reading.036` — Прочитати розділ книжки
- `fp.task.activity.040` — Зробити вправи на розтяжку
- `fp.task.activity.042` — Потренувати спортивну навичку
- `fp.task.creativity.044` — Зробити поробку
- `fp.task.creativity.045` — Пограти на музичному інструменті
- `fp.task.social.047` — Допомогти молодшому члену сім'ї
- `fp.task.social.048` — Зробити добру справу
- `fp.task.social.049` — Пограти разом без сварок
- `fp.task.social.050` — Попросити вибачення після конфлікту
- `fp.task.social.051` — Підтримати сімейну розмову
- `fp.task.habits.052` — Провести день без зайвого екрана
- `fp.task.habits.053` — Завершити завдання без нагадувань
- `fp.task.habits.054` — Підготувати план на завтра
- `fp.task.habits.055` — Записати три хороші події дня
- `fp.task.family.056` — Допомогти приготувати вечерю
- `fp.task.family.057` — Підготувати сімейну гру
- `fp.task.family.058` — Вигуляти собаку з дорослим
- `fp.task.family.059` — Допомогти доглядати домашню тварину
- `fp.task.family.060` — Разом скласти меню на вихідні
- `fp.task.adult.017` — Розкласти чисту білизну
- `fp.task.adult.018` — Замінити постільну білизну
- `fp.task.adult.019` — Винести сміття
- `fp.task.adult.020` — Перевірити запаси вдома
- `fp.task.adult.021` — Полагодити дрібну несправність
- `fp.task.adult.022` — Замовити необхідні товари
- `fp.task.adult.023` — Підготувати обід
- `fp.task.adult.024` — Підготувати вечерю
- `fp.task.adult.025` — Перевірити особистий календар
- `fp.task.adult.026` — Спланувати робочий тиждень
- `fp.task.adult.027` — Завершити важливе робоче завдання
- `fp.task.adult.028` — Відповісти на важливі повідомлення
- `fp.task.adult.029` — Розібрати робочий стіл
- `fp.task.adult.030` — Зробити резервну копію важливих файлів

### REWARD (22)
- `fp.reward.time.004` — 30 хвилин відеогри
- `fp.reward.time.008` — 30 хвилин творчості з дорослим
- `fp.reward.privilege.012` — Обрати музику в авто
- `fp.reward.privilege.014` — Обрати книжку для читання
- `fp.reward.activity.015` — Сімейний похід у парк
- `fp.reward.activity.016` — Спільна настільна гра
- `fp.reward.activity.017` — Пікнік із сім'єю
- `fp.reward.activity.018` — Відвідати дитячий майданчик
- `fp.reward.activity.019` — Спільний велосипедний виїзд
- `fp.reward.activity.020` — Кулінарний вечір разом
- `fp.reward.items.021` — Невеликий набір наліпок
- `fp.reward.items.022` — Новий олівець або ручка
- `fp.reward.items.023` — Маленька іграшка
- `fp.reward.items.024` — Нова книжка
- `fp.reward.items.025` — Набір для творчості
- `fp.reward.items.026` — Колекційна фігурка
- `fp.reward.family.027` — Додаткова казка перед сном
- `fp.reward.family.028` — Похід у бібліотеку
- `fp.reward.family.029` — Вечір конструктора з дорослим
- `fp.reward.family.030` — Особливий сімейний сніданок
- `fp.reward.family.031` — Обрати сімейну активність у вихідні
- `fp.reward.family.032` — Сімейний творчий проєкт

## Economic status: no approval recorded
The economy v0.6 handoff has 12 unresolved decisions ECO-001...ECO-012; this document requests Architecture review, it is **not** an Architecture decision.
- All global TASK rewardAmount and REWARD priceAmount remain null in runtime catalog; no auto-default values approved.
- Designer review v0.4/v0.5 provides optional suggestions only. Null means **no suggested/default value**, not “free”, “zero reward guaranteed” or “purchase allowed”.
- Approval must specify which values, if any, may become editable prefill, not actual earnings or purchases.
- Complexity must be independent of time; responsibility, autonomy and accessibility deserve review.
- Basic care, school, emotional bonds and ordinary family interaction are not paywalled.

## Decisions requested before v0.8
1. **ECO-001–012:** approve/reject/edit each item in `economy_v0.6_handoff.json`. State reviewer, date and approved semantics.
2. **PACK-001:** should 65 unlisted codes remain browse-only, move to individual packs or be offered in a separate “More suggestions” step? Recommend browse-only unless user selects them.
3. **PACK-002:** adult personal pack currently includes only a subset of adult TASK suggestions; confirm intended minimal onboarding instead of implicit omission.
4. **PACK-003:** rewards covering shared family time and basic relationship activities should not have a paywalled default.
5. **QA:** user can find unlisted items in catalog; skip and empty selection work; no auto purchase and no ledger effects on import.

## Handoff to implementation
Architecture: record decision statuses and approve schema/UX rules. Backend: keep current null monetary fields until approval. Web/Android: show missing monetary suggestions as unset, with user edit and explicit confirm. QA: test zero/empty/null and cross-family isolation. Release: no deploy inferred.

**No changes to catalog-v1.json, family data, economy settings or ledger.**
