# FamilyPoints — approved category icons: GitHub asset handoff

**User approval:** v3 vector style, 18 icons. **Asset source:** `src/main/resources/media/catalog/categories/*.svg`. **18 SVG confirmed present in main**. Existing task/reward definitions and media bindings remain unchanged.

## Proposed category-to-asset mappings (requires Backend/Web/Android registration)
| Template category | Approved asset |
|---|---|
| `hygiene` | `categories/self_care.svg` |
| `routine` | `categories/planning.svg` |
| `chores` | `categories/chores.svg` |
| `school` | `categories/learning.svg` |
| `reading` | `categories/learning.svg` |
| `activity` | `categories/activity.svg` |
| `creativity` | `categories/creativity.svg` |
| `social` | `categories/family_communication.svg` |
| `habits` | `categories/initiative.svg` |
| `family` | `categories/home.svg` |
| `adult` | `categories/adult_tasks.svg` |
| `time` | `categories/screen_time.svg` |
| `privilege` | `categories/privilege.svg` |
| `items` | `categories/material_reward.svg` |

Additional visual choices: `family_events.svg` (calendar), `nature.svg`, `help_others.svg`, `practical_skills.svg`, `achievements.svg`; these are available for more specific task mappings when a category registry supports them. This table is a **proposal**, not an enabled registry.

## Integration acceptance
- Resolve media catalog key and permitted static asset delivery path per Backend contract (don't assume arbitrary SVG path can be loaded by current media APIs).
- Validate sanitized static SVG delivery, CSP/content type/cache and Android SVG ingestion or PNG fallback, accessible labels and 24px legibility.
- Desktop/mobile light/dark UI, fallback for unknown category and old workspace templates.
- No automatic modifications to private family icons/avatars; keep avatar work paused.
- Keep separate graphic assets from economy and from personal user-uploaded media.

**Uploaded:** 18 native SVG files. **Not uploaded:** 54 PNG exports. PNG package remains separately available; add binaries only if required by Android/media pipeline.
