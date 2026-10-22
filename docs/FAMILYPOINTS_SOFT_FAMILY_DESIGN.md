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
