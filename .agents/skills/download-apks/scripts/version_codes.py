#!/usr/bin/env python3
"""Read and update version code mappings grouped by APK architecture."""

import argparse
from pathlib import Path
import json
import subprocess
from zipfile import ZipFile


UNIVERSAL_ABIS = {"arm64-v8a", "armeabi-v7a", "x86", "x86_64"}


def architecture_name(abis):
    return "universal" if set(abis) == UNIVERSAL_ABIS else ",".join(sorted(abis)) or "unknown"


def apk_pairs(package, directory):
    for apk in Path(directory).rglob("*.apk"):
        report = json.loads(subprocess.check_output([
            "android", "analyze", "--analysis=basic", "--format=json", str(apk)
        ]))
        info = report["results"][0]["basic"]["appInfo"]
        if info["packageName"] == package:
            with ZipFile(apk) as archive:
                architectures = {
                    name.split("/")[1] for name in archive.namelist()
                    if name.startswith("lib/") and name.endswith(".so")
                }
            yield info["versionName"], int(info["versionCode"]), architecture_name(architectures)


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("package")
parser.add_argument("version", nargs="?")
parser.add_argument("codes", nargs="*", type=int)
parser.add_argument("--arch", help="ABI, comma-separated ABIs, or universal")
parser.add_argument("--from-dir", type=Path, help="Import matching APK manifests and native ABIs")
args = parser.parse_args()
if args.codes and not args.arch:
    parser.error("--arch is required when adding codes")
if not args.from_dir and not args.version:
    parser.error("a version or --from-dir is required")

updates = list(apk_pairs(args.package, args.from_dir)) if args.from_dir else [
    (args.version, code, architecture_name(args.arch.split(","))) for code in args.codes
]
cache = Path(__file__).resolve().parent.parent / "version-codes" / args.package
pairs = [
    (name, int(code), file.stem)
    for file in cache.glob("*.txt")
    for name, code in (line.split("=") for line in file.read_text().splitlines())
]
pairs.extend(updates)
pairs = sorted(
    set(pairs),
    key=lambda pair: (tuple(map(int, pair[0].split("."))), pair[1]),
)

if updates:
    cache.mkdir(parents=True, exist_ok=True)
    for arch in sorted({arch for _, _, arch in pairs}):
        (cache / f"{arch}.txt").write_text("".join(
            f"{name}={code}\n" for name, code, value in pairs if value == arch
        ))

if not args.from_dir:
    requested_abis = UNIVERSAL_ABIS if args.arch == "universal" else set(args.arch.split(",")) if args.arch else set()
    for name, code, arch in pairs:
        abis = UNIVERSAL_ABIS if arch == "universal" else set(arch.split(","))
        if name == args.version and requested_abis.issubset(abis):
            print(f"{code}\t{arch}")
