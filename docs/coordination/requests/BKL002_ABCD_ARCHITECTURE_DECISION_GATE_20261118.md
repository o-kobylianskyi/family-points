# BKL-002 — Architecture decision gate for Backend A/B/C/D

**Date:** 2026-11-18. **Priority:** P0. **Status:** DECISIONS_REQUESTED, not approved. **Prepared by:** coordination audit. **Scope:** unblock parallel design without authorizing production reset, restore, migrations or access changes.

## Observed exact source evidence
- Backend A `docs/coordination/requests/B-A-02_PORTABLE_MEMBER_SOURCE_CONTRACT_20261118.md`: baseline `family_members` id/family_id/family_role_id/member_type/name/active/deleted_at, V21/V22 account memberships, V29 global identity and V31 duplicate emails. Source archive has 23 V50 categories and no `FAMILY_MEMBERS`; proposes privacy-minimal authenticated source ancestry, archived tombstones and hidden identity dependency. A-MEM-01..12 tests proposed, **no runtime code**.
- Backend B `docs/coordination/requests/BACKEND_B_RESTORE_DEPENDENCY_MATRIX_20261118.md`: enumerates 23 source categories and source→target stages, requires authenticated original member parentage and explicit target member mapping; code reports `IDENTITY_REBIND_REQUIRED`, `restorable=false`.
- Backend C `docs/coordination/requests/BACKEND_C_SECURITY_RESET_AUDIT_20261118.md`: three Reset HTTP controllers use `canManageWorkspace` allowing `MANAGE_WORKSPACE` / `ADMIN_OVERRIDE`; these are not original-owner proof. Existing password challenge and 5-minute DB proof are useful but insufficient; `/execute` stays HTTP423.
- Backend D `BACKEND_D_LATEST.md` and `logs/backend-d.md` absent as of inspection. Do not infer D acknowledgment from finance tests committed by other tracks.
- Approved policy `docs/architecture/ADR-FAMILY-RESET-002.md` is authoritative; no guess-based legacy owner designation; user/global identity retained, no cross-family writes.

## Architecture decisions requested (one accountable ACK per row)

| ID | Decision | Proposed safe default pending ACK | Required review / consumer |
|---|---|---|---|
| **ABCD-DEC-01** | Owner source-of-truth and legacy enrollment | Explicit durable original-owner global account association created atomically with new family; legacy owner unestablished until authenticated audited enrollment; never infer from role/name/lowest member ID | Architecture + C + B, Security/QA |
| **ABCD-DEC-02** | Portable original member catalog | Signed, privacy-minimal original family member/source role/type/tombstone ancestry as mandatory **hidden graph dependency** for member-referencing categories; no raw credentials, email, OAuth subject or automatic target account creation | Architecture + A producer + B consumer + D financial verifier |
| **ABCD-DEC-03** | Global-account provenance in portable backup | Archive-local opaque account reference, if allowed, is *not* target auth identity; owner-reviewed target pairing remains separately authenticated; shared accounts and deleted members fail closed without reviewed policy | Architecture + A + B + C |
| **ABCD-DEC-04** | Wire version and old archives | Explicit vNext archive/category version and manifest coordination, no silent V50 reinterpretation; old v1 may remain inspectable but must not become restorable without needed provenance | Architecture + A + B, Release |
| **ABCD-DEC-05** | Owner approval / action separation | C owns real fresh owner auth and durable permit issuance; Reset and Restore action scopes separated; B consumes Restore permit atomically with target replay, C consumes Reset permit atomically with reset; shared lock-order decision under Architecture | Architecture + C + B + QA |
| **ABCD-DEC-06** | Financial selective dependency closure | D defines supported source types, group/reputation/points/obligation policy, historic refund meaning and exact immutable reconciliation; A exports source graph, B applies mapping, C uses approved policy in Reset preview; unsupported type blocks execution | Architecture + D + A + B + C |
| **ABCD-DEC-07** | Snapshot source consistency | Archive capture uses proven transaction-consistent source view/digest/epoch; altered data or 12MiB/row bound must fail closed without truncated archive; media bytes accounted for before VERIFIED | Architecture + A + B + Release |
| **ABCD-DEC-08** | Shared schema, DTO, service ownership | A owns source archive schema/serializer; B target executor and Restore receipt; C owner/auth/Reset proof and execution; D pure financial invariants; shared Flyway/REST/DTO require named IMP and all impacted consumer ACKs | Architecture + all active streams |

## Allowed parallel work pending decisions
- A: nonmutating source FK/category inventory; test-only archive privacy/source digest fixtures, no new production FAMILY_MEMBERS category.
- B: offline consumer graph validation and isolated PG18 transaction/replay rehearsal, no production Restore enable or unilateral archive change.
- C: owner/auth flow gap inventory, negative tests using synthetic owner binding, no release of Reset endpoint and no real-owner guesses.
- D (once present): inspect real Task/Reward/PointExchange/Refund/group/reputation producers; produce signed financial graph coverage table and precise A archive requirements; no financial policy guess.
- QA: exact-SHA independent tests and accepted bug reports; no destructive real-family tests.
- Release: historical full CI evidence per immutable SHA, future candidate gates; no deploy.

## Required coordination response
1. Architecture record each **ABCD-DEC-01..08** as `ACK / REVISE / BLOCKED` with rationale and owner in Architecture journal or append to this document, and create/update relevant `IMP-XXXX` before shared migration/contract changes.
2. Backend B ACK/REVISE Backend A's B-A-02 original member shape, listing all missing source fields and source→target semantics; A must wait before producing new archive wire format.
3. Backend C propose owner binding + reauth interface, including Google-only fail-closed and eventual provider proof; B review transaction lock/permit consume boundary.
4. Backend D create distinct `BACKEND_D_LATEST.md` and `backend-d.md` on pickup; inventory REFUND and group/reputation policies with negative tests.
5. Each role refresh current SHA, append own handoff and QA requests. Posts in issue **do not automatically notify independent ChatGPT chats**; each role must read coordination requests at next pickup.

**Stop condition:** No production `VERIFIED`, `restorable`, `replaySupported` or Reset Execute unblocking without separate code, exact-SHA CI, independent QA/Release and explicit authorized approval. **BETA HOLD.**
