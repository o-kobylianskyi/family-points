# Media V38 — Approved PNG catalog integration

**Status: awaiting binary import; not installed.**

Approved artifact: `familypoints-media-v38-integration-pack-verified.zip` (30 individual, 512×512 transparent PNG avatars).
SHA-256: `48348f8a855808f3ee6343c6e83c0d1b7688d862c2bdefcde76a969cd56e18bc`.

The approved ZIP contains `pack/catalog.json`, `pack/assets/*.png`, and `install_approved_avatars.py`. Import binaries and API edits **atomically**; do not advertise catalog items before previews exist in the Git tree. Current GitHub connector accepts base64 Git blobs but cannot ingest this session's local PNG file bytes; a partial catalog commit would produce 404s.

## Integration checks

1. Work from latest `main`, clean worktree. Verify archive checksum and the 30 catalog checksums.
2. Review and fix shipped script before running: `patch_controller` uses heuristic `key.indexOf('_')` for MIME but preflight expects `media.isPngCatalogKey(key)`; instead MIME must use a registry lookup. Run only after preflight assertions match the code edits.
3. Copy 30 PNG assets into `src/main/resources/media/catalog`, register keys in `MediaService`, serve their previews with `image/png` in `MediaController`, and update `MediaCatalogServiceTest`.
4. Hide rejected legacy SVG **AVATAR choices** from selectable catalog; preserve old binding keys until migrated. Keep unrelated family/cover/group/task/reward catalog images intact.
5. Run Maven/PostgreSQL integration suite, full exact-SHA CI, and Web/Android catalog/preview consumer regressions before release.
6. Only Release/DevOps may deploy to BETA after database backup/rehearsal, signed APK tests and QA approval.

This documentation commit does **not** claim PNG upload, code integration, tests or deployment.