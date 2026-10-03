# Git + Flyway transition

## 1. Existing local database
The first application start after this update will create `flyway_schema_history` and baseline the already-existing schema as version 1. Hibernate now uses `ddl-auto=validate`; it will no longer modify the schema.

Set `DB_PASSWORD` and `JWT_SECRET` in the IntelliJ Run Configuration environment. `DB_URL` and `DB_USERNAME` have local defaults.

## 2. Git checkpoint
From the project root:

```powershell
git status
git add .
git status
git commit -m "Establish Workspace baseline and Flyway migrations"
git tag -a v0.3-workspace -m "Stable Workspace baseline"
git push origin main
git push origin v0.3-workspace
```

Before `git add .`, confirm `.idea`, `target`, `node_modules`, `.env`, and secrets are not listed.

## 3. Existing tracked secrets
`.gitignore` does not untrack files already committed. Check:

```powershell
git ls-files .idea src/main/resources/application.properties
git log -S "spring.datasource.password" --all -- src/main/resources/application.properties
```

If `.idea` is already tracked:

```powershell
git rm -r --cached .idea
```

`application.properties` should remain tracked, but only with environment-variable placeholders. If a real DB/JWT secret was previously committed, rotate that secret.

## 4. Clean database bootstrap
The current transition baselines the existing database. Before using Flyway to create a brand-new empty FamilyPoints database, export the current schema-only SQL and turn it into `V1__baseline_schema.sql`. Do not apply that V1 to the existing baselined database; Flyway will regard version 1 as already applied.

Example with PostgreSQL tools:

```powershell
pg_dump --schema-only --no-owner --no-privileges -U family_points -d family_points > V1__baseline_schema.sql
```

Review the dump before placing it in `src/main/resources/db/migration/`.

## 5. Future database changes
Never return to `ddl-auto=update`. Every schema/data compatibility change gets a new migration, e.g. `V2__task_permissions.sql`. Commit the Java/React code and matching migration together.
