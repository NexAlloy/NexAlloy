---
name: test-fingerprints
description: Run NexAlloy multi-version fingerprint tests, organize and select APK inputs in app/binaries, and verify matched targets and coverage from the JSON report.
---

# Test fingerprints

Use the existing APK-based `FingerprintsKtTest` to validate fingerprint and final
finder matches across the relevant application versions.

## APK inputs

`app/binaries/` contains local original APK inputs for `FingerprintsKtTest`.
The app module runs the test with `binaries/` as the input path relative to its
working directory. APKs are ignored by Git; use ignored `captures/` for logs and
temporary inspection output. Keep patched comparison APKs outside the active
test input tree.

For missing original APKs, use [download-apks](../download-apks/SKILL.md) to obtain
the latest and Morphe supported/experimental versions, reusing its tracked
version-name/code cache before querying APKMirror.

## File names and application folders

The APK's **file name must start with a recognized package name**. The test uses
this prefix to select the application's fingerprints, independently of the
containing folder:

| File name prefix | Application |
| --- | --- |
| `com.google.android.youtube` | YouTube |
| `com.google.android.apps.youtube.music` | YouTube Music |
| `com.reddit.frontpage` | Reddit |

Use descriptive names such as `<package>-<versionName>-<versionCode>.apk`. Only the package prefix is required for routing. Renaming
a file cannot add support for an application absent from the routing table.

Prefer a separate folder for each application, named after its package. Discovery
is recursive, so subfolders are also supported. Keep only APKs
in visible input paths: the input provider enumerates regular files without an
`.apk` extension filter, so visible notes or other non-APK files can break setup.

## Excluding inputs and focusing one application

Any file or folder whose name starts with `.` is excluded. The rule applies to
every component of the relative path at any depth:

- Prefix a file name with `.` to exclude one APK.
- Prefix a folder name with `.` to exclude all its descendants.
- Put inactive APKs in a `.disabled/` subfolder to leave the rest of an app active.

When testing one application, you can prefix the other application folders
with `.`. For example, this layout tests only the visible YouTube APKs:

```text
app/binaries/
  com.google.android.youtube/
    com.google.android.youtube-<versionName>-<versionCode>.apk
    .disabled/
      com.google.android.youtube-<inactiveVersion>-<versionCode>.apk
  .com.google.android.apps.youtube.music/
    com.google.android.apps.youtube.music-<versionName>-<versionCode>.apk
  .com.reddit.frontpage/
    com.reddit.frontpage-<versionName>-<versionCode>.apk
```

Confirm the intended
APKs appear in `app/build/reports/fingerprints/testDebugUnitTest.json`.

For YouTube Music, one `arm64-v8a` APK per version name is sufficient. Check
required version names against the report's actual APK inputs.

The input discovery and application routing are implemented in
[FilePathArgumentsProvider.kt](../../../app/src/test/java/io/github/nexalloy/FilePathArgumentsProvider.kt)
and [FingerprintTestReport.kt](../../../app/src/test/java/io/github/nexalloy/FingerprintTestReport.kt).

## Run and verify coverage

Run from the repository root; use `.\gradlew.bat` on Windows:

```sh
./gradlew :app:testDebugUnitTest --tests io.github.nexalloy.FingerprintsKtTest
```

Read `app/build/reports/fingerprints/testDebugUnitTest.json` directly, or filter it
with the existing helper:

```sh
python .agents/skills/merge-upstream-patches/scripts/summarize_fingerprints.py --group '<package-or-group-glob>' --details
```

Read the [fingerprint result analysis rules](../merge-upstream-patches/references/fingerprint-results.md)
for the report schema, filter options, and isolated-failure handling. 

Check the affected final finders and their dependencies on every relevant active
APK. Inspect the actual matched method, field, and class identities, not just test
counts. A skipped, undiscovered, or null result is not a validated match. For
version-specific branches, verify actual matches on both sides of the boundary;
report missing coverage rather than inferring it from skips or absent samples.

Reuse the general multi-version runner. Do not retain unit tests tied to a
particular APK path, version, or obfuscated member.

Fingerprint matching does not establish runtime correctness.
