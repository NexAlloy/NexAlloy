#!/usr/bin/env python3
"""Read and update the per-package version code cache."""

from pathlib import Path
import json
import subprocess
import sys


def apk_pairs(package, directory):
    for apk in Path(directory).rglob("*.apk"):
        report = json.loads(subprocess.check_output([
            "android", "analyze", "--analysis=basic", "--format=json", str(apk)
        ]))
        info = report["results"][0]["basic"]["appInfo"]
        if info["packageName"] == package:
            yield info["versionName"], int(info["versionCode"])


package, version, *codes = sys.argv[1:]
from_dir = version == "--from-dir"
updates = list(apk_pairs(package, codes[0])) if from_dir else [
    (version, int(code)) for code in codes
]
cache = Path(__file__).resolve().parent.parent / "version-codes" / f"{package}.txt"
pairs = [
    (name, int(code))
    for name, code in (line.split("=") for line in cache.read_text().splitlines())
] if cache.exists() else []
pairs.extend(updates)
pairs = sorted(
    set(pairs),
    key=lambda pair: (tuple(map(int, pair[0].split("."))), pair[1]),
    reverse=True,
)

if updates:
    cache.parent.mkdir(exist_ok=True)
    cache.write_text("".join(f"{name}={code}\n" for name, code in pairs))

if not from_dir:
    for name, code in pairs:
        if name == version:
            print(code)
