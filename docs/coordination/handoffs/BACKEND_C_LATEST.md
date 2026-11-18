# FamilyPoints — Backend C CLOSED / Final Handoff to Backend B

**Date:** 2026-11-18  
**State:** **CLOSED, no longer an independent workstream.** Per the project owner's directive in [Issue #15](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6974473668), **Backend B is the single primary backend implementer/integrator** for pending Reset/Restore security and former C tasks. Do not create new Backend C runtime/tests/docs work unless explicitly reopened by the owner.

**Canonical complete final handoff:** [BACKEND_C_FINAL_20261118.md](BACKEND_C_FINAL_20261118.md) — source/test inventory; C→B binary V2 canonical contracts; original-owner enrollment and reauth gaps; production durable grant and transaction/receipt requirements; exact-SHA QA proof and remaining gates.

**Accepted source:** `WorkspaceResetProofService`, `ResetChallengeAttemptLimiter`, `ResetOriginalOwnerPolicy`, `OriginalOwnerEnrollmentEvidence`, `WorkspaceDestructivePermitContract`, `WorkspaceRestorePermitScopeV2`, `WorkspaceRestoreSelectionClosureV2`; PG18 synthetic permit, ownership and concurrency tests. **These are not a production original-owner grant or permission to execute.**

**Consumer:** B has conditionally ACKed C binary V2; B-B-08 uses C hashing methods and fixed interoperability vectors. Historic B-B-07 pipe-delimited fingerprints must not be used as C approval. Source archive authenticity and true owner reauth are still missing; the sole ongoing accountable owner is B.

**Verified QA (pinned):** full [#44275375375](https://github.com/o-kobylianskyi/family-points/actions/runs/44275375375) at `ab77611b866f4757ab579df5778e738e1a2bc685`: Maven **702/702**, Web/Android/Compose/attestation PASS. Subsequent sources including C-C-11 need a new pinned full non-skipped run; old green does not transfer to current HEAD.

**Safety:** Reset Execute **HTTP 423**; Restore `restorable=false`/`replaySupported=false`; **BETA HOLD**. No real-family mutations, DB migration, release or deployment authorized.

**Historic original C-C-00 scope handoff:** preserved in the earlier Git history. No new C packages/tasks.
