# FamilyPoints — Calendar UI visual reference (daily agenda)

**Date:** 2026-10-22  
**Status:** AGREED product/UI baseline (user explicitly approved recommendations 2026-10-22); implementation and visual/device acceptance PENDING. Design document only — not implemented.  
**Tracks:** Architecture P (visual direction), Android A (native Compose), Web W (responsive UI). Backend B remains the authoritative source for schedule semantics.  
**Design contracts:** `docs/architecture/ADR-CAL-001.md` (calendar/domain), `docs/ANDROID_SOFT_FAMILY_UI_SPEC.md` (existing light/dark Soft Family reference).  
**Requests:** XTR-20261022-P-02 (Web), XTR-20261022-P-03 (Android); cross-track IMP-0006.

## Original user-provided reference

A single 1200×1200 JPEG showing an iPhone mockup, with greeting “Good evening, Sarah”, pastel Events/Reminders/Inbox statistic tiles, Today/Tomorrow switch, event timeline, separate Chores list and floating five-item bottom navigation; uploaded by the user in the Architecture / Planning chat on 2026-10-22.

**SHA-256 of provided JPEG:** `2bfb18d08d237c48c88568414d48893bb5a5e1b74c07220a1ed73f33edf8dfe7`. The original image is **a conversation attachment and has NOT been committed to the Git repository**. A later image file should use the actual attachment with attribution/licensing verification, rather than a newly guessed replacement.

### What is selected as baseline

**Layout / information architecture / interaction tone**, not literal branding or feature data. The prior user-approved Soft Family light/dark palettes and illustration/component vocabulary remain authoritative; do NOT replace either with the exact purple/yellow/turquoise color scheme, an iOS status bar, fictitious example people, locations, or Inbox functionality.

- Compact contextual greeting, clear identity + current family/member.
- Three soft rounded summary tiles, with role-appropriate current counts and contextual navigation.
- Small “Сьогодні / Завтра” segmented tabs and visible path to a complete day/week/month/range calendar.
- Comfortable white/soft-surface rounded agenda card; narrow time column, thin timeline spine, colored event dot, participant avatar stack, concise title/secondary explanation.
- Separate lower “Завдання без часу” section for undated-in-day tasks; tidy list rows, status/actions, no huge cards for each item.
- Fixed rounded bottom navigation respecting safe-area inset and scroll padding; accessible focus and screen reader labels.

## Explicit user approval — 2026-10-22

The user explicitly agreed with the Architecture recommendations for adapting this calendar reference to FamilyPoints. This confirms the **functional composition and information architecture** below as the product target, not acceptance of any implemented screen.

**Approved decisions:**
1. Keep a separate, agenda-centered **Today/Home** experience and a **Full Calendar** with day/week/month/date-range views.
2. Today uses three permission-aware compact summary cards, quick Today/Tomorrow switching, a chronological vertical time rail with colored semantic events and authorized participant avatars, plus a separate section for tasks without exact time.
3. A single agenda contains task deadlines/occurrences and reward availability windows, with distinct icons, statuses and explanatory text. Reminder is separate from deadline; add it only when supported by actual product logic.
4. Tasks and rewards are sorted chronologically with past items clearly distinguished; no overlaid/obscured final rows, responsive touch controls, accessible typography/contrast.
5. Preserve existing agreed **Soft Family** light/dark themes and native Compose implementation for Android. Borrow screenshot composition, not iOS-only controls, fake sample content, purple/yellow palette or unrelated Inbox/microphone functions.
6. Role-driven UX: child sees own authorized tasks, balances and possible rewards; authorized parent/admin can switch family/member views and see approval requests.
7. Preferred bottom tabs: **Home · Calendar · Tasks · Rewards · Family**; Settings lives under profile. Respect existing features/permissions and keep alternative entry points when reorganizing navigation.
8. Web and Android receive the same semantic calendar and permission rules from Backend but can use platform-appropriate responsive/native implementation. Neither track must interrupt its current styling milestone.
9. Offline Android view remains read-only and visibly stale until synced; reward purchases and permission enforcement stay server-authoritative.

**Implementation-only decisions still open:** precise API shape, event count/performance limits, workspace timezone migration, Android route refactor details, and reward utilization timing. These are not implicitly approved just by accepting the visual proposal.

## Proposed FamilyPoints mobile information architecture

1. **Top header:** greeting (“Вітаємо”/time-aware optional), family/workspace and selected member; profile/notifications or search if useful, never fake unread counts.
2. **Three role-sensitive metric tiles:** child: `Завдання сьогодні`, `Винагороди доступні`, `Мої бали`; parent/admin: `Завдання сьогодні`, `Запити на погодження`, `Винагороди/події` (configurable based on real API). Counts must have clear definitions and open the relevant destination. Hide tiles lacking permission rather than leaking other-member data.
3. **Agenda card:** Today/Tomorrow quick tabs; date header/picker opens full calendar. Timeline sorted chronologically with past items optionally dimmed and current/next item emphasized. When showing next item first, past items are explicitly moved to “Минулі”, not intermixed out of order.
4. **Event types:** `TASK` (due/deadline at displayed time, not asserted start), `REWARD_WINDOW` (from–to interval when acquisition is allowed, conditionally), and optional `REMINDER` (distinct from deadlines, only after feature exists). Use type icons and localized text, not color-only cues.
5. **No-time tasks:** separate concise section `Без визначеного часу`, retaining start/complete and permissions. Actual date still matters.
6. **Bottom navigation candidate** (requires consumer review against existing app navigation): `Головна`, `Календар`, `Завдання`, `Винагороди`, `Сім'я`; `Налаштування` available from profile/menu, with Points from Home and member account. Prefer <=5 visible tabs; do not silently remove existing routes or functions.

## Event anatomy

- **Time left:** `До 17:00` for task due time; `16:00–18:00` for reward acquisition window; `Упродовж дня` for all-day events. In month/week/day, display the correct workspace date and IANA timezone; do not use the device clock as purchase authorization.
- **Spine + dot:** visual chronology and semantic task/reward/reminder type; completed/missed/blocked states conveyed with text/icons and adequate contrast in BOTH light and dark.
- **Avatars:** authorized participants only; if none or no image, show compact initials/illustrations. Do not leak children/other family members to unauthorized viewers.
- **Primary:** task/reward title; **secondary:** member(s), points or price, remaining requirements, location only if a real field exists; date/time status. Avoid overlong ellipses without a details opening.
- **Tap:** navigates to details with `definitionId/date/slotKey` for tasks or `rewardId/windowId` for reward windows; visible available action only if allowed by server flags.
- **Reward variants:** UPCOMING, OPEN + ELIGIBLE, OPEN + REQUIREMENTS_UNMET, CLOSED/EXPIRED; show `Наступне вікно` when known, never imply future eligibility is guaranteed before task/points approval checks.
- **Existing virtual task semantics:** `scheduledTime` is currently a due/deadline; do not display it as a task start. Instances appear only after action/deadline per ADR-CAL-001.

## Usability improvements over provided reference

1. **Chronology:** source screenshot visually places a 19:30 event ahead of 10:30 and 16:00; sort by local chronological time or explicitly separate future/upcoming from past.
2. **Relevance:** replace Events/Reminders/Inbox sample tiles with real task/reward/points/approval indicators. Inbox should not be implemented solely because it appears in screenshot.
3. **Full calendar:** the reference depicts a TODAY DASHBOARD; offer separate full calendar month/week/day/range with filter by task vs reward, member, group if authorized. Month displays category dots and daily list drill-down; no attempt to squeeze detailed agenda onto month cells.
4. **Action clarity:** differentiate “scheduled due” tasks from opening time windows of rewards, and availability from requirements and purchase success. Time-based reward acquisition != actual device-use entitlement.
5. **Mobile ergonomics:** bottom navigation never overlays last Chores row, has safe-area padding, normal finger-size targets (~44–48 dp), legible minimum text size/contrast, and not too many interactive top cards.
6. **Offline:** Android cached calendar is read-only, marked offline/out-of-date; no offline reward purchase or locally inferred server permission.
7. **Dark mode:** preserve same composition/tabs/meaning in dark scheme, substitute semantic surface and text tokens, do not merely invert JPEG colors.
8. **Localizations:** UK, DE, EN, RU; dates/weekdays/time and long German labels handled through responsive/wrapping text.
9. **No floating microphone overlay:** reference voice icon is not a product requirement; avoid unrelated actions until a concrete feature exists.
10. **Role-aware privacy:** parents may switch member view only with permission; children see only their authorized own/group schedule; do not show unauthorized reward/task details.

## Proposed build sequence
- First: Backend calendar and availability DTO contract (XTR-P-01, IMP-0006).
- Web/Android complete currently approved Soft Family style implementation independently.
- Second: both clients implement TODAY agenda composition adapted from this reference, then full day/week/month views and reward schedule editor.
- Verify native Compose vs responsive Web parity in **structure**, not exact rendering, with screenshots light/dark; inspect 360–430 dp wide Android devices and responsive desktop widths.
- No app/code changes, builds, tests or live rollout are implied by this visual specification.

## Visual acceptance checklist
- [ ] User-approved mobile composition is recognizable: three compact tiles, Today/Tomorrow, time-line, no-time chores, fixed bottom navigation.
- [ ] Tasks/rewards visually distinct, semantic statuses and permission-driven actions correct.
- [ ] Chronological order and due vs start vs reward acquisition interval unambiguous.
- [ ] No source-image fictional people/events or fabricated balances stored.
- [ ] Full calendar and current day dashboard both navigable; screens do not overflow/overlap on phones.
- [ ] Existing light/dark Soft Family design, accessibility and four locales respected.
- [ ] Calendar virtual occurrences and backend purchase guard correctly integrated and independently tested.
