# FamilyPoints — Designer consumer integration source audit

**Date:** 2026-10-31. **Baseline:** main `1e3e91b0f08bf1883cd94a83d0166a1da33b89ab`. **Status:** SOURCE AUDIT, not DEV/E2E validation.

## Confirmed source evidence
- Backend bundled 18 approved category SVG assets and registered their MediaCatalog keys in MediaService (earlier reviewed).
- Web `frontend/src/pages/TemplatesPage.jsx` contains a category→`category-*` display map and authorized media-preview loading, supporting visual thumbnails in global suggestions.
- Web `EntityMedia.jsx` implements authorized private binding rendering; previous Web journal reports partial integration on managed TASK surfaces, not complete REWARD support.
- Android `MediaApi.kt` defines catalog and authorized preview endpoints, but inspected `TemplateLibrarySection.kt` and `RewardsScreen.kt` contain no `category-*` mapping, media catalog preview references, or demonstrated category-specific icon rendering.
- Backend runtime `src/main/resources/templates/economy-suggestions-v1.json` contains 110 TASK and 32 REWARD records, 11 optional task rewards and 12 optional reward prices; no auto-apply, no workspace mutation.
- `frontend/src/pages/TemplatesPage.jsx`, `frontend/src/pages/RewardsPage.jsx`, and Android `TemplateLibrarySection.kt`/`RewardsScreen.kt` did not evidence `suggestedPricePoints`, `suggestedRewardPoints`, `difficultySuggestion` or `pricingState` mapping. This is not proof the whole app lacks usage; consumers must identify any other integration surfaces and provide evidence.

## Requested closure
1. **Web P1:** consume read-only template economy suggestions for task/reward preview and creation drafts with explicit parent acceptance; preserve null as unset, never free. Existing user's private overrides win. Ensure category icons on private templates, live task/reward surfaces with explicit binding precedence.
2. **Android P1:** consume economy suggestions with same nullable semantics and parent confirmation. Provide verified category-icon renderer for the 18 media keys in relevant Task/Reward interfaces; existing MediaApi endpoint alone is insufficient.
3. **Backend/Architecture:** certify DTO schema, media target types, and suggestion consumption contract before consumer side-effects; no silent retroactive amounts and no reset of real family data.
4. **QA:** prove 142/6 counts; 99 in packs and 43 outside (supersedes stale 65); one/new/mixed import dedupe; nullable reward prices and TIME 30m; 18 previews; cross-family media isolation; 4 languages and both themes. Pin actual DEV SHA.
5. **Release:** separate all-green current HEAD CI from live DEV/BETA acceptance; BKL-002 scoped reset remains release-critical.

## Evidence limits
No Web browser, Android emulator, authenticated DEV HTTP, SQL mutation, family reset, or actual deployment was performed in this Designer audit. All required E2E cases remain PENDING until evidence.
