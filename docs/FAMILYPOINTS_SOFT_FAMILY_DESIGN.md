# FamilyPoints visual direction — Soft Family (2026-10-22)

Status: **approved visual reference, implementation in progress, NOT released**.

## Selected references

- **Light:** airy off-white surfaces, calm sage and blue-green accents, large but space-efficient rounded cards, illustrated family header.
- **Dark:** dark navy/charcoal surfaces and muted cyan/teal highlights, readable light text, compact family dashboard and member cards.
- These are the two **user-selected mockups** from the conversation, not the later all-green alternatives. Do not silently replace with purple palette or bright green accents.
- Match **page structure**, not just colors: home greeting + family hero, compact balance/reputation/tasks, member cards, activities; members tabbed people/groups; points per-member selector + balance/actions/history; bottom navigation on Android/mobile.
- Illustrations are placeholders, NOT actual profile photos.

## Android-specific approved reference and acceptance (2026-10-22)

- The user explicitly reconfirmed BOTH supplied image mockups (Light and Dark) as the actual FamilyPoints Android target, not merely inspiration.
- Native Android component anatomy, accepted color direction, Light/Dark/System setting, reference image checksums, and open acceptance matrix: [ANDROID_SOFT_FAMILY_UI_SPEC.md](ANDROID_SOFT_FAMILY_UI_SPEC.md).
- Preserve common information architecture and actions across themes. Do not pretend the mockup images are already source-controlled or the application already matches them.

## Reference images are NOT production UI assets (user clarification 2026-10-22)

- Both approved Soft Family mockups and their cropped sub-images are **visual references only**. Android and Web must construct live, responsive screens using native UI/components; never embed a screenshot/cropped card/button/text panel as the functional interface.
- Extract the graphic language (shape, illustrations, colors, effects, composition); **adapt layouts**, support permissions, accessibility, different screen sizes, system behavior and real application states.
- **Remove any text baked into artwork** and render it separately as actual translated text. Names, roles, points, balances, history, tasks, labels and dates are runtime data / localized strings, not painted pixels.
- Only **text-free decorative illustrations** may become reusable image assets after clean isolation, adaptation and privacy/provenance checks. Controls, icons, chart/statistics and status are semantic components or vectors.
- Locales: Ukrainian, German, English, Russian; correctly handle dynamic strings, number/plural formatting, long translations, font scaling.
- Visual acceptance means similar identity and polish, **not pixel-for-pixel copying** of the iPhone mockup. Android-specific detailed rules: [ANDROID_SOFT_FAMILY_UI_SPEC.md](ANDROID_SOFT_FAMILY_UI_SPEC.md).

## Planned media functionality

- Per-account/profile avatar: built-in illustrated character, uploaded image with crop, or initials fallback. Photos stored/access-controlled by account/member identity.
- Per-workspace family header image (cover): editable by authorized workspace administrators, crop and position, defaults to illustration, scoped to the active family.
- No uploads to public routes without authorization; enforce file type, size, privacy and safe image processing.

## Themes

- Light / Dark / Follow device, preference persisted per user/device; align appearance across Android and browser.
- Web CSS semantic groundwork in `frontend/src/App.css` as `--fp-*` tokens. Dark tokens are opt-in via `data-theme=dark` but **no browser setting is wired yet**.
- Android native theme currently has temporary styling from preceding work; reconcile with approved light and dark references.
- Do not mix a new layout with unfinished legacy styling; migrate dashboard, members, points, login and settings coherently.

## Acceptance checklist

- [ ] Review design tokens/screens against the two selected mockups, including small-phone and desktop responsive behavior.
- [ ] Implement user-facing theme switch and persist across sessions.
- [ ] Implement profile avatar controls and authorization/validation.
- [ ] Implement workspace cover art configuration.
- [ ] Verify web and Android visual/functional parity with screenshots.
- [ ] Run frontend lint/build and Android build on developer machine.
- [ ] Publish only after explicit release gate and device/browser acceptance.
