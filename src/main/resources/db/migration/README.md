# Flyway migrations

The current development database is adopted as Flyway baseline version 1.
Do not edit an applied migration. Add future changes as `V2__...sql`, `V3__...sql`, etc.

`baseline-on-migrate=true` is transitional for adopting the existing non-empty development database. Once all environments have `flyway_schema_history`, remove it or set it to false.

For a clean database bootstrap, create a schema-only V1 snapshot from the current database before relying on fresh installations. See `GIT_FLYWAY_SETUP.md`.
