# Designer → Backend/Web/Android: economy v1 and media contract

**Date:** 2026-10-31. **Backend source baseline:** `cda7e8a07a3d20ca6650bea1ed5df31330865973`.
**Status:** implemented source contract reviewed; numeric economy suggestions remain preliminary. Ready for consumer review, not authenticated DEV/browser/emulator/BETA acceptance. Tracks issue [#17](https://github.com/o-kobylianskyi/family-points/issues/17).

## 1. Implemented economy wire shape

`GET /template-catalog/economy-suggestions` returns serialized JSON with `Content-Type: application/json`. Authentication is required by SecurityConfig; this is global bundled content, not workspace data. `GET /template-catalog` returns the separate versioned catalog.

The full response contains **110 tasks and 32 rewards**. Eleven task entries and twelve reward entries have optional numeric suggestions. Other amounts remain JSON null. Below is a real-value EXTRACT of the implemented response, not a shortened response the endpoint actually returns:

```json
{
  "schema": "familypoints.economy.suggestions.v1",
  "version": 1,
  "sourceCatalogVersion": 1,
  "currencyCode": "POINTS",
  "autoApply": false,
  "workspaceMutation": false,
  "approvalRequiredBeforeActivation": true,
  "tasks": [
    {
      "code": "fp.task.hygiene.001",
      "difficultySuggestion": "EASY",
      "estimatedMinutes": 3,
      "suggestedRewardPoints": null,
      "pricingState": "UNSET",
      "reviewRequired": true,
      "flags": [
        "BASIC_RIGHTS_OR_WELLBEING"
      ]
    },
    {
      "code": "fp.task.routine.007",
      "difficultySuggestion": "EASY",
      "estimatedMinutes": 5,
      "suggestedRewardPoints": 5,
      "pricingState": "OPTIONAL_SUGGESTION",
      "reviewRequired": true,
      "flags": [
        "REPEATED_AWARD_LIMIT_REVIEW"
      ]
    }
  ],
  "rewards": [
    {
      "code": "fp.reward.time.001",
      "suggestedPricePoints": 25,
      "pricingState": "OPTIONAL_SUGGESTION",
      "kind": "TIME",
      "durationMinutes": 15,
      "reviewRequired": true,
      "flags": [
        "SCREEN_TIME_PARENTAL_CONTROLS_REVIEW"
      ]
    },
    {
      "code": "fp.reward.time.007",
      "suggestedPricePoints": null,
      "pricingState": "UNSET",
      "kind": "TIME",
      "durationMinutes": 30,
      "reviewRequired": true,
      "flags": [
        "NO_PRICE_DOES_NOT_MEAN_FREE_OR_PAYWALLED"
      ]
    }
  ]
}
```

- Envelope `schema=...v1`, `version=1`, `sourceCatalogVersion=1`, `currencyCode=POINTS`. Catalog codes identify content; the enclosing collection identifies TASK versus REWARD. Version belongs to the envelope, not an invented per-item version.
- A reward suggestion's `kind: TIME` is suggestion metadata, not the activation request enum. Use the server creation draft's `rewardKind` and `defaultDurationMinutes`; do not copy suggestion metadata blindly into a creation request.
- `null` means UNSET, never zero/free. Creation drafts may omit unset amount fields rather than emit null. Explicit zero in a user-owned draft is different and must not be replaced by a truthiness fallback.
- `pricingState: OPTIONAL_SUGGESTION` and `reviewRequired: true` require explicit adult review. `difficultySuggestion` is independent of estimated minutes. Flags are review context, including basic-rights/wellbeing concerns; do not automatically price these activities.
- The service returns defensive copies; reads never save workspace templates, activate definitions or post balances/ledger changes. Suggestions do not silently override stored family amounts or prices.
- The catalog has 142 entries and six packs. QA's expected pack membership split is 99 in packs / 43 outside; authenticated DEV acceptance remains separate from classpath/source verification.

## 2. Draft, import and activation semantics

Base: `/workspaces/{workspaceId}/templates`. Workspace-manager authorization is required. Workspace POST/PUT/PATCH/DELETE routes remain subject to the epoch filter: send `X-Workspace-Data-Epoch`, including preview-like POST routes after the first reset. Missing header is legacy-compatible only at epoch zero.

| Operation | Implemented effect |
|---|---|
| GET `/{templateId}/instantiate-preview` | Scoped read-only private template projection |
| GET `/{templateId}/creation-draft` | JSON `{templateId,kind,request,economySuggestion?,requiresConfirmation:true}`; assignment/currency selection still required as indicated |
| POST `/{templateId}/creation-draft/apply-economy-suggestion` | Explicit opt-in changes ONLY the returned draft amount/currency, adds `suggestionAppliedToDraft:true`; does not persist or activate |
| POST `/imports/preview`, body `{selectedCodes:[...]}` | Selection/dedupe preview; zero selection supported; not activation |
| POST `/imports`, same body | Copies selected catalog records to private library, preserving catalog code/version; does not create active work or purchases |
| POST / PUT / DELETE private templates | Explicit private-library persistence; existing family edits are not automatically repriced |
| POST `/{templateId}/activate-task` | Explicit editable task request, `Idempotency-Key: UUID`, workspace management plus CREATE_TASKS |
| POST `/{templateId}/activate-reward` | Explicit editable reward request, `Idempotency-Key: UUID`, workspace management plus MANAGE_REWARDS |

Creation-draft suggestions are separate from `request`; a stored private price remains the draft's price. Explicit apply can replace the returned draft amount because the parent requested it, but stored data remains unchanged until a subsequent explicit persistence/activation action. Activation does not automatically follow import/apply. `requiresConfirmation` is a UX flag, not a cryptographic proof or an additional audited approval token.

Applying a recommendation requires an integral suggested amount, applicable pricingState, and the active POINTS currency in the TARGET workspace. IDs from another family or catalog placeholders must not be reused. The server resolves scoped currency IDs. TIME 30-minute values must remain 30 when mapped to defaultDurationMinutes; difficulty is not duration.

Current activation replay behavior is conservative: an already claimed operation returns 409 and requires refresh; it does **not** promise an identical success body for arbitrary replay. Failed downstream creation rolls back the claim. This is distinct from future reset-execute idempotency, which remains unavailable.

## 3. Implemented media targets versus catalog classifications

The binding API validates string target types; it does not expose REWARD as a writable target:

| Binding type | Allowed slot(s) | Scope |
|---|---|---|
| WORKSPACE | COVER, ICON | Target ID equals active workspace |
| MEMBER | AVATAR | Active member of this family |
| MEMBER_GROUP | ICON | Active people group of this family |
| WORK_NODE | ICON | Active WorkNode of type GROUP in this family |
| TASK | ICON | TaskDefinition of this family |

**REWARD, REWARD_CATEGORY and TASK_CATEGORY are NOT writable binding target types.** Catalog `category: REWARD` or `TASK_CATEGORY` and `tags:[CATEGORY]` are display/filter metadata, not accepted binding target/slot values. Do not build persistence calls against speculative targets. Defaults for reward cards/private templates can be client presentation choices until a separately implemented binding contract exists.

Implemented endpoints under `/workspaces/{workspaceId}/media`:

- GET `/catalog`: `[{key,category,labelKey,version,selectable,previewPath,tags,sortOrder}]`.
- GET `/catalog/{key}/preview`: category assets are image/svg+xml, known PNG choices are image/png; nosniff and private,no-store headers. Requires active workspace membership even for reusable bundled artwork.
- GET `/bindings/{type}/{targetId}/{slot}`: effective `{source,mediaId,catalogKey,version,path}`.
- PUT same path: `{assetId:null|UUID,catalogKey:null|string,expectedVersion:integer}`. Exactly one assetId/catalogKey; expectedVersion required (zero for a new binding).
- DELETE same path with `?expectedVersion=...`: clears with a versioned tombstone; stale versions return 409.
- POST `/references`: bounded `{targets:[{type,targetId,slot}]}` projection (1..100 inputs), all-or-nothing authorization; this read-only POST still goes through the workspace mutation epoch filter.
- POST multipart `/uploads`, part `file`: validated JPEG/PNG only. This is not an SVG upload endpoint.
- GET `/{assetId}/variants/{variant}`, variant COVER/CARD/THUMB: private authorized binding-based image access, not unrestricted UUID access.

Approved category SVG keys (a subset of the larger catalog, not the total catalog count):

- `category-achievements`
- `category-activity`
- `category-adult-tasks`
- `category-chores`
- `category-creativity`
- `category-family-communication`
- `category-family-events`
- `category-help-others`
- `category-home`
- `category-initiative`
- `category-learning`
- `category-material-reward`
- `category-nature`
- `category-planning`
- `category-practical-skills`
- `category-privilege`
- `category-screen-time`
- `category-self-care`

Explicit own bindings win. TASK may inherit the direct WorkNode GROUP icon, then ancestors, with bounded traversal; otherwise use existing server default. Clearing retains the version so ABA/replay cannot silently restore an old binding. Existing explicit family choices must not be rewritten by category presentation defaults.

Current coarse ACLs remain server authoritative: own member avatar versus other members, people-group access, task/work-node global visibility and manager permissions. Do not infer delegated media access merely from a client-visible task. Fetch previews with authentication and active-workspace scope; segregate caches by account/workspace and clear/quarantine them when epoch/account/workspace changes. Android must render SVG through its verified SVG pipeline, not raw BitmapFactory.

## 4. Errors and compatibility

Existing business rejection bodies handled by GlobalExceptionHandler have `timestamp,status,error,message`; there is no universal stable `code` field. Security/filter errors can have different envelopes. Do not parse localized prose to decide behavior.

- 400: invalid selection/kind/JSON/media target/slot/version or malformed request.
- 401/403: authentication/permissions/workspace checks, subject to security transport.
- 404: missing/scoped-out template, currency or media target; inaccessible private media may be hidden as not found.
- 409: stale/missing-after-zero epoch, media version conflict, duplicate activation claim or mutation conflict.
- 422: invalid stored template for creation, unavailable applicable numerical suggestion, or missing active family POINTS for apply.
- Endpoint-specific reason strings can appear in message, but are not a new typed error schema.

No new DTO fields, migration, automatic pricing, retroactive ledger changes, reset or deployment is introduced by this contract document. Existing request/response shape remains as implemented. A future writable reward-media target needs separate schema/ACL/version/consumer review.

## 5. Evidence and remaining consumer acceptance

Source/API inspection: TemplateCatalogController, TemplateEconomySuggestionsService, WorkspaceTemplateController, MediaController/MediaService and SecurityConfig at the baseline above. CI regression includes:

- TemplateEconomySuggestionsServiceTest: 110/32 coverage, 11/12 numeric suggestions, null preservation, defensive copies.
- WorkspaceTemplateControllerTest: custom price retained in creation draft, suggestions separate from request, explicit apply does not write, unset-price refusal, target-family currency lookup, 30-minute TIME mapping and explicit duration precedence.
- MediaCatalogServiceTest: renderable registered assets, version/display metadata, authorized scoped preview and resource-path rejection.
- Real Spring/JPA/PostgreSQL tests: template activation claim rollback, media upload/binding/clear rollback and no implicit ledger posting.

Web/Android still own explicit adult-acceptance UX, actual target support, four-language/light-dark rendering, authenticated cache isolation and SVG display. QA must pin actual DEV SHA and run exact payload/142/6/18-preview/tenant/role/null/30-minute checks. This document is Backend source sign-off for existing supported behavior, not a claim those consumer/DEV/BETA checks passed.
