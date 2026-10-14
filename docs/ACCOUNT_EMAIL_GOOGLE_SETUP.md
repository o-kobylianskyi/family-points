# FamilyPoints: email verification + Google signup (2026-10-14)

## Status
Source changes are committed, **not locally compiled or end-to-end tested**.
Email and Google registration are opt-in, OFF by default. Do not deploy this package to BETA yet.

## Password account registration
- `POST /auth/register-account` now requires `username`, `email`, and a password of 12–128 characters.
- Newly created user_accounts are `enabled=false` until email confirmation.
- A SHA-256 hash of a random 256-bit token is stored in account_email_tokens; confirmation expires after 24 hours.
- `POST /auth/email/verify` accepts `{"token":"..."}`; only then is the account enabled.
- `POST /auth/email/request-verification` accepts email; the issued token has a two-minute per-account cooldown.
- `POST /auth/email/request-reset` and `POST /auth/email/reset-password` implement password recovery via a 30-minute token.
- Existing accounts have nullable email and preserve their current enabled state. Do not backfill verified addresses without proof.

## Mail configuration for local DEV
Provide only local secrets via environment, never commit values:
```
FAMILYPOINTS_GLOBAL_ACCOUNT_REGISTRATION_ENABLED=true
FAMILYPOINTS_MAIL_FROM=no-reply@family-point.com
FAMILYPOINTS_WEB_URL=http://localhost:5173
SMTP_HOST=<your SMTP host>
SMTP_PORT=587
SMTP_USERNAME=<SMTP username>
SMTP_PASSWORD=<SMTP password>
SMTP_AUTH=true
SMTP_STARTTLS=true
```
Configure SPF/DKIM/DMARC with the chosen mail provider and confirm sender ownership.
SMTP sending happens synchronously inside the signup transaction: test failure/rollback behavior before release.
No public SMTP service/provider is selected yet.

## Google (Web)
Create a Google Cloud OAuth 2.0 **Web application** client. For local DEV, register exact
`http://localhost:5173` authorized JavaScript origin; configure deployed DEV origin separately.
Use **the same** public client ID for backend and Vite:
```
GOOGLE_CLIENT_ID=<Google Web OAuth client ID>
VITE_GOOGLE_CLIENT_ID=<same client ID>  # in frontend/.env.local or shell before Vite starts
FAMILYPOINTS_GOOGLE_ENABLED=true
FAMILYPOINTS_GOOGLE_REGISTRATION_ENABLED=true
```
The Web button uses Google Identity Services. Backend checks signature via Google's JWKs,
expiry, issuer, audience, `azp` if necessary, Google subject, verified email.
New Google signup uses `POST /auth/google/register`, no email-based account merging.
Existing linked Google accounts can sign in at `POST /auth/google`.
Existing password users must authenticate and explicitly link Google using
`POST /api/account/google/link` with password re-verification; the link feature is also OFF by default.

## Release gates / known outstanding
- Run Maven/Gradle/Vite builds and Testcontainers after V30 migration.
- Run disposable DB integration tests for token consumption, duplicate-email registration and Google linking races.
- Verify account-enumeration resistance, SMTP downtime behavior, resend throttling and anti-automation controls (IP/email).
- Complete Android native Google Credential Manager sign-in with platform OAuth client mapping; Android currently only supports email/password signup.
- Set up a verified sender before enabling email registration. Keep Contabo unchanged.
