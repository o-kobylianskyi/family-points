# FamilyPoints — Architecture / Planning journal

Chronological log of architecture, planning, cross-track decisions and priorities. Existing concepts are in the root concept journal.

## 2026-10-22 — Five permanent tracks (AGREED; DOCUMENTATION ONLY)
- Adopted Architecture/Planning, Backend, Web, Android, Release/DevOps as five independent development conversations.
- Set mandatory Git review and change/handoff process via AGENTS.md and docs/coordination/; each track has own append-only journal.
- Created shared impact queue requiring producer disclosure and consumer acknowledgement, plus ADR list and dated status snapshot.
- Preserved existing project history and checklist. Seeded IMP-0001..0005. ADR-COORD-001..003.
- No code/build/server/APK changes or runtime tests performed during this documentation change.
- Next: assign/prioritize cross-track acknowledgements; architecture decisions for images, workspace permissions, offline ledger, client compatibility.

## 2026-10-22 — Architecture / Planning (P) track ownership accepted; initial coordination review (DOCUMENTATION ONLY)
- **Scope:** architectural ownership, domain invariants, roadmap/dependency ordering, ADRs, shared API/DB/auth/offline/media contracts, cross-track impact arbitration and handoff policy. Backend, Web, Android and Release retain implementation and verification ownership of their tracks.
- **Verified remote source:** `feature/task-soft-delete` at `e729d1d7f63e83363db6cd739191cb638402ca39` immediately before this journal entry; default `main` is older. Future sessions must refresh HEAD rather than reuse this SHA.
- **Reviewed:** `AGENTS.md`, coordination README/state/protocol/impact/ADRs/handoff, five track journals, current and concept checklists, `docs/MULTI_FAMILY_ONBOARDING_PLAN.md`, Web acceptance notes and existing development/deployment history. Existing ADR-COORD-001..003 and IMP-0001..0005 remain authoritative and must not be recreated.
- **Initial impact triage (DOCUMENT REVIEW ONLY — no consumer ACK or test claim):**
  - IMP-0004: workspace/identity scoping and two-family negative authorization tests are public-release security gates; reconcile the design plan with currently committed code.
  - IMP-0002: registration, Google, invite, CORS and feature flags need actual Backend/Web/Android/Release contract and BETA review.
  - IMP-0003: the Android `IllegalAccessError` source mitigation is not proof of a working signed APK; real-device acceptance and Release approval precede publication.
  - IMP-0001: family cover and member avatar media lifecycle, tenancy/security, client contracts and cache policy require ADR before implementation.
  - IMP-0005: Android offline task/points queue requires a server-authoritative idempotency/conflict/replay ADR before enabling mutations.
- **Sequencing proposal, not a release order or implementation approval:** P0 confirm tenant security/auth consistency and unblock verified APK + independent Release gates; P1 agree media, workspace-session compatibility and offline ledger contracts; then coordinate UX parity and deeper feature delivery without crossing unreviewed shared APIs. Web/Android can continue independent, contract-preserving UI work concurrently.
- **Pending Architecture actions:** source-level review before marking its IMP acknowledgements; produce ADRs with compatibility/migration/negative tests and owners; keep producer/consumer checkboxes open until evidenced; synchronize snapshot and root checklist upon material state changes.
- **Evidence limitations:** no source modifications, builds, tests, CI status, VPS/DB deployment or real-device checks performed as part of this Architecture initialization.

## 2026-10-22 — Distributed planning and cross-track task delegation (ADR-COORD-004, AGREED; DOCUMENTATION ONLY)
- User clarified that planning can occur in any direction-specific chat, depending on ongoing development; Architecture is not a universal planning/approval queue.
- A track finding required work outside its ownership prepares an executable technical specification for the responsible track, saved as a Git-tracked `XTR` request with acceptance criteria, evidence, dependencies and status.
- Receiving chat independently fetches and acknowledges Git request; multiple destination tracks receive separate linked requests. Producer/consumer reviews and `IMP-XXXX` requirements still apply for shared contracts. Release retains publishing approval.
- Updated AGENTS, coordination README, CHANGE_PROTOCOL, HANDOFF_TEMPLATE and ADR list; added dedicated requests documentation/template. No app code, database, build, server, APK or CI changes were made.

## 2026-10-22 — Calendar for tasks and rewards; timed reward availability (DESIGNED; NOT IMPLEMENTED)
- User requested a shared calendar of tasks and rewards while Web/Android work on approved styling; reward acquisition must additionally respect date/time besides task-completion prerequisites.
- Inspected TaskDefinition/TaskScheduleSlot/TaskInstance virtual projections, TaskGenerationService and TaskService, TaskDeadlineService, RewardDefinition/Requirement/RewardService, DTOs, existing RewardsPage, TaskScheduleEditor and Android schedule DTO, and historical concept/checklist rules on 2026-10-22.
- Preserved current task occurrence identity, deadline semantics, read-only future projections and historical ledger. Noted reward schedules do not exist yet; current reward day/week limits and task deadlines use server-local LocalDateTime and Workspace lacks IANA timezone.
- Wrote `docs/architecture/ADR-CAL-001.md` design and IMP-0006; delegated Backend XTR-20261022-P-01 (first), Web XTR-20261022-P-02 and Android XTR-20261022-P-03 (after their style milestones). Each receiving track must independently ACK and refine its contract; Architecture does not implement their code.
- Planned reward modes ALWAYS/ONE_TIME/RECURRING_WEEKLY; *acquisition-time* gate, not usage entitlement; both purchase paths, old-client compatibility and timezone/DST constraints. Tests, migrations, UI, deployment and device verification NOT run.

## 2026-10-22 — User-selected calendar visual reference (LAYOUT AGREED; REFINEMENTS PROPOSED)
- Reviewed the user's 1200×1200 JPEG family agenda/calendar reference (SHA-256 `2bfb18d08d237c48c88568414d48893bb5a5e1b74c07220a1ed73f33edf8dfe7`). The user selected it as a FamilyPoints calendar composition base; original image remains a conversation attachment, not a Git-tracked binary.
- Selected: compact greeting/summary tiles, Today/Tomorrow switch, vertical date/time rail and avatar markers, separate no-time chores, rounded bottom navigation. Adapt to shared TASK/REWARD_WINDOW semantics and existing Soft Family light/dark themes; do not replace theme palettes or create fictional events.
- Recommendations documented: chronological sorting (source screenshot is out of order), distinguish deadline from start and reward acquisition windows, parent/child role-aware tiles, full week/month drilldown, accessible status indicators, safe-area footer, offline read-only, no unrelated microphone/Inbox feature.
- Spec: `docs/FAMILYPOINTS_CALENDAR_UI_REFERENCE.md`. Updated Web/Android XTR-20261022-P-02/03 references; work still scheduled after ongoing styling and Backend contract. No code/build/device/server/deployment changes made in this batch.

## 2026-10-22 — Calendar UX recommendations explicitly approved (ADR-UX-CAL-001, AGREED; DOCS ONLY)
- User confirmed full agreement with previously recommended adaptations of uploaded family agenda reference.
- Promoted `docs/FAMILYPOINTS_CALENDAR_UI_REFERENCE.md` from proposed recommendations to AGREED product design baseline, preserving non-implemented checklist.
- Recorded two distinct destinations (Today/Home agenda and full day/week/month/range calendar), role-based summary cards and member visibility, chronological TASK/REWARD_WINDOW timeline, untimed task list, five-tab nav recommendation, accessibility and existing Soft Family light/dark themes; no Inbox/microphone copy.
- Updated XTR-20261022-P-02 (Web) and XTR-20261022-P-03 (Android) for confirmed UI scope, and ADR-CAL-001 to reference explicit UX approval while Backend contract remains PROPOSED pending review.
- Original uploaded 1200x1200 reference verified SHA-256 `2bfb18d08d237c48c88568414d48893bb5a5e1b74c07220a1ed73f33edf8dfe7`; original binary remains outside Git. No app source, tests, DB, APK or VPS modified.
