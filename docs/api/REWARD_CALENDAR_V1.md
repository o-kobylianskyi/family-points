# FamilyPoints — Backend Calendar + Reward Acquisition Windows API (draft PR #4)

**Source:** `feature/backend-calendar-rewards-v1`. **Status:** code committed, Java/Maven CI passed on `6b11bbf`; PR #4 remains DRAFT. This API is **not yet available in the deployed BETA**, and has **not been reviewed by Web/Android**. XTR-20261022-P-01 · IMP-0006 · ADR-CAL-001.

## Endpoints (scoped to selected family workspace)

`GET /workspaces/{workspaceId}/calendar?from=2026-10-26&to=2026-11-26&memberId=3&type=ALL`

- Authenticated workspace member only. Optional `memberId` defaults to current member; another member requires `VIEW_ALL_TASKS` or `ADMIN_OVERRIDE`.
- `from` and `to`: inclusive local dates, **max 62 days**; `type=ALL|TASK|REWARD`; result capped at 2,500 events, sorted.
- `TASK`: `{kind,key,date,title,status,memberId,taskDefinitionId,instanceId,scheduledTime,dueAt,...}`. The time is a **deadline**, never a start time. Persisted task instance state overrides virtual planned occurrence. `instanceId=null` means virtual only.
- `REWARD_WINDOW`: `{kind:"REWARD_WINDOW",key,date,title,status,memberId,rewardId,startsAt,endsAt,scheduleOpenNow,...}`. **ALWAYS** rewards have no dated window events, but remain in the reward catalog. Recurring windows are computed, not materialized. Reward `scheduleOpenNow` means **time gate only** and does **not** imply adequate balance, prerequisites, authorization or approval.
- The response is a JSON array of `CalendarEventResponse`; no write occurs in this GET. Exact field set follows that record in Java source. Counts/filters beyond this scope need separate contract review.

`GET /workspaces/{workspaceId}/rewards/{rewardId}/availability`
- Any authorized workspace member may inspect a reward that belongs to their workspace.
- Response: `rewardId`, `mode`, `workspaceTimeZone`, `startsAt`, `endsAt`, `weeklyWindows`, `scheduleOpenNow`, `availableUntil`, `nextOpenAt`, `reason`. `nextOpenAt` for weekly is searched through 366 upcoming days; null means none found within horizon.

`PUT /workspaces/{workspaceId}/rewards/{rewardId}/availability`
- Requires `MANAGE_REWARDS` or `ADMIN_OVERRIDE`.
- Example payloads:

```json
{"mode":"ALWAYS","startsAt":null,"endsAt":null,"weeklyWindows":[]}
```

```json
{"mode":"ONE_TIME","startsAt":"2026-11-05T18:00:00+01:00","endsAt":"2026-11-05T20:00:00+01:00","weeklyWindows":[]}
```

```json
{"mode":"RECURRING_WEEKLY","weeklyWindows":[{"windowId":"weekday-1","dayOfWeek":1,"localStartTime":"16:00:00","localEndTime":"18:00:00","effectiveFromDate":null,"effectiveToDate":null,"enabled":true}]}
```

- `dayOfWeek` is ISO 1=Mon..7=Sun; up to 56 windows; IDs ASCII alphanumeric, underscores or hyphens; ranges half-open `[start,end)`, same day only, no overlaps on same weekday with overlapping effective dates; effective dates inclusive.
- `ONE_TIME` instants stored UTC/TIMESTAMPTZ, and old rewards default `ALWAYS`. Old `PUT /workspaces/{workspaceId}/rewards/{id}` **preserves** existing time windows because schedule has its own endpoint.
- Purchase outside window from direct or approved catalog request returns HTTP **409** with error message/reason `REWARD_OUTSIDE_AVAILABILITY_WINDOW`; no reward purchase/point debit/obligation written by that rejected transaction. A free-form request not tied to reward catalog has no catalog time window.

`GET /workspaces/{workspaceId}/calendar/time-zone`
`PUT /workspaces/{workspaceId}/calendar/time-zone` with `{"timeZone":"Europe/Berlin"}` requires `MANAGE_WORKSPACE` / `ADMIN_OVERRIDE`. Region-based IANA zone required. Existing workspaces get explicit **UTC** default from V35; an admin must choose their local region. The new zone affects schedule calculations but does **not** rewrite historical LocalDateTime tasks, legacy purchase quotas/cooldowns, or past ledgers.

## Open compatibility/release gates

1. Do **not** equate `scheduleOpenNow` with final purchase eligibility. Server always reevaluates during purchase.
2. A future UI may submit a catalog reward request before opening; only final acquisition is time-gated. Time-based usage (e.g. Device Owner enforcing screen minutes) is an independent future feature.
3. Existing legacy task deadlines and quota timestamps are JVM-local; automatic migration to workspace zone was intentionally **not** implemented. Coordinate separately if later changing daily/weekly quota semantics.
4. DST policy: Java IANA zone rules shift spring-gap local times forward; earlier offset chosen for autumn overlaps. A gap may collapse a configured window for that occurrence.
5. Security: requested member and reward IDs must belong to active workspace, other member schedule visibility is restricted; 2-workspace integration tests and client E2E still needed.
6. Verify Flyway V35 fresh-install and upgrade against disposable PostgreSQL, plus CI, integration/device tests and Release gate. This document alone does not publish the API.

**Producer:** Backend track. **Consumers:** Web, Android, Architecture and Release, pending review/ACK in IMP-0006.

## Interim BETA Flyway integration decision — 2026-10-24
Existing `main` introduced emergency V35 for REWARD_PURCHASE, validated by CI. This draft feature's migration has been **renumbered to V36** to avoid the duplicate V35 checksum/name. The V36 migration is new and must not be rewritten after it has been applied to a persistent database. Older disposable PR #8 V35/V36 combined proof is obsolete and must never merge. Release must verify main V35 → calendar V36 → media V37 on a backed-up populated BETA clone, and review old disposable/dev Flyway checksums first.

## Integrated pre-BETA candidate — PR #13
Calendar migration is **V36** after V35 reward-ledger hotfix and before V37 Media. Calendar and acquisition-window authorization are integrated into current reward service, without discarding existing reward purchase idempotency and concurrency checks. This remains a draft release candidate; use the QA handoff for test cases and do not infer deployment from code status.
