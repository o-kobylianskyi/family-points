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
