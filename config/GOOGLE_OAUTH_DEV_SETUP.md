# FamilyPoints — Google OAuth for local DEV (Web + Android)

The Google sign-in and registration code is present on the feature branch. It does **not** automatically configure Google Cloud. This guide does **not** require sharing passwords or OAuth secrets.

## 1. Configure the Google Cloud project

1. In Google Cloud Console, configure the **Google Auth Platform / OAuth consent screen**, app branding and authorized test users (if the app is in Testing mode).
2. Create a **Web application OAuth client**; copy its **Client ID** (public). Under *Authorized JavaScript origins* enter the exact URL for the browser frontend. For local Chrome on the development PC use:
   - `http://localhost:5173` (or `http://127.0.0.1:5173` if used)
   - For access from another PC/phone use a **real HTTPS origin** and register that origin.
3. Do **not** use `http://192.168.1.110:5173` with Google Identity Services. An HTTP private LAN IP is not an allowed secure website origin. HTTPS must have a trusted certificate and an exact Google Cloud authorized origin.
4. Create a separate **Android OAuth client** using package:
   `com.olehkobylianskyi.familypoints.android`
   and the app's SHA-1 signing-certificate fingerprint (use `cd android; .\gradlew.bat signingReport` on Windows). Register the debug and later release signatures as needed.
5. For Android Credential Manager, use the **same Web Client ID from step 2 as the server client ID**. The Android client identifies the installed app; it is **not** the ID sent as the token audience to the backend.

## 2. DEV Backend (local only, never commit credentials)

In the gitignored `config/familypoints.dev.local.properties`, set:

```properties
familypoints.google.client-id=YOUR_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
familypoints.google.enabled=true
familypoints.google.registration-enabled=true
familypoints.google.linking-enabled=true
```

Spring also accepts the `GOOGLE_CLIENT_ID`, `FAMILYPOINTS_GOOGLE_ENABLED`,
`FAMILYPOINTS_GOOGLE_REGISTRATION_ENABLED`, and `FAMILYPOINTS_GOOGLE_LINKING_ENABLED`
environment variables. Environment variables override the local properties. The client ID is public;
**never** put an OAuth client secret in Vite or Android Gradle.

These flags apply **only to local DEV**. Keep BETA/PROD unchanged until approved.

## 3. Vite Web

Create or update the gitignored `frontend/.env.local`:

```dotenv
VITE_GOOGLE_CLIENT_ID=YOUR_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
```

Restart Vite. Open `http://localhost:5173` on the laptop or a configured HTTPS origin
(not an HTTP LAN IP). Google Auth supports:
- Login via `POST /auth/google` only for a **previously registered or explicitly linked** Google identity.
- Registration via `POST /auth/google/register`; it creates a **new global account**, never merges by email.
- Account linking via Settings after current-password confirmation.
- Workspace selection when no family or multiple families belong to the account.

## 4. Android

In a **local non-committed** `android/gradle.properties` or via Gradle `-P` set:

```properties
familyPointsGoogleWebClientId=YOUR_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
```

Then sync Gradle and run:
```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

The native Google button appears **only when the Web Client ID is configured** and the backend is reachable online. Native Credential Manager obtains an ID token and passes it to the same backend as Web. No offline password enrollment is granted merely by Google login.

## 5. Acceptance test

1. Existing Google-linked account -> Google login -> choose correct family -> logout/login again.
2. New Google identity -> Google registration -> first-family onboarding.
3. Attempt to login an unknown Google identity without registering -> rejected.
4. Shared email on two global accounts -> **no auto-link or identity merge**.
5. Child account without `MANAGE_MEMBERS` -> cannot access private participant emails.
6. Incorrect audience, expired token, and untrusted origin -> rejected.
7. Android debug device with Google Play Services / a Google account -> native credential chooser appears.
8. Test all UI in UK, DE, EN and RU; no Ukrainian hardcoded fallback when Deutsch is selected.

Do not share ID tokens in screenshots/logs, and do not deploy/configure Contabo BETA during these DEV checks.
