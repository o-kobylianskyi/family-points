# FamilyPoints release channels

## Tag naming
- Development: ordinary commits on feature branches (never auto-deploy).
- Release candidate: `vX.Y.Z-rc.N` (test only).
- BETA: **annotated, immutable** Git tag `vX.Y.Z-beta.N` (eligible for controlled Contabo deployment **only after** release workflow passes and the operator approves).
- Stable: `vX.Y.Z` (separate later process).

Do **not** select releases using a substring search for "beta" in commit messages, branch names or filenames. Match the full pattern `^v[0-9]+\\.[0-9]+\\.[0-9]+-beta\\.[0-9]+$` and check the ref is an annotated tag. Once published, never move/delete/reuse a release tag. Protect `v*` tags in GitHub repository rules.

## Candidate → BETA release
1. Finish changes on DEV, run tests, check cross-family authorization, data/security risks and Flyway changes.
2. Select a full, reviewed 40-character Git SHA. Tag exactly that commit:
   ```bash
   git tag -a v0.1.0-beta.1 <sha> -m "FamilyPoints BETA 0.1.0-beta.1"
   git push origin v0.1.0-beta.1
   ```
3. Wait for **FamilyPoints BETA release gate** to pass on that tag. Failed/pending workflows are not approved. Create a GitHub Release with changelog + DB migration range, SHA, API compatibility and Android version.
4. On Contabo verify currently deployed SHA, database backup/restore, and server secrets/flags. Make sure Flyway migration rollback is understood.
5. Run the **manual** deployment command `bash scripts/beta/deploy-release.sh v0.1.0-beta.1` from the VPS checkout. Docker and backup use sudo inside the script. Deployment does not touch DEV or other Nginx/WireGuard/1C routing.
6. Test login, workspace setup, roles, task awards, rewards and connectivity. Record deployed SHA/tag, health checks and restore info in `FAMILYPOINTS_CONTABO_DEPLOYMENT_LOG.md`.

## Android installable updates
- API: `https://family-point.com/`; versionName follows the tagged BETA version.
- Use the SAME private Android signing keystore and app ID for all updates; keep key and passwords outside GitHub. Securely back up the keystore.
- Increase `familyPointsVersionCode` for every distributed APK, even when versionName is similar.
- Build on the developer workstation with `scripts/android/build-beta-release.ps1 -VersionCode <n> -VersionName '<tag without v>'`. Publish/checksum the APK only after the relevant BETA backend is deployed and phone smoke tests pass.
- No automatic APK background delivery or unattended server deployment is enabled.

## Compatibility / limitations
- A tagged commit is a release *candidate* until the release CI passes and the human approves.
- Existing SSH command: `ssh -i "$env:USERPROFILE\\.ssh\\contabo_admin_ed25519" adminfp@184.174.37.206`.
- Git checkout/image rollback alone may not roll back a forward-only Flyway migration.
- Do not use `docker compose down -v` or overwrite/reuse persistent BETA data.

## Environment lifecycle (agreed 2026-10-21)
- NOW: family-point.com runs public BETA on existing Contabo stack; root domain is not yet production. BETA Git tag is a release channel independent from DNS name.
- LATER: family-point.com becomes PRODUCTION only upon separate stable release approval. Public BETA moves to beta.family-point.com, with its own database/volume, signing/application configs, JWT secrets, TLS and rollout; local DEV stays on existing dev.family-point.com:8443.
- Do not automatically transfer user or child data from BETA to PRODUCTION. Plan consent/retention and migrations explicitly.
- Android currently targets the root BETA; after split release-channel API URLs must be explicit. If users need beta and stable APKs installed side by side, use separate application IDs and version/update streams.
- No automatic deployment on tags; staging/production requires separate approval and verification.
