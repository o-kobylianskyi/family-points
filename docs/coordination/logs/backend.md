# FamilyPoints — Backend journal

New technical batches: Java/Spring, PostgreSQL/Flyway, DTO/API, authorization/tenant isolation, transactions and tests. Retain historical work in root development log.

## 2026-10-22 — Track initialized (DOCUMENTATION ONLY)
- Incoming: IMP-0001 secure tenant-scoped images; IMP-0002 auth/Google/username/CORS; IMP-0004 multi-workspace rights; IMP-0005 offline idempotency.
- Verify actual code, DTOs and endpoint behavior at current Git SHA before acknowledging implementation. No backend changes or tests in this setup.

## 2026-10-22 — Backend acceptance of XTR-20261022-P-01 (ACKNOWLEDGED; SOURCE REVIEW ONLY)
- **Reviewed branch/HEAD:** `feature/task-soft-delete` at `89cbf1e026815dc59d2b1fc5ab9ce06549382524` before recording this note. Also read `AGENTS.md`-governed IMP register, ADR-CAL-001, Architecture journal and current cross-track request.
- **New inbound assignment:** XTR-20261022-P-01 / IMP-0006 — add unified virtual TASK + REWARD_WINDOW calendar projection and server-authoritative reward acquisition windows. P1 foundation; source/API blocked for Web/Android until Backend DTO agreement. Product calendar UI agreed; backend schema/timezone/contract still proposals.
- **Source verified (code inspection, not tests):** RewardDefinition and RewardDefinitionRequest/Response currently have no availability windows; RewardService.purchaseReward and purchaseRequest have transactional member lock and existing quotas/requirements/point debit but no time gate. TaskService.getMemberSchedule is one member/day, virtual projected tasks plus persisted instance precedence. Workspace currently has no IANA ZoneId field. RewardController has existing manager PUT reward endpoint. Latest migration observed: V34.
- **Accepted scope:** compatible ALWAYS default, ONE_TIME/RECURRING_WEEKLY intervals, pure Clock/ZoneId evaluator, manager schedule API, transactional checks in both purchase paths, bounded read-only unified calendar, cross-workspace security, regression/DST tests, Flyway migration. Legacy PUT must not erase configured windows; existing task slot semantics and purchase history must remain intact.
- **Open decisions requiring actual implementation review:** workspace timezone migration/default without retroactive deadline/quota shifts; exact DTO/error codes/next opening semantics; DST overlap handling; bounds/batching for calendar requests. Coordinate with Architecture, Web and Android through IMP-0006 before breaking contracts.
- **Other Backend incoming impacts still open:** IMP-0001 media storage/permissions; IMP-0002 auth/Google/registration contract; IMP-0004 multi-family isolation; IMP-0005 offline idempotency. No blanket ACK of these was recorded by this targeted review.
- **Verification:** none — no Java edits, Gradle/Maven/DB migrations, unit/integration tests, CI or deploy in this intake-only action.
- **Next:** review existing auth/tenant release gates (P0), then make additive calendar API/DB design with consumer review, implement as cohesive Backend package and run real DB/tests before handing Web/Android DTOs.

## 2026-10-22 — Draft Backend calendar implementation PR #4 (CODE COMMITTED; CI PARTIAL)
- **Source branch:** `feature/backend-calendar-rewards-v1` based on `feature/task-soft-delete` (do not confuse source PR with deployed BETA). PR https://github.com/o-kobylianskyi/family-points/pull/4 is **DRAFT**, target current integration branch.
- **Code commits:** `e807047b` Flyway V35 + reward availability domain/service + transactional purchase checks; `477299d5` 62-day read-only TASK/REWARD_WINDOW calendar and zone API with batched task query; `22877f95` existing test compatibility and locking schedule edit; `6b11bbf6` boundary/DST/both-purchase-gate unit tests; `a968a1bb` API draft contract documentation.
- **Endpoints proposed in PR:** GET calendar (`from,to,memberId,type`); GET/PUT reward `/{rewardId}/availability`; GET/PUT `/calendar/time-zone`. Contract details: `docs/api/REWARD_CALENDAR_V1.md` **on feature branch**. Default family timezone UTC until administrator chooses IANA region; no legacy local timestamp/quotas rewrite. Old reward PUT preserves schedule; TIME_BASED duration remains separate from acquisition.
- **CI evidence:** GitHub Actions run https://github.com/o-kobylianskyi/family-points/actions/runs/39853638907 at source `6b11bbf6`: **Java 21 / Maven / PostgreSQL Testcontainers SUCCESS**, Web build SUCCESS, BETA Compose validation SUCCESS. Android job still in progress at observation. API-doc-only newer commit not yet individually verified in CI. Live PostgreSQL/BETA/device usage **NOT TESTED** in this chat.
- **Remaining acceptance:** reviewer ACK by Architecture/Web/Android; independent tenant-negative tests, schema fresh/upgrade coverage, daylight saving edge review, performance/security verification and Release gate. Never declare PR merged, BETA deployed or XTR DONE yet. Keep IMP-0006 OPEN.
- **Other unrelated Backend P0 work (IMP-0002, -0004) remains a release gate**; no auth or tenant safety work included in this PR.

## 2026-10-22 — Calendar Backend CI validation at 3199c03 (VERIFIED CI; NO MERGE/DEPLOY)
- Backend feature branch `feature/backend-calendar-rewards-v1` source `3199c031917938a45b64bf83818f48c29693ad79`, draft PR #4: https://github.com/o-kobylianskyi/family-points/pull/4.
- Follow-up source `08a4fb6`: optimize range task projection with assigned-member database query and fetch linked TaskInstance associations; add additive catalog response `availabilityMode` string (new schedule read/update endpoint still authoritative).
- Follow-up tests `3199c03`: dedicated calendar workspace/member access rejection and range cap tests, Testcontainers-based verification Flyway V35 table/columns installed from clean migration chain.
- **CI evidence** https://github.com/o-kobylianskyi/family-points/actions/runs/39854939259 at exact source `3199c03`: Java 21/Maven/PostgreSQL Testcontainers **SUCCESS**, Web production build **SUCCESS**, BETA Docker Compose validation **SUCCESS**; Android CI **IN PROGRESS** when checked. No local Maven, real-device, browser or VPS tests in this chat.
- **Remaining:** consuming tracks' ACK and DTO/UI review, explicit BETA release approval, migrations and timezone behavior review on existing real instance (only disposable Testcontainers tested), broader auth/tenant/cross-client E2E. Do not conflate PR status with deployment.

## 2026-10-22 — New Web-to-Backend media XTR intake (ACK; ADR BLOCKED)
- Checked actual integration branch at `b44c501224d9c234f863fbf86f8a81026b725c88` and identified newly submitted `XTR-20261022-W-01` / `IMP-0007` (Web producer): scoped family/member/group/task images, curated defaults/uploads, category inheritance, secure media delivery, cleanup and storage/backup.
- **Backend ACK of responsibility only**; Architecture media ADR remains prerequisite to final entity/model/API design. No new source, test, migration or deployment in this intake.
- Calendar XTR-P-01 / draft PR #4 is separate and stays READY_FOR_REVIEW; do not conflate stages or merge dependencies.
