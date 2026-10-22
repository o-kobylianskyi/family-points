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

The original PNGs are **conversation attachments**, not yet image binaries stored in this Git repository. Hashes uniquely identify those files. For pixel-accurate acceptance obtain those originals, not web image-search replacements. The existing shared design brief is [FAMILYPOINTS_SOFT_FAMILY_DESIGN.md](FAMILYPOINTS_SOFT_FAMILY_DESIGN.md).

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
4. Compare real-device screenshots to the originals; record screenshots, exact git SHA and test evidence. Independently resolve the Android release crash / verified APK gate IMP-0003.

**Cross-track:** Design is tracked under IMP-0001 (Web/Android/Architecture). No Backend, Flyway, VPS, auth or release change is authorized by this document. Media work requires a new Architecture/Backend-reviewed contract. Log actual work in docs/coordination/logs/android.md.
