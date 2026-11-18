# Backend A B-A-04g — exact-SHA full CI rerun after 744/3 financial fixture repair

Date 2026-11-18. Non-deploying **[full-ci]** trigger. This change is documentation only and explicitly schedules all four jobs for the full tested source.

Previous `25696945dc24cb6696af920e949cfc1cc6ed5eef` run `44287295299` failed Java Maven at 744 tests, 3 failures, zero errors/skips, while Android/Web/Compose passed. Fixes: financial test rows now contain the parent denomination fields the strict validator requires; PG18 physical reward purchase FK nullability remains recorded independently of the strict non-null archive parent; additional negatives reject missing historical purchase and absent point currency.

Verify (actual Java test execution, not a green job with Maven steps skipped):
- `DisposablePortableFinancialSourceClosureTest`, including historical purchase, task nullable amount/currency, null purchase FK and missing denomination failures.
- `DisposablePortableFinancialEdgeSchemaPostgresTest` on migrated Testcontainers PostgreSQL 18.
- Existing member source/role/tombstone closure and physical retention rollback suites, plus Backend D provenance tests, all via `./mvnw -B -ntp clean verify`.
- Exact same SHA Web regression/lint/build, Android Gradle build/unit and Compose safety.
- Capture Surefire total, zero failures and errors, full-attestation artifact SHA, independent QA receipt and Deployment=false. If red, report exact failing fixture and re-open Backend A.

Owner handoff `docs/coordination/requests/B-A-04G_LEDGER_PROVENANCE_CI_REPAIR_AND_D_GATE_20261118.md` asks Backend D to sign polymorphic ledger source_type/source_id and group/reputation/REFUND semantics. No production archive vNext category, schema, restore or physical purge enabled. BETA HOLD, HTTP423.
