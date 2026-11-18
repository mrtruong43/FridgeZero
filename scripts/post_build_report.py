#!/usr/bin/env python3
"""
Post-build reporter for FridgeZero:
1. Updates source_summary.md with the latest source code and directory tree.
2. Summarizes code changes (git diff --numstat / status) excluding source_summary.md.
3. Reports the built APK size and delta vs. baseline/previous build.
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SUMMARY_PATH = ROOT / "source_summary.md"
DEBUG_APK_PATH = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "FridgeZero-debug.apk"
BASELINE_APK_PATH = ROOT / "FridgeZero-debug.apk"
METRICS_CACHE_PATH = ROOT / "app" / "build" / ".last_apk_metrics.json"

EXCLUDED_ASSET_NAMES = {"three.min.js"}

LANG_BY_EXT = {
    ".kt": "kotlin",
    ".kts": "kotlin",
    ".java": "java",
    ".xml": "xml",
    ".html": "html",
    ".js": "javascript",
    ".css": "css",
    ".toml": "toml",
    ".properties": "properties",
    ".pro": "proguard",
    ".json": "json",
    ".md": "markdown",
}


def run_git(args: list[str]) -> str:
    res = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return res.stdout.strip()


def collect_source_files() -> list[Path]:
    files: list[Path] = []
    candidates = [
        ROOT / "docs" / "spec.md",
        ROOT / "settings.gradle.kts",
        ROOT / "build.gradle.kts",
        ROOT / "gradle.properties",
        ROOT / "gradle" / "libs.versions.toml",
        ROOT / "app" / "build.gradle.kts",
        ROOT / "app" / "src" / "main" / "AndroidManifest.xml",
    ]
    for c in candidates:
        if c.is_file():
            files.append(c)

    values_dir = ROOT / "app" / "src" / "main" / "res" / "values"
    if values_dir.is_dir():
        files.extend(sorted(p for p in values_dir.rglob("*.xml") if p.is_file()))

    assets_dir = ROOT / "app" / "src" / "main" / "assets"
    if assets_dir.is_dir():
        for p in sorted(assets_dir.rglob("*")):
            if p.is_file() and p.name not in EXCLUDED_ASSET_NAMES and p.suffix in {".html", ".css", ".js", ".json", ".txt"}:
                files.append(p)

    for src_set in ("main", "test", "androidTest"):
        kt_root = ROOT / "app" / "src" / src_set / "kotlin"
        if kt_root.is_dir():
            files.extend(sorted(p for p in kt_root.rglob("*.kt") if p.is_file()))

    # Deduplicate while preserving order
    seen: set[Path] = set()
    unique: list[Path] = []
    for f in files:
        if f not in seen:
            seen.add(f)
            unique.append(f)
    return unique


def build_directory_tree(files: list[Path]) -> str:
    rel_paths = [f.relative_to(ROOT).as_posix() for f in files]
    kt_main = [p.removeprefix("app/src/main/kotlin/com/truong/fridgezero/") for p in rel_paths if p.startswith("app/src/main/kotlin/com/truong/fridgezero/")]
    kt_test = [p.removeprefix("app/src/test/kotlin/com/truong/fridgezero/") for p in rel_paths if p.startswith("app/src/test/kotlin/com/truong/fridgezero/")]
    values_files = sorted(Path(p).name for p in rel_paths if p.startswith("app/src/main/res/values/"))
    asset_files = [p.removeprefix("app/src/main/assets/fridge3d/") for p in rel_paths if p.startswith("app/src/main/assets/fridge3d/")]

    lines = [
        "FridgeZero/",
        "├── docs/spec.md",
        "├── gradle/libs.versions.toml",
        "├── build.gradle.kts",
        "├── settings.gradle.kts",
        "├── gradle.properties",
        "└── app/",
        "    ├── build.gradle.kts",
        "    └── src/",
        "        ├── main/",
        "        │   ├── AndroidManifest.xml",
        "        │   ├── assets/fridge3d/",
    ]
    for name in asset_files:
        lines.append(f"        │   │   ├── {name}")
    vendor_path = ROOT / "app" / "src" / "main" / "assets" / "fridge3d" / "three.min.js"
    if vendor_path.is_file():
        lines.append("        │   │   └── three.min.js                # [vendor UMD r128, omitted]")
    lines.append("        │   ├── kotlin/com/truong/fridgezero/")
    for p in kt_main:
        lines.append(f"        │   │   └── {p}")
    lines.extend([
        "        │   └── res/",
        "        │       ├── font/                       # 7 bundled TTFs (Space Grotesk, Plus Jakarta Sans, JetBrains Mono)",
        f"        │       └── values/{{{', '.join(values_files)}}}",
    ])
    if kt_test:
        lines.append("        └── test/kotlin/com/truong/fridgezero/")
        for p in kt_test:
            lines.append(f"            └── {p}")
    return "\n".join(lines)


def write_source_summary(files: list[Path]) -> tuple[int, int]:
    now_str = datetime.now().astimezone().strftime("%Y-%m-%d %H:%M:%S %z")
    # Format timezone offset as +07:00
    if len(now_str) >= 5 and now_str[-5] in "+-":
        now_str = f"{now_str[:-2]}:{now_str[-2:]}"

    vendor_path = ROOT / "app" / "src" / "main" / "assets" / "fridge3d" / "three.min.js"
    vendor_size = f"{vendor_path.stat().st_size:,}" if vendor_path.is_file() else "N/A"

    parts: list[str] = [
        "# FridgeZero — Complete Source Code & Architecture Summary\n\n",
        f"> **Generated:** {now_str}\n",
        "> **Architecture:** Jetpack Compose + Room + WorkManager + CameraX 1.4.2 + Offline Three.js r128 WebGL Stage (Single Shared WebView Host at MainActivity Root + v3 Neo-Brutalist)\n",
        f"> **Excluded:** `local.properties` (machine-specific paths/sensitive config), binary files (`.apk`, `.jar`, `.ttf`, `.webp`), build outputs (`build/`, `.gradle/`), and third-party minified vendor bundle `app/src/main/assets/fridge3d/three.min.js` ({vendor_size} bytes).\n\n",
        "## 1. Directory Structure\n\n",
        "```text\n",
        build_directory_tree(files),
        "\n```\n\n",
        "## 2. File Contents\n\n",
    ]

    for f in files:
        rel = f.relative_to(ROOT).as_posix()
        text = f.read_text(encoding="utf-8", errors="replace")
        line_count = len(text.splitlines()) + (1 if text.endswith("\n") else 0)
        lang = LANG_BY_EXT.get(f.suffix.lower(), "")
        fence = "````" if lang == "markdown" else "```"
        parts.append(f"### `{rel}` ({line_count} lines)\n\n")
        parts.append(f"{fence}{lang}\n")
        parts.append(text.rstrip("\n"))
        parts.append(f"\n{fence}\n\n")

    output = "".join(parts).rstrip("\n") + "\n"
    SUMMARY_PATH.write_text(output, encoding="utf-8", newline="\r\n")
    return len(files), len(output.splitlines())


def summarize_code_changes() -> dict:
    # Unstaged + staged changes vs HEAD, excluding generated summary and root binary APK
    numstat = run_git(["diff", "HEAD", "--numstat", "--", ".", ":!source_summary.md", ":!FridgeZero-debug.apk"])
    status_out = run_git(["status", "--short", "--", ".", ":!source_summary.md", ":!FridgeZero-debug.apk"])

    changed_files: list[dict] = []
    total_added = 0
    total_deleted = 0

    for line in numstat.splitlines():
        if not line.strip():
            continue
        cols = line.split("\t", 2)
        if len(cols) != 3:
            continue
        ins_s, del_s, path = cols
        ins = int(ins_s) if ins_s.isdigit() else 0
        dels = int(del_s) if del_s.isdigit() else 0
        total_added += ins
        total_deleted += dels
        changed_files.append({"path": path, "added": ins, "deleted": dels})

    untracked = []
    for line in status_out.splitlines():
        if line.startswith("?? "):
            untracked.append(line[3:].strip())

    return {
        "changed_files": changed_files,
        "untracked_files": untracked,
        "total_added": total_added,
        "total_deleted": total_deleted,
    }


def format_delta(new_bytes: int, old_bytes: int) -> dict:
    diff = new_bytes - old_bytes
    pct = (diff / old_bytes * 100.0) if old_bytes > 0 else 0.0
    sign = "+" if diff > 0 else ""
    return {
        "old_bytes": old_bytes,
        "new_bytes": new_bytes,
        "diff_bytes": diff,
        "diff_kib": round(diff / 1024.0, 2),
        "diff_mib": round(diff / (1024.0 * 1024.0), 3),
        "diff_pct": round(pct, 2),
        "formatted": f"{sign}{diff:,} bytes ({sign}{diff / 1024.0:.2f} KiB, {sign}{pct:.2f}%)",
    }


def get_head_apk_size() -> int | None:
    res = subprocess.run(
        ["git", "cat-file", "-s", "HEAD:FridgeZero-debug.apk"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if res.returncode == 0 and res.stdout.strip().isdigit():
        return int(res.stdout.strip())
    return None


def analyze_apk(update_cache: bool = True, sync_root_apk: bool = True) -> dict:
    result: dict = {
        "built_exists": DEBUG_APK_PATH.is_file(),
        "built_path": str(DEBUG_APK_PATH.relative_to(ROOT).as_posix()),
        "root_apk_path": str(BASELINE_APK_PATH.relative_to(ROOT).as_posix()),
        "root_apk_synced": False,
    }
    if not DEBUG_APK_PATH.is_file():
        return result

    new_bytes = DEBUG_APK_PATH.stat().st_size
    result["built_bytes"] = new_bytes
    result["built_mib"] = round(new_bytes / (1024.0 * 1024.0), 2)
    result["built_mb"] = round(new_bytes / 1_000_000.0, 2)

    head_bytes = get_head_apk_size()
    if head_bytes is not None:
        result["vs_head_baseline"] = format_delta(new_bytes, head_bytes)
    elif BASELINE_APK_PATH.is_file():
        result["vs_head_baseline"] = format_delta(new_bytes, BASELINE_APK_PATH.stat().st_size)

    prev_bytes = None
    if METRICS_CACHE_PATH.is_file():
        try:
            prev = json.loads(METRICS_CACHE_PATH.read_text(encoding="utf-8"))
            cached = prev.get("debug_apk_bytes")
            if isinstance(cached, int) and cached > 0:
                prev_bytes = cached
        except Exception:
            pass
    if prev_bytes is None and BASELINE_APK_PATH.is_file():
        prev_bytes = BASELINE_APK_PATH.stat().st_size
    if prev_bytes is not None:
        result["vs_previous_build"] = format_delta(new_bytes, prev_bytes)

    if sync_root_apk:
        shutil.copy2(DEBUG_APK_PATH, BASELINE_APK_PATH)
        result["root_apk_synced"] = True

    if update_cache:
        METRICS_CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
        METRICS_CACHE_PATH.write_text(
            json.dumps(
                {
                    "debug_apk_bytes": new_bytes,
                    "updated_at": datetime.now().astimezone().isoformat(),
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="Update source_summary.md, sync root APK, and print post-build report.")
    parser.add_argument("--no-cache-update", action="store_true", help="Do not update app/build/.last_apk_metrics.json")
    parser.add_argument("--no-sync-apk", action="store_true", help="Do not copy built debug APK to repo root")
    parser.add_argument("--json", action="store_true", help="Output structured JSON report")
    args = parser.parse_args()

    files = collect_source_files()
    file_count, summary_lines = write_source_summary(files)
    changes = summarize_code_changes()
    apk = analyze_apk(update_cache=not args.no_cache_update, sync_root_apk=not args.no_sync_apk)

    report = {
        "summary_file": "source_summary.md",
        "summary_files_included": file_count,
        "summary_total_lines": summary_lines,
        "code_changes": changes,
        "apk": apk,
    }

    if args.json:
        print(json.dumps(report, indent=2, ensure_ascii=False))
        return

    print("=== POST-BUILD REPORT ===")
    print(f"Summary updated: source_summary.md ({file_count} files, {summary_lines} lines)")
    print(
        f"Code diff vs HEAD (excl. source_summary.md): "
        f"{len(changes['changed_files'])} modified (+{changes['total_added']} / -{changes['total_deleted']} lines), "
        f"{len(changes['untracked_files'])} untracked"
    )
    for item in changes["changed_files"]:
        print(f"  - {item['path']}: +{item['added']} / -{item['deleted']}")
    for u in changes["untracked_files"]:
        print(f"  - {u} (untracked)")

    if apk.get("built_exists"):
        print(
            f"Built APK ({apk['built_path']}): "
            f"{apk['built_bytes']:,} bytes ({apk['built_mib']} MiB / {apk['built_mb']} MB)"
        )
        if apk.get("root_apk_synced"):
            print(f"Root APK synced: {apk['root_apk_path']}")
        if "vs_previous_build" in apk:
            print(f"  vs previous build: {apk['vs_previous_build']['formatted']}")
        if "vs_head_baseline" in apk:
            print(f"  vs HEAD baseline (FridgeZero-debug.apk): {apk['vs_head_baseline']['formatted']}")
    else:
        print(f"Built APK not found at {apk['built_path']}")


if __name__ == "__main__":
    main()
