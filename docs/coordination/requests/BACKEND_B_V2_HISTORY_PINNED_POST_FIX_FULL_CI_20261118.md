# Backend B — v2 task+reward+point history source — pinned FINAL post-fix CI request

2026-11-18. This commit requests full non-skipped CI **after** the final source fix `796100ecacb7395649af46f230f17befcd095ec2`, root/producer source changes, inspector verified C SourceArchive binding and all new tests.

Run Maven PostgreSQL18 on the **exact SHA of this trigger**, inspect `PortableV2HistoryGraphTest`, `PortableMemberAncestryV2Test`, `PortableManifestRootV2Test` and `WorkspacePortableAncestryV2SourceLossPostgresTest`. Then all four Java/Web/Android/Compose + exact-SHA attestation executed (not skipped). Collect test counts (pass/fail/error/skipped), compiler output, source-loss test results, and first stack. Fail closed and fix only reproducible errors.

Original source ancestry 749/749 baseline from QA-068 @89b0f8e1 is historical and **not** current new history PASS. Current source graph contains six categories beyond FAMILY_ROLES/FAMILY_MEMBERS, not all 23+ or 17 unresolved member FKs. Full archive and production Restore **not accepted**, restorable/replaySupported false, HTTP423 and BETA HOLD.

Formal integrated scope packet `BACKEND_B_V2_HISTORY_ONE_PACKAGE_FINAL_CI_20261118.md`. Stop after QA acceptance only for the supported bounded history graph; other categories remain blocked until reviewed, not silently included.
