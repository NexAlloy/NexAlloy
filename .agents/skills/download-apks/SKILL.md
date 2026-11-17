---
name: download-apks
description: Download original APKs from play.chsbuffer.work for NexAlloy testing, including latest and Morphe supported or experimental versions, with a Git-tracked version-name/code cache and APKMirror as the default mapper.
---

# Download APKs

Keep version-name mapping separate from downloading. The downloader takes a package name, version code and output path. It uses:

```text
https://play.chsbuffer.work/download/<package>/<versionCode>/base.apk?old_store=1&device_name=px_9a
```

Version code `0` requests the latest version. YouTube is `com.google.android.youtube`; YouTube Music is `com.google.android.apps.youtube.music`.

## Choose versions and resolve codes

- For a Morphe version matrix, read the selected upstream checkout's `patches/src/main/kotlin/app/morphe/patches/<app>/shared/Constants.kt`. Include supported and experimental targets, plus the latest APK via code `0`.
- Check the cache first. On a miss, use APKMirror's release/variant pages or indexed results to confirm the exact version name and code. APKMirror is the default mapper; a different mapper can supply the same verified pairs without changing either script.
- Cache verified codes immediately. Keep multiple codes for the same version name. Never derive a code from neighboring versions. Reuse cache hits without revisiting APKMirror.
- If APKMirror or the download endpoint presents a Cloudflare challenge or block, stop the batch and report the unresolved versions. Do not retry or bypass the block.
- Process version names in numeric descending order, then codes in numeric descending order. Select the APK variant needed for the task; cache ordering does not encode architecture or Android requirements.

The cache is `version-codes/<package>.txt` inside this skill, tracked in Git. Each line is one `versionName=versionCode` pair. The scripts use pairs rather than a dictionary keyed by version name.

```sh
# Print cached codes for one version, descending. Empty output means a cache miss.
python .agents/skills/download-apks/scripts/version_codes.py com.google.android.apps.youtube.music 9.46.51

# Add confirmed codes, merge exact duplicates and sort the cache.
python .agents/skills/download-apks/scripts/version_codes.py com.google.android.apps.youtube.music 9.46.51 94651240 94651280

# Import mappings from all APKs in a directory and its subdirectories.
python .agents/skills/download-apks/scripts/version_codes.py com.google.android.apps.youtube.music --from-dir app/binaries
```

Directory import uses `android analyze` to read each APK's manifest and caches only entries whose package name matches. Keep cache changes in version control with the task. The cache helper makes no mapper requests; the download helper performs no mapping or cache updates.

## Download and verify

Save downloaded APKs under `app/binaries/` using the placement and naming rules in [test-fingerprints](../test-fingerprints/SKILL.md). Set `output_apk` to the chosen path, create its parent directory, and call the downloader with a resolved code or `0`:

```sh
bash .agents/skills/download-apks/scripts/download_apk.sh com.google.android.apps.youtube.music 0 "$output_apk"
```

Read the downloaded APK's manifest to verify package, version name and version code. For `0`, use its actual manifest version for the final filename and resolve/cache that version through the mapper. Reuse an existing verified APK of the same package and code.
