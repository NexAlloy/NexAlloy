---
name: download-apks
description: Download original APKs from play.chsbuffer.work for NexAlloy testing, including latest and Morphe supported or experimental versions, with a Git-tracked version-name/code cache and manual version lookups on APKMirror.
---

# Download APKs

Keep version-name mapping separate from downloading. The downloader takes a package name, version code and output path. It uses:

```text
https://play.chsbuffer.work/download/<package>/<versionCode>/base.apk?old_store=1&device_name=px_9a
```

Version code `0` requests the latest version. YouTube is `com.google.android.youtube`; YouTube Music is `com.google.android.apps.youtube.music`.

## Choose versions and resolve codes

- For a Morphe version matrix, read the selected upstream checkout's `patches/src/main/kotlin/app/morphe/patches/<app>/shared/Constants.kt`. Include supported and experimental targets, plus the latest APK via code `0`.
- For YouTube Music fingerprint testing, one `arm64-v8a` APK per version name is sufficient.
- Check the cache first. On a miss, open APKMirror's release/variant pages or indexed results and read the version name, code and architecture. APKMirror is the default source for these lookups.
- Cache verified codes immediately. Keep multiple codes for the same version name. Never derive a code from neighboring versions. Reuse cache hits without revisiting APKMirror.
- If APKMirror or the download endpoint presents a Cloudflare challenge or block, stop the batch and report the unresolved versions. Do not retry or bypass the block.
- Process version names in numeric ascending order, then codes in numeric ascending order. Select by the recorded architecture. Do not infer architecture from code suffixes. Confirm Android requirements separately when needed.

The cache is `version-codes/<package>/<architectures>.txt` inside this skill, tracked in Git. Use `universal.txt` for APKs containing all four ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`. Otherwise, the filename lists the APK's native ABIs, comma-separated and sorted; `unknown` means no native ABI was available from the APK. Each line remains one `versionName=versionCode` pair. The scripts use pairs rather than a dictionary keyed by version name. ABI queries include matching universal APKs; `--arch universal` selects universal APKs only.

```sh
# Print cached codes and architectures, ascending. Empty output means a cache miss.
python .agents/skills/download-apks/scripts/version_codes.py com.google.android.apps.youtube.music 9.46.51

# Select an ABI; APKs containing that ABI also match.
python .agents/skills/download-apks/scripts/version_codes.py --arch arm64-v8a com.google.android.apps.youtube.music 9.46.51

# Add confirmed mappings, merge exact duplicates and sort the cache.
python .agents/skills/download-apks/scripts/version_codes.py --arch arm64-v8a com.google.android.apps.youtube.music 9.46.51 94651240

# Import mappings from all APKs in a directory and its subdirectories.
python .agents/skills/download-apks/scripts/version_codes.py com.google.android.apps.youtube.music --from-dir app/binaries
```

Directory import uses `android analyze` to read each APK's manifest and caches only entries whose package name matches. It reads native ABIs from `lib/<ABI>/*.so` in the APK. Keep cache changes in version control with the task. The cache helper manages local mappings and APK imports; the download helper performs no version lookups or cache updates.

## Download and verify

Save downloaded APKs under `app/binaries/` using the placement and naming rules in [test-fingerprints](../test-fingerprints/SKILL.md). Set `output_apk` to the chosen path, create its parent directory, and call the downloader with a resolved code or `0`:

```sh
bash .agents/skills/download-apks/scripts/download_apk.sh com.google.android.apps.youtube.music 0 "$output_apk"
```

Read the downloaded APK's manifest to verify package, version name and version code. For `0`, use its actual manifest version for the final filename and import the mapping into the cache. Reuse an existing verified APK of the same package and code.

A cached mapping does not establish test coverage. Check the downloaded version names against the requested version matrix, then confirm those inputs in the fingerprint report.
