# FamilyPoints — centralized DEV service settings

The Git-tracked **config/familypoints.dev.example.properties** is the single documented template
for local Spring Boot DEV service settings: PostgreSQL, JWT, Brevo SMTP,
account registration, first/additional family provisioning, and Google OAuth server flags.

The real **config/familypoints.dev.local.properties** must NEVER be committed (gitignored).

## Laptop quick start

From repository root (PowerShell):
```powershell
Copy-Item .\config\familypoints.dev.example.properties .\config\familypoints.dev.local.properties
```
Fill in existing DEV `spring.datasource.password`, `security.jwt.secret` and Brevo `spring.mail.password`
only in the local file; remove leading `#` on those three lines. **Do not paste secrets into chat.**

Run `FamilyPointsApplication` in IntelliJ with **Working directory** set to
`C:\Users\Oleh\IdeaProjects\family-points` (project root).
The existing `src/main/resources/application.properties` loads the local override via
`spring.config.import=optional:file:./config/familypoints.dev.local.properties`.
If you start Spring with a different working directory, it will NOT discover that file.
Environment variables supplied by IntelliJ/system have higher precedence than these file values:
remove stale `FAMILYPOINTS_FIRST_FAMILY_ENABLED=false` etc. when testing.

The committed template is **never loaded automatically**. Copying it enables DEV-only
registration/first-family/additional-family flags. To change scope, edit the LOCAL copy only.

## Frontend Google OAuth setting

The Vite browser client reads `VITE_GOOGLE_CLIENT_ID` from `frontend/.env.local`,
not from Spring properties. This public client ID must match
`familypoints.google.client-id` on the backend. Both remain blank/OFF until
Google Cloud OAuth settings are configured.

## Production/BETA

Do not copy any DEV configuration to Contabo. Retain BETA environment deployment
configuration separately (see `deploy/beta/.env.example`).
No secret or deployment is required to maintain the template.
