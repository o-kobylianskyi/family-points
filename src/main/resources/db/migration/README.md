# FamilyPoints Flyway migrations

## Clean installation (Beta, no DEV records)

The existing migration chain starts with V2 and ends at V20.
V1 was historically created by Hibernate before Flyway was introduced.
The initial structure must be generated from the *historical Git tag*
`v0.3-workspace`, which predates V2, **not** dumped from the current
V20 production/development schema.

Run on a Windows machine with Java 21, Maven wrapper, Git and Docker Desktop:

```powershell
git pull
.\scripts\db\generate-v1-baseline.ps1
```

The script creates disposable PostgreSQL 17 on a random localhost port,
checks out the pre-V2 tagged source in a temporary Git worktree, uses
Hibernate only against the temporary database to generate legacy V1,
exports `pg_dump --schema-only` (no users, balances or task records), and
writes `V1__baseline_schema.sql`.

Then it creates a **second empty database** and starts the current code with
Flyway V1–V20 and `ddl-auto=validate`. It only prints PASS after all
20 migrations finish and current JPA mapping validation succeeds. On
failure, inspect the temporary logs; **do not commit or deploy an
unvalidated V1**. The current DEV DB is never contacted.

Review and commit the generated V1 only after the PASS result:

```powershell
git status --short
git add src/main/resources/db/migration/V1__baseline_schema.sql
git commit -m "db: add validated clean baseline V1"
git push
```

## Existing development database

DEV was historically adopted as Flyway baseline version 1. It already
has a `flyway_schema_history` row for V1 rather than an applied SQL
migration, so newly committing V1 will **not replay V1 against the
existing DEV database**. Do not edit previously applied versions V2–V20.

For a public Beta with brand-new PostgreSQL, set
`SPRING_FLYWAY_BASELINE_ON_MIGRATE=false`, which is already present in
`deploy/beta/compose.yaml`. All schema creation should be versioned
and independently verified.

No DEV account/point/reward/task data should be copied to Beta.

## Retry after an initial V1 was generated

If `V1__baseline_schema.sql` already exists but the first Flyway validation
failed on PostgreSQL 17 with `schema "public" already exists`, **do not
regenerate V1 or delete the file**. Pull the latest code and run:

```powershell
.\scripts\db\generate-v1-baseline.ps1 -ValidateExisting
```

This mode skips the temporary historical Git worktree, preserves the generated
V1, changes only an unconditional `CREATE SCHEMA public;` to
`CREATE SCHEMA IF NOT EXISTS public;` (if present), and validates V1..V20
on a new temporary database. The temporary V1 backup and logs are retained
if validation fails. It never connects to the original DEV database.

A Windows `git worktree remove --force` cleanup error may be caused by a
temporary file lock; the validation-only mode avoids that historical worktree.
Inspect `git worktree list` before deleting any local temporary folder.
