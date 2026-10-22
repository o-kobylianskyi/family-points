# FamilyPoints — Git coordination hub

Established 2026-10-22 by project decision. **Five permanent, separate development chats**, with Git as the shared durable record. Chats do not synchronize automatically; each independently fetches current documentation and commits.

| Direction | Ownership | Routine append-only log |
|---|---|---|
| Architecture / Planning | Requirements, domain design, ADRs, priorities and cross-track contracts | [Architecture](logs/architecture.md) |
| Backend | Java/Spring, REST/DTO, PostgreSQL/Flyway, tenant security, logic and tests | [Backend](logs/backend.md) |
| Web | React/Vite, Soft Family design, accessibility, localization, web API clients | [Web](logs/web.md) |
| Android | Kotlin/Compose, native UX, offline sync, device integration and Android build code | [Android](logs/android.md) |
| Release / DevOps | CI, VPS, DEV/BETA/PROD, container/DB, tagged releases and APK publication | [Release](logs/release.md) |

## Decentralized planning and direction-specific handoffs

Every track can plan and implement its own work. Architecture / Planning is **not** a mandatory planning gateway; it arbitrates shared contracts, conflicting decisions and cross-domain priorities. When a task belongs to another track, the current chat writes an actionable specification addressed to that track, rather than changing another track's code or relying on chat-to-chat context transfer. Multi-track work is decomposed into owned tasks with explicit dependencies.

Cross-track tasks are kept as individually tracked files in [requests/](requests/) using [TASK_TEMPLATE.md](requests/TASK_TEMPLATE.md); the originating chat also shows the handoff to the user. The target chat picks up the request on its next session or when directed by the user, reviews and acknowledges it, then owns implementation and its own journal. There is no automatic delivery between chats. Shared-contract changes **also** require an `IMP-XXXX` entry; a task request is not a substitute for impact review.

## Shared coordination documents
- [PROJECT_STATE.md](PROJECT_STATE.md): dated status and observed SHA, important dependencies, gates; **snapshot, not live monitor**.
- [IMPACT_REGISTER.md](IMPACT_REGISTER.md): proposed/current changes that affect other tracks, consumers and review acknowledgements. **Mandatory at chat start.**
- [ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md): agreed decisions and deliberate supersessions.
- [CHANGE_PROTOCOL.md](CHANGE_PROTOCOL.md): contract change, integration, commit and release protocol.
- [HANDOFF_TEMPLATE.md](HANDOFF_TEMPLATE.md): context handoff to the next conversation.

## Existing historical records MUST remain accessible
`FAMILYPOINTS_DEVELOPMENT_LOG.md` (technical history), `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md` (domain decisions), `FAMILYPOINTS_CONTABO_DEPLOYMENT_LOG.md` (operations), `FAMILYPOINTS_CURRENT_WORK_CHECKLIST.md` and `FAMILYPOINTS_CONCEPT_CHECKLIST.md`. Do not replace these with short summaries. Continue the appropriate historical journal for major events; routine work goes into the five track logs.

Evidence order: actual code at exact SHA for committed work; command/test logs for verified behavior; deployment/runtime observation for what's live; architecture docs for intent. Distinguish PROPOSED/AGREED/COMMITTED/TESTED/DEPLOYED/BLOCKED. A Git log is not proof of production deployment.
