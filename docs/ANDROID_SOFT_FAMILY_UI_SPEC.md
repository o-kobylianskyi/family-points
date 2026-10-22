# FamilyPoints Android — Soft Family light/dark UI target

**Status:** User-approved visual target, 2026-10-22. NOT YET IMPLEMENTED/DEVICE-VERIFIED.
**Owner:** Android track; cross-track visual alignment under IMP-0001.
**Platform:** Native Kotlin + Jetpack Compose. No WebView of the React frontend.

## Original user reference images

The user attached **two canonical, equally authoritative reference mockups** in the 2026-10-22 Android conversation:

| Reference | Image dimensions | SHA-256 of original user attachment |
| --- | --- | --- |
| **Dark — Style A / Soft Family**: three phones (Home, Members, Points), navy/cyan | 1122 x 1402 PNG | ffb1c9712e0f26df0ae3afe109aa90ce1c66956d2e27420139b23211ab048ed5 |
| **Light — Style A / Soft Family**: same three screens, white/sky-blue/sage | 1122 x 1402 PNG | 961d13f3e857276144b6604a00b89696834052f39525d54ea5a42630e2da3d5b |

The original PNGs are **conversation attachments**, not yet image binaries stored in this Git repository. Hashes uniquely identify those files. For visual acceptance use those originals, not web image-search replacements; references are not directly embeddable UI assets. The existing shared design brief is [FAMILYPOINTS_SOFT_FAMILY_DESIGN.md](FAMILYPOINTS_SOFT_FAMILY_DESIGN.md).

## Mandatory rule — adapt references, do not paste screenshots (user clarification 2026-10-22)

**The approved mockups and all crops are design references, NOT production-ready UI assets.** The goal is the visual *language* (theme, hierarchy, shape, typography, spacing, illustration feel), not literal pixel-for-pixel screenshot imitation. Rebuild layout and controls in native Compose with real app data and behavior.

| In the reference image/crop | Required production implementation |
| --- | --- |
| Card with label, numeric value, button, indicator or role/status text | Draw card and layout in Compose; all text must be editable/rendered at runtime, fetched from localized resources and state. Do **not** display flattened PNG with embedded text. |
| Sample username, role, balance, email, task, date/time | Replace with real account/workspace-specific authorized data; format numbers, points/pluralization, currencies, dates and times per active locale. Placeholder names/contact details never shipped as UI. |
| Text embedded in decorative artwork | Remove embedded writing when preparing usable illustration; alternatively recreate the art **without words** and overlay a separate localized Compose Text element if text is intentionally needed. Do not merely hide inaccessible raster text behind a same-color patch. |
| Avatar/photo/character or decorative family image | Rework into reusable **text-free**, safely cropped illustrations/drawables/avatars; don't use screenshots of entire member cards as an image. Use actual server-provided avatar only under approved media contracts. |
| Icons/navigation/segmented selectors/pills/action buttons | Render as native clickable, semantic accessible Compose components with vector icons and state (selected/disabled/loading/error), not image cutouts. |
| Gradients, glow, borders, shadow, rounded shapes | Recreate via reusable Compose modifiers/Brush, Shapes and theme tokens; adapt elevation and contrast in light/dark themes. |
| iPhone frame, Dynamic Island, 9:41 clock, marketing header/slogan/background | Do not ship as app chrome. Respect Android system UI, insets, gestures and current platform conventions. |

**Asset rule:** Rectangular crops exported from the source PNG can guide design and be used for side-by-side inspection only. No full-phone, screenshot, card, button, label, menu, tab, points panel or other mixed image-with-text should be placed directly in the Android UI, even if individually cropped. Isolate/crop **only text-free decorative art** for possible inclusion, ensuring legal/provenance, quality, retina/density, transparent edges and dark/light contrast. Where imagery needs transformation, redraw/recreate a clean art-only asset rather than freezing UI text into an image.

**Content/localization:** UI strings belong in Android localized resources or the established app i18n mechanism for **uk/de/en/ru**, never baked into graphics or hard-coded from mockups. Include right-length handling for German, plural rules for points/reputation/tasks, Unicode names, ellipsizing only when appropriate, TalkBack descriptions and locale-aware date/time and numeric formatting. Preserve actual family/member names exactly as supplied by authorized user data, not translations of proper names.

**Responsive/functional acceptance:** Fit narrow/wide devices and font scaling, scroll and allow multiline labels, live updates, permission-driven actions, real/offline/error/empty/loading states, and reachable tap targets. Adapt content density, do not stretch a reference bitmap. The light and dark appearances share components/behaviors while using theme-specific color/illustration treatment.

**Acceptance clarification:** Compare Android screenshots to the references for *design consistency*, **not strict pixel matching**. An adapted component that works better on real Android screens and localizes correctly takes precedence over exact crop geometry. This rule overrides any older "pixel-perfect" wording about transferring reference images, but not the choice of the original two target mockups.

## Approved visual direction

**Light theme**
- Cool off-white / icy-light-blue page background, white cards with restrained soft shadows.
- Powder-blue to medium sky-blue pill selectors and balance highlights; muted sage green positive/primary creation actions.
- Dark nearly-navy headline typography, grey-blue secondary text and subtle borders.
- Warm illustrated characters and family images on hero/empty states.

**Dark theme**
- Deep graphite/navy to near-black page background, layered charcoal-blue cards.
- Blue/ice-cyan and mint/aqua gradients for highlights/selected actions, subtle glow (not excessive neon).
- Clear off-white primary text, legible cool-gray secondary labels, understated blue borders.
- Same friendly illustration and card language, adapted to dark-surface contrast.

**Shared identity**
- Large but space-efficient rounded cards; pill buttons/segmented tabs, well-spaced content, approachable illustrated avatars, consistent icon strokes and typography hierarchy.
- Both themes **must have one component hierarchy and one functional feature set**. Theme switching must not remove/reorder features or break state.
- Existing temporary Compose Theme.kt uses a light-only purple (#7856D8) scheme; Color.kt includes default starter colors. These are not approved final design tokens.
- Do not treat sample names, email addresses, zero balances, sample avatars or an iPhone's 9:41 system chrome in the mockups as literal user data or Android UI requirements.

## Screen anatomy

### Home
Header: current family/workspace, greeting, optional notifications; prominent balance; compact reputation/active-task metrics; family/member and task preview areas. The dark sample foregrounds recent tasks while the light sample foregrounds family/active tasks: these are layout variations, not separate data models. Adapt responsive composition and contextual empty states.

### Members
Title and authorized add action; Members / Groups segmented tabs; illustrated round avatars, name, role, permitted status, compact reputation and point totals; cards with rounded shape and contextual actions. Contact details displayed only if authorized by real Backend permissions.

### Points
Member selection pills, selected member profile and prominent balance, Earn/Add, Spend, Penalty controls based on actual permission flags; history with filter and illustrated empty state. Offline pending operations must stay visually distinct from confirmed backend balances.

### Common shell
Android-native bottom navigation adapting the reference Home/Tasks/Members/Points pattern, while retaining access to Rewards, Settings and all actual features. Correct Android status/navigation bars, gesture insets, back handling, keyboard/IME, small-phone wrapping. Never imitate iOS Dynamic Island or copy device chrome from the mockup.

## Implementation contract

1. Theme setting **Light / Dark / Follow device**, persist across app restarts. Default Follow device until overridden.
2. Semantic tokens: page background, surface/elevated surface, primary, secondary, positive, warning/error, primary/secondary text, border, muted, gradient and shadow. Map them to Material 3 color schemes and Compose card/pill decorations; use reusable components rather than per-screen literals.
3. Exact palette hex codes must be visually checked against the *original user images* before being finalized. Colors in mockups are gradients and cannot be accurately specified by guessing single pixels.
4. Preserve localization (UK/DE/EN/RU), accessible touch targets (48dp preferred), font scaling, text contrast (body text target >=4.5:1), narrow-screen scrolling and readable status colors.
5. Family cover / member profile image upload requires separately approved Backend/Architecture tenant-scoped media API; built-in illustrations and safe fallbacks do not imply upload is implemented.
6. UI refactoring must preserve online/offline indicators, permission gates, multi-workspace identity, point/reward semantics and existing API behavior. No API contract changes implied by this design choice.

## Acceptance (all currently OPEN)

| Component / test | Light | Dark |
| --- | --- | --- |
| Home: normal and empty data on small phone | OPEN | OPEN |
| Members: groups, role badges, permission variants | OPEN | OPEN |
| Points: member choice, operations, history, pending offline | OPEN | OPEN |
| Tasks, Rewards, Login, Settings consistent theme | OPEN | OPEN |
| Theme persistence + Follow device + restart | OPEN | OPEN |
| Four locales, dynamic text sizing, contrast and system insets | OPEN | OPEN |
| Actual screenshots compared with **original attachments** | OPEN | OPEN |
| Gradle and real-device UI verification | OPEN | OPEN |

## Suggested implementation sequencing (not a release order)

1. Implement theme token layers, persisted setting and reusable navigation/card/pill components.
2. Adapt Home, Members, Points and their loading/empty/error/offline variants in both themes.
3. Migrate other screens for consistent design and screenshot parity.
4. Compare real-device screenshots to the originals for visual consistency, native behavior and localization (not exact screenshot duplication); record screenshots, exact git SHA and test evidence. Independently resolve the Android release crash / verified APK gate IMP-0003.

**Cross-track:** Design is tracked under IMP-0001 (Web/Android/Architecture). No Backend, Flyway, VPS, auth or release change is authorized by this document. Media work requires a new Architecture/Backend-reviewed contract. Log actual work in docs/coordination/logs/android.md.

## Calendar screen reference supplement (2026-10-22)
The user additionally chose a compact iPhone-style TODAY/AGENDA screenshot as the FamilyPoints **calendar composition** baseline. This does not supersede the already approved Android light/dark Soft Family themes or authorize literal iOS chrome. Android Calendar in Compose should reuse the day-agenda layout, three contextual tiles, Today/Tomorrow, narrow time rail + avatars, separate no-time tasks, and a safe-area bottom nav; then open the full week/month calendar. Full recommendations and original-image SHA-256: [Calendar UI visual reference](FAMILYPOINTS_CALENDAR_UI_REFERENCE.md). Backend reward availability and deadline semantics remain controlled by ADR-CAL-001/IMP-0006.

## Temporary bundled avatar/illustration strategy (user decision 2026-10-22)

Until Backend media storage/catalog/assignment endpoints are implemented, **ship original, locally bundled Soft Family-style avatar drawings and category/task icons** rather than initials-only placeholders. They must be image-only assets with no embedded names, labels, or balances. Use a small varied built-in collection for people, groups, family and tasks; select a stable default based on entity ID and visual context, not a random choice on every render. Avoid representing gender/age as an authoritative inferred attribute. For user records with no media reference, show the bundled default; a manual locally selected avatar may be supported later only if it cannot falsely imply server synchronization.

Introduce one reusable source-resolution layer/component: later replace local defaults with Backend `imageRef`/effective image results when the agreed media API is ready; retain bundled art as loading/error/offline fallback. Uploaded private media must respect workspace authorization and per-account caches. Never display personal photos or claim cross-device synchronization before Backend is implemented. Keep typography and actions as localized Compose components, not embedded bitmap UI. Backend and Architecture remain owners of IMP-0007 media contracts.
