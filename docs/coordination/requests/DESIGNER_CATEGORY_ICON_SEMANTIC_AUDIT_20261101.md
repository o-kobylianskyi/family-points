# Designer category SVG semantic audit — 2026-11-01

Inspected all 18 real SVG files in `src/main/resources/media/catalog/categories` and existing Web/Android category-to-Media mapping, against template meanings. This is **source review**, not visual DEV/device acceptance.

## Key conclusions
- **Corrected**: `hygiene -> category-self-care` uses toothbrush+water drop at source commit `20e41f8`; no longer heart. Web and Android map hygiene to this same key. The live Backend bundles SVG assets, so **GitHub change alone does not update deployed DEV**. Compare running Backend SHA and authenticated `/workspaces/{id}/media/catalog/category-self-care/preview` body before claiming fixed. No unapproved live deploy done.
- **Semantically appropriate**: activity=athletic shoe, chores=cleaning container, creativity=palette, family communication=people, family events=calendar, help others=people together, home=house, initiative=lightbulb, learning=book, material reward=gift, nature=seedling, practical skills=tools, self-care=toothbrush/water, achievements=target, privilege=star.
- **Acceptable but review for nuanced categories**: `planning` is a clock and Web/Android map `routine` to it (a schedule is appropriate, but a visual checklist may better express a daily routine); `screen-time` shows a game controller (suits games, not all screen time); `adult-tasks` depicts a person with briefcase (suits work, not every adult family chore).
- **Potential semantic mismatch**: `habits -> category-initiative` is a lightbulb; habitual self-discipline/routine differs from creativity/initiative. A dedicated habits/checklist icon is preferable in a future asset pass; do not silently remap to another existing icon without catalog-consumer review. `privilege` uses star/check; acceptable but differs from originally specified ticket-with-star.
- Existing global catalogue and private library icons use mapped display-only classification; do not infer icons from localized titles. Writable REWARD Media target is NOT supported; do not make new REWARD bindings to solve presentation problems.

## Actionable cross-team QA
1. Release/Backend: deploy matching image deliberately to authorized DEV only, verify running SHA and authenticated preview bytes are the toothbrush SVG. Check media response MIME SVG and cache, then Web reload. No database migration required merely for bundled SVG replacement.
2. Web/Android: inspect UX by category rather than treating all 18 SVG as interchangeable. Record 4 locales x 2 themes, global and private templates, fallback on bad media response, A/B workspace isolation.
3. Designer next backlog: decide dedicated `habits` and optional `screen time vs gaming` illustration, update both consumers and icon registry only with versioned compatibility. Retain existing 18 keys until coordinated update.
4. Issue18 setup: do not confuse `MEMBER_GROUP`, `WORK_NODE GROUP` and catalog category; import-only vs activation must be separate explicit parent actions. Read-only review first, no unapproved reset/deploy.

CI status at initial check: `41266601264` in progress for `2b53caf5`; no exact-HEAD PASS assertion. No emulator or DEV verification performed by Designer.
