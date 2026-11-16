#!/usr/bin/env python3
"""Fail-closed CI outcomes: classification is evidence, not release authorization."""
import json
import os
import sys
from pathlib import Path

COMPONENTS = ("backend", "android", "web", "compose")
RESULTS = {"success", "failure", "cancelled", "skipped", "timed_out", "action_required", "neutral"}

def evaluate(env):
    selected = {k: env.get(k.upper() + "_SELECTED", "") for k in COMPONENTS}
    results = {k: env.get(k.upper() + "_RESULT", "") for k in COMPONENTS}
    scope = env.get("SCOPE_RESULT", "")
    attestation = env.get("FULL_ATTESTATION_RESULT", "")
    valid_scope = scope == "success" and all(v in ("true", "false") for v in selected.values())
    valid_results = all(v in RESULTS for v in results.values())
    scoped_ok = valid_scope and valid_results and all(
        results[k] == "success" if selected[k] == "true" else results[k] in ("success", "skipped")
        for k in COMPONENTS
    )
    all_selected = valid_scope and all(selected[k] == "true" for k in COMPONENTS)
    full_pass = scoped_ok and all_selected and attestation == "success"
    return {
        "sourceSha": env.get("SOURCE_SHA", ""), "event": env.get("EVENT_NAME", ""),
        "runUrl": env.get("RUN_URL", ""), "scopeResult": scope,
        "selected": selected, "results": results, "fullAttestationResult": attestation,
        "scopedCiPassed": scoped_ok, "fullCiVerified": full_pass,
        "qaAccepted": False, "releaseApproved": False, "deploymentExecuted": False,
    }

def main():
    report = evaluate(os.environ)
    Path("ci-push-result.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    summary_path = os.getenv("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as out:
            out.write("## CI per-commit outcome\n\n")
            for name, val in (("SHA", report["sourceSha"]), ("Event", report["event"]),
                              ("Run", report["runUrl"]), ("Scope evaluation", report["scopeResult"])):
                out.write(f"- {name}: {val}\n")
            out.write("\n| Component | Selected | Job result |\n|---|---|---|\n")
            for k in COMPONENTS:
                out.write(f"| {k} | {report['selected'][k]} | {report['results'][k]} |\n")
            out.write(f"\n- Scoped CI PASS: {report['scopedCiPassed']}\n")
            out.write(f"- Full CI verified: {report['fullCiVerified']}\n")
            if all(report["selected"][k] == "false" for k in COMPONENTS):
                out.write("- No component tests selected (e.g. docs-only push); NOT FULL CI.\n")
    print(f"CI REPORT SHA {report['sourceSha']}: scoped={report['scopedCiPassed']}, full={report['fullCiVerified']}")
    if not report["scopedCiPassed"]:
        sys.exit("Push CI scope or one or more selected jobs failed")

if __name__ == "__main__":
    main()
