# Android APK distribution (outside Google Play)

Public landing page: https://family-point.com/download
Public files: /downloads/familypoints-beta.apk, /downloads/familypoints-beta.json, /downloads/familypoints-beta.apk.sha256

## Build on trusted Windows developer machine

1. Generate a permanent Android release keystore **once**, securely, outside this repository. Back it up securely. Do NOT use the debug keystore. Losing the release key prevents normal in-place updates.
2. Set FP_ANDROID_KEYSTORE_FILE, FP_ANDROID_KEYSTORE_PASSWORD, FP_ANDROID_KEY_ALIAS, FP_ANDROID_KEY_PASSWORD in a private local shell. Set FP_ANDROID_GOOGLE_WEB_CLIENT_ID if required for Google sign-in; this is the OAuth web client ID, not an Android secret.
3. Install Android SDK API 37 and build-tools, set ANDROID_HOME, and execute at repository root:

```powershell
.\scripts\android\build-release-apk.ps1 -VersionCode 1 -VersionName '0.1.0-beta.1'
```

The builder validates signing with apksigner, prints the checksum and writes 3 files under `android-release-output/`. Do not share keystore or passwords.

**Version policy:** increment versionCode for every new APK; preserve applicationId and signing certificate. APK updates require identical signing identity and greater versionCode. Test updating a device installation with actual user data.

## Publish without backend/web deployment

Transfer only the three generated public files to a temporary location on Contabo using a secure channel. Verify remote content. From `/opt/familypoints-beta`, run:

```bash
bash scripts/android/publish-apk.sh /path/to/familypoints-beta.apk /path/to/familypoints-beta.json
```

Type `PUBLISH`. This requires the script to already be present on the host; when a prior BETA tag is deployed, stage the reviewed script separately or wait for the next controlled tagged release. The script operates only in the `downloads/` bind mount. It **does not** run Compose, switch Git revisions, modify PostgreSQL or restart services. Do not copy secrets to the public downloads directory.

Verify public endpoints:

```bash
curl -fI https://family-point.com/downloads/familypoints-beta.apk
curl -fsS https://family-point.com/downloads/familypoints-beta.json
curl -fsS https://family-point.com/downloads/familypoints-beta.apk.sha256
```

Verify downloaded bytes against SHA-256 and inspect APK certificate fingerprint using apksigner. Test Android login, device install, permissions, upgrade without removing old app, and Google OAuth (which may require release certificate SHA-1/SHA-256 and correctly registered client configuration).

## Deployment caveats

- The download page changes must first be included in a future annotated BETA release tag and deployed through the established release gate. Publishing an APK alone never replaces the frontend JavaScript.
- Android release signing cannot be completed through the GitHub connector without access to a trusted keystore. Never upload the private signing key to this repository.
- The publisher stages files on one filesystem and replaces them individually, with metadata last. It is not a fully transactional multi-file exchange; concurrent downloads during publishing could briefly see mixed files. For high-volume releases, migrate to immutable version-specific filenames with a pointer manifest.
- This is a development implementation. CI and on-device acceptance checks must be run before declaring a first public Android APK available.

## Google sign-in readiness (unreleased)

- Signed BETA APK build now requires `FP_ANDROID_GOOGLE_WEB_CLIENT_ID`, the same **Web OAuth** client ID that the production web application uses as `VITE_GOOGLE_CLIENT_ID`. A missing/malformed ID now causes a build error rather than silently hiding Google sign-in.
- Existing native login uses Credential Manager / Google ID token and `/auth/google`; registration uses `/auth/google/register`. A successful global login with no or multiple families opens workspace selection.
- Verify release signing SHA-1/Google Cloud Android app configuration, credential picker on Huawei P20 Pro, backend login/registration and password login before publishing. Do not publish until manual real-device acceptance.
