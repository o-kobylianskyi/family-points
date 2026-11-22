"""REL-BETA-STORAGE-01: static/synthetic Docker Compose BETA archive isolation gates.

No containers start; docker compose config reads source only with dummy secrets.
Positive and intentionally tampered negative cases prevent storage exposure or
accidental executable-restore/retention flags from reaching BETA configuration.
"""
import copy
import json
import os
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]
ARCHIVE_DIR = "/var/lib/familypoints/backups"
ARCHIVE_VOLUME = "beta_backup_artifacts"
PROPERTY = "familypoints.backup.local-directory=${FAMILYPOINTS_BACKUP_LOCAL_DIRECTORY:}"


def effective_compose():
    env = {**os.environ, "DB_PASSWORD": "ci-dummy-db-password-not-real",
           "JWT_SECRET": "ci-dummy-jwt-not-real"}
    result = subprocess.run(
        ["docker", "compose", "-f", "compose.beta.yml", "config", "--format", "json"],
        cwd=ROOT, env=env, capture_output=True, text=True, check=True,
    )
    return json.loads(result.stdout)


def validate(compose):
    services = compose["services"]
    backend = services["backend"]
    web = services["web"]
    database = services["database"]

    env = backend.get("environment", {})
    if env.get("FAMILYPOINTS_BACKUP_LOCAL_DIRECTORY") != ARCHIVE_DIR:
        raise AssertionError("Backend archive path must match the mounted path")
    mounts = [v for v in backend.get("volumes", []) if v.get("target") == ARCHIVE_DIR]
    if len(mounts) != 1 or mounts[0].get("type") != "volume":
        raise AssertionError("Exactly one writable persistent named archive volume required")
    mount = mounts[0]
    if mount.get("read_only") is True:
        raise AssertionError("Backend archive volume cannot be read-only")
    source = mount.get("source", "")
    # Compose's normalized JSON may use the logical source key or the
    # project-prefixed Docker resource name, depending on CLI version.
    declared = compose.get("volumes", {})
    if source == ARCHIVE_VOLUME:
        exists = ARCHIVE_VOLUME in declared
    else:
        exists = source.endswith("_" + ARCHIVE_VOLUME) and any(
            item.get("name") == source for item in declared.values()
            if isinstance(item, dict)
        )
    if not (source == ARCHIVE_VOLUME or source.endswith("_" + ARCHIVE_VOLUME)):
        raise AssertionError("Archive mount must use dedicated named volume")
    if not exists:
        raise AssertionError("Named archive volume is not declared")

    init = services.get("backup-volume-init")
    if not init:
        raise AssertionError("Dedicated archive volume initializer missing")
    init_volumes = init.get("volumes", [])
    if len(init_volumes) != 1 or init_volumes[0].get("source") != source or init_volumes[0].get("target") != ARCHIVE_DIR:
        raise AssertionError("Initializer must mount only backend's exact archive volume")
    if init.get("ports") or init.get("networks") or init.get("network_mode") != "none":
        raise AssertionError("Initializer must have no network or ports")
    if str(init.get("user")) not in ("0:0", "0"):
        raise AssertionError("Initializer must be restricted explicit root, never backend root")
    if init.get("read_only") is not True or init.get("cap_drop") != ["ALL"]:
        raise AssertionError("Initializer must be read-only and drop all capabilities by default")
    if set(init.get("cap_add", [])) != {"CHOWN", "FOWNER"}:
        raise AssertionError("Initializer may have only CHOWN/FOWNER capability")
    if "no-new-privileges:true" not in init.get("security_opt", []):
        raise AssertionError("Initializer must prohibit privilege gains")
    if init.get("restart") not in ("no", None):
        raise AssertionError("Initializer must run as a completed one-shot")
    if not init.get("configs"):
        raise AssertionError("Initializer must load its immutable script via config")
    deps = backend.get("depends_on", {})
    if deps.get("backup-volume-init", {}).get("condition") != "service_completed_successfully":
        raise AssertionError("Backend must wait for successful archive init before startup")

    for name, service in (("web", web), ("database", database)):
        for volume in service.get("volumes", []):
            if volume.get("source") == source or volume.get("target") == ARCHIVE_DIR:
                raise AssertionError(f"Archive must never be mounted in {name}")
        if service.get("environment", {}).get("FAMILYPOINTS_BACKUP_LOCAL_DIRECTORY"):
            raise AssertionError(f"Archive path must not be passed to {name}")
    if backend.get("ports") or database.get("ports"):
        raise AssertionError("Backend and database must not publish ports")
    ports = web.get("ports", [])
    if len(ports) != 1 or ports[0].get("host_ip") != "127.0.0.1":
        raise AssertionError("Web ingress must remain loopback-only")

    forbidden = {
        "FAMILYPOINTS_RESTORE_EXECUTE_ENABLED",
        "FAMILYPOINTS_RESET_EXECUTE_ENABLED",
        "FAMILYPOINTS_RETENTION_PHYSICAL_PURGE_ENABLED",
    }
    for service in services.values():
        for name, value in service.get("environment", {}).items():
            if name in forbidden and str(value).lower() in {"true", "1", "yes", "on"}:
                raise AssertionError("No executable Restore/Reset or physical purge opt-in")
    for expected in ("FAMILYPOINTS_GOOGLE_ENABLED",
                     "FAMILYPOINTS_GOOGLE_REGISTRATION_ENABLED",
                     "FAMILYPOINTS_GOOGLE_LINKING_ENABLED",
                     "FAMILYPOINTS_GLOBAL_ACCOUNT_REGISTRATION_ENABLED"):
        if str(env.get(expected)).lower() != "false":
            raise AssertionError("Optional BETA enrollment and auth remain disabled by default")


class BetaArchiveVolumeConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.effective = effective_compose()

    def test_real_compose_is_backend_only_and_persistent(self):
        validate(self.effective)

    def test_property_name_is_explicit_and_fail_closed_elsewhere(self):
        props = (ROOT / "src/main/resources/application.properties").read_text()
        self.assertIn(PROPERTY, props)
        self.assertNotIn("familypoints.backup.local-directory=/var/", props)

    def test_deployment_invokes_init_before_no_deps_backend(self):
        script = (ROOT / "scripts/beta/deploy-release.sh").read_text()
        start = script.index('deployment_stage="archive-volume-init"')
        init = script.index('run --no-deps --rm backup-volume-init', start)
        backend = script.index('up -d --build --no-deps backend web', start)
        self.assertLess(start, init)
        self.assertLess(init, backend)
        self.assertNotIn("down -v", script)

    def test_initializer_tampered_volume_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backup-volume-init"]["volumes"][0]["source"] = "beta_postgres"
        with self.assertRaisesRegex(AssertionError, "exact archive volume"):
            validate(cfg)

    def test_initializer_network_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backup-volume-init"]["network_mode"] = "host"
        with self.assertRaisesRegex(AssertionError, "no network"):
            validate(cfg)

    def test_backend_missing_init_dependency_rejected(self):
        cfg = copy.deepcopy(self.effective)
        del cfg["services"]["backend"]["depends_on"]["backup-volume-init"]
        with self.assertRaisesRegex(AssertionError, "wait for successful archive init"):
            validate(cfg)

    def test_initializer_recursive_privilege_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backup-volume-init"]["cap_add"] = ["CHOWN", "FOWNER", "DAC_OVERRIDE"]
        with self.assertRaisesRegex(AssertionError, "only CHOWN/FOWNER"):
            validate(cfg)

    def test_missing_backend_mount_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backend"]["volumes"] = []
        with self.assertRaisesRegex(AssertionError, "Exactly one"):
            validate(cfg)

    def test_wrong_directory_binding_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backend"]["environment"]["FAMILYPOINTS_BACKUP_LOCAL_DIRECTORY"] = "/tmp/backups"
        with self.assertRaisesRegex(AssertionError, "path"):
            validate(cfg)

    def test_web_archive_mount_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["web"].setdefault("volumes", []).append(
            copy.deepcopy(cfg["services"]["backend"]["volumes"][0]))
        with self.assertRaisesRegex(AssertionError, "web"):
            validate(cfg)

    def test_database_archive_mount_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["database"].setdefault("volumes", []).append(
            copy.deepcopy(cfg["services"]["backend"]["volumes"][0]))
        with self.assertRaisesRegex(AssertionError, "database"):
            validate(cfg)

    def test_public_backend_port_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backend"]["ports"] = [{"target": 8080, "published": "8080"}]
        with self.assertRaisesRegex(AssertionError, "publish ports"):
            validate(cfg)

    def test_public_web_bind_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["web"]["ports"][0]["host_ip"] = "0.0.0.0"
        with self.assertRaisesRegex(AssertionError, "loopback"):
            validate(cfg)

    def test_destructive_restore_opt_in_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["services"]["backend"]["environment"]["FAMILYPOINTS_RESTORE_EXECUTE_ENABLED"] = "true"
        with self.assertRaisesRegex(AssertionError, "physical purge|Restore"):
            validate(cfg)

    def test_backup_volume_missing_declaration_rejected(self):
        cfg = copy.deepcopy(self.effective)
        cfg["volumes"].clear()
        with self.assertRaisesRegex(AssertionError, "not declared"):
            validate(cfg)


if __name__ == "__main__":
    unittest.main()
