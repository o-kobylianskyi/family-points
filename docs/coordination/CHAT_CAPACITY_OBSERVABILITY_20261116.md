# FamilyPoints — temporary cross-chat context-capacity observability pilot

**Decision by user:** 2026-11-16, after Web conversation reached the hard maximum with no advance warning. **Status:** TEMPORARY / OBSERVATION. **Applies to:** Architecture/Planning, Backend, Web, Android, Designer, QA, Release/DevOps and new role chats. **Owner:** Architecture (schema/control), each role (its evidence), QA (audit), Release (optional aggregation only). **Not part of product BETA feature scope.**

## What can and cannot be measured
ChatGPT role assistants have **no trusted API/counter for total context-window tokens, remaining context percentage or absolute conversation length** and cannot automatically inspect other conversations. This pilot MUST NOT claim accurate context percentages or exact numbers when the data is absent. Old role chat may reach a hard cap before any last response. The user screenshot on 2026-11-16 is observed positive evidence for a **hard-limit event**, **not** evidence of its hidden token count.

Possible metrics and their provenance:
1. **Verified human/UI observation:** timestamp, track, `HARD_LIMIT_BANNER` / `LENGTH_WARNING_UI` / `NO_WARNING_BEFORE_LIMIT` with user-supplied screenshot, no sensitive content in repo. A chat maximum banner is a fact; threshold still unknown.
2. **Visible local/role count:** number of messages/turns *actually countable in the accessible current transcript*; mark `PARTIAL_VISIBLE` if older content could be compacted/omitted. Never treat this as a lifetime conversation count. A chat message or screenshot can contain multiple screenshots/files; log separately if known.
3. **Text volume** (characters/bytes/tokens) only for explicitly available messages/log snippets/exports; record method (`MANUAL`, `COUNTED_VISIBLE_TEXT`, `USER_EXPORT`) and coverage; token count of raw text is NOT the model's true processed context, due to images, hidden system/tool context, compression and caching.
4. **Elapsed time and workload proxies**: first/last known date, count of substantial work batches, code/CI tool calls if locally counted, files/artifacts/log excerpts if measured. These are proxies, not token percent.
5. **Handoff recency:** rolling `<TRACK>_LATEST.md` last committed SHA/time and number of substantial batches since update. This is the only direct preventable continuity risk we can control regardless of hidden limits.

## Recording rule
At **every substantial work batch** (i.e. source change, audit, tests, handoff or issue coordination), append **one compact checkpoint** to `docs/coordination/chat-capacity/<TRACK>.md` using the table below. Avoid one commit per chat line: log once per batch. If the role has no direct source or reliable measurement, set metric `unknown`, `not_available` or `PARTIAL_VISIBLE`; never invent a number. Include complete task summary in the regular track log, not in metric table.
- Update and commit the rolling continuation `docs/coordination/handoffs/<TRACK>_LATEST.md` **in the same work batch**. No dependency on guessed thresholds.
- Explicitly report real UI hard-limit banners as an `EVENT` at any time; if old chat blocked, coordinator or replacement chat records the event from user's screenshot.
- For 50 / 75 / 85 / 90 / 95 percent: **advisory only**; do not display a threshold crossed without a defensible method or claim it is system-reported.
- Do NOT store conversation bodies, screenshots of private family/workspace data, raw messages, secrets, email, child details, user IDs, passwords or personal conversation transcripts in Git. Only anonymized counts and coarse observations.
- Do NOT instrument ChatGPT internals, scrape hidden tokens, or use inaccessible APIs. User may later decide whether to export local chat history for batch analysis; that is separate opt-in.

## Checkpoint table schema
One entry per substantial batch, with this minimal consistent row:

`DateTime CET | Track | ChatGeneration | VisibleUserTurns | VisibleAssistantTurns | Visibility | MeasuredTextChars | MeasurementMethod | WorkBatchesKnown | LatestHandoffSHA | Event | Notes`

- `ChatGeneration`: user-defined track sequence, e.g. `WEB-02`; **not** an actual ChatGPT conversation ID.
- `VisibleUserTurns`/`VisibleAssistantTurns`: counted **only if directly accessible**, else `unknown`.
- `Visibility`: `FULL_VERIFIED_EXPORT` / `PARTIAL_VISIBLE` / `NOT_AVAILABLE`.
- `MeasuredTextChars`: integer only if genuinely measured, else `unknown`.
- `MeasurementMethod`: `USER_EXPORT` / `COUNTED_VISIBLE_TEXT` / `USER_UI` / `NONE`.
- `WorkBatchesKnown`: role's recorded batches since latest chat start, if tracked, else `unknown`.
- `LatestHandoffSHA`: real committed handoff SHA, else `NOT_READY`.
- `Event`: `CHECKPOINT`, `HARD_LIMIT_BANNER`, `EARLY_HANDOFF`, `NEW_CHAT`, `LENGTH_WARNING_UI`.
- `Notes`: only non-sensitive concise observations.

**Example for historical Web incident (known fields only):**
`2026-11-16 ~21:11 CET | Web | WEB-old (exact generation unknown) | unknown | unknown | NOT_AVAILABLE | unknown | USER_UI | unknown | 6d84766f | HARD_LIMIT_BANNER | User screenshot: maximum chat length, no prior warning reported`.

## How we use results later
- Until enough *actual* full conversation histories have been exported and measured, **no credible limit or percentage can be calculated**.
- Compare *fully measured* ended chats only against other fully measured chats; do not divide partial visible turns by a guessed maximum. Bound estimates by observation method and variability of tool/image/code workload, chat compaction and model changes.
- As soon as a useful sample exists, Architecture/QA may propose a **conservative empirical early-handoff interval** based on observed lower-tail message/character sizes, with caveats. Do not claim it is a ChatGPT token limit.
- Default safety meanwhile: durable handoff after every substantial batch, independent of percentages.

## Roles and acceptance
- **Architecture:** own schema and aggregate trend review; audit contradictions and false precision; ensure every track participates.
- **Backend/Web/Android/Designer:** collect own checkpoints and maintain rolling handoff; keep feature development and QA handoff normal.
- **QA:** also log own capacity checkpoints, periodically check each role's log for fresh entries and valid provenance; issue a deficiency if absent.
- **Release/DevOps:** log own checkpoints; no special infra/script needed; optional read-only aggregation of the files, not automatic access to ChatGPT messages.
- **All:** when a chat unexpectedly stops, replacement chat can recover from `<TRACK>_LATEST.md`; do not wait for a percentage alarm.

## First checkpoint deadline
Each role **on its next active pickup or substantial work batch** creates/updates `docs/coordination/chat-capacity/<TRACK>.md` and `docs/coordination/handoffs/<TRACK>_LATEST.md`, linking this protocol. Other chats do not receive an immediate automatic message from a GitHub commit; each must read AGENTS.md and pending coordination notes.
