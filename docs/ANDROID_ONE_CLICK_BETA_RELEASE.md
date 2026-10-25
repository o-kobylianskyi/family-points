# FamilyPoints Android BETA — one-click Windows release

## First run (once)

From repository root double-click `scripts/android/release.bat`, or run:

```powershell
.\scripts\android\release.bat
```

The first invocation creates **private, git-ignored** `scripts/android/release-config.local.json` from `release-config.example.json` and prompts for the **Google OAuth Web client ID** (the one expected by Backend). It never stores signing passwords or private keys in configuration.

Verify/configure in the JSON as needed: `androidSdk`, `keystoreFile`, `keyAlias`, permanent **release certificate SHA-256**, `deviceSerial`, server IP, SSH username and private-key path, domain/manifest. This repo's example holds this test device's known paths and certificate; do not reuse the configuration blindly on another machine.

The batch script runs PowerShell with process-only `-ExecutionPolicy Bypass`, avoiding a permanent machine-wide PowerShell policy change. Keystore/key passwords are requested with hidden input each build; SSH passphrase remains handled by ssh/scp (consider local ssh-agent for fewer prompts). No passwords written to disk.

## Menu

- **1 Status**: reads publicly served manifest, local APK metadata, paths, no secrets.
- **2 Build**: uses public `versionCode+1`, loads config, asks keystore/password, invokes `build-release-apk.ps1`, validates the resulting APK's signer **against a pinned expected certificate**, and emits APK, JSON and SHA256 under git-ignored `android-release-output/`.
- **3 Install**: `adb -s <deviceSerial> install -r`; verifies signer, never uninstalls or wipes local offline queues. Device acceptance (launch, password/Google login, points, workspace switching) must be done manually.
- **4 Google logs**: displays non-secret `FamilyPointsGoogleAuth` diagnostic stages from configured device only. Clear log before reproducing issue: `adb -s SERIAL logcat -c`.
- **5 Publish**: verifies cert, ensures local `versionCode` is strictly higher than live, asks `PUBLISH`, invokes existing `publish-to-contabo.ps1` (it additionally asks `UPLOAD` and SSH passphrase), and validates live JSON hash. Server publisher also requests `PUBLISH`. Backend/Web/Docker/DB untouched.
- **6 All**: build + install + publish, with explicit publication confirmation. Only select after real-phone launch/auth checks as appropriate.

Noninteractive options: `release.bat status|build|install|logs|publish|all`. Override build code with `release.bat build -VersionCode 12` when required; it must be above the public manifest.

## Release rules

The published `0.1.0-beta.10` on 2026-10-25 was signed using certificate SHA-256 `167d63a05676a8fc9a1671be11a6d7c1246ff9d97286efca30cb5e104c152d4f`. Earlier beta.9 used a **different certificate**. Normal in-place upgrade is supported **only from beta.10 onward with this preserved keystore**; never generate a new release key. Keep an off-machine encrypted backup of the keystore itself; the user's no-backup instruction for the disposable beta *database* does not mean the signing key is disposable.

The scripts intentionally do not auto-tag, push, deploy Backend/Web, bypass CI, reset Android storage, delete DB or force-publish an APK. Git commits after backend/web tag do not automatically update the deployed containers. For a diagnostics-only local beta.11, use menu **Build**, **Install**, **Logs** but **do not Publish** until diagnosed and tested.

## Troubleshooting

- `PowerShell script execution disabled`: use `release.bat` (process-only bypass).
- `Missing config`: first run creates `release-config.local.json`, asks Web Client ID; edit afterward if needed.
- `APK signed with UNEXPECTED key`: check configured `keystoreFile`, do not publish.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: existing installation uses different signer. Do not auto-uninstall and lose offline data.
- `New versionCode must exceed published`: read live manifest and rebuild with a higher code.
- `Publisher failed after upload`: inspect live JSON/APK before retrying; server may already have published successfully.
- Compilers/CI can still fail; this wrapper does not conceal failures.
