---
name: merge-upstream-patches
description: Merge a new Morphe patches upstream version into NexAlloy's morphe-patches submodule and port relevant patch changes into the main app. Use for upstream patch syncs, not ordinary local patch edits.
---

# Merge upstream patches

Use this workflow in the NexAlloy repository. `morphe-patches` is a Git submodule: its `nexalloy` branch is the fork being merged, and its `upstream` remote points to MorpheApp. 
The ported code lives in `app/src/main/java/io/github/nexalloy/morphe`. 
The upstream patch sources live in `morphe-patches/patches/src/main/kotlin/app/morphe/patches`. 
Read `DEVELOPMENT.md` for this project's fingerprint and hook translation conventions; some paths in that guide still use the older `revanced-patches` name.

## Required inputs before starting

Before beginning the merge and porting work, obtain these inputs from the user:

- The target Morphe patches version/tag to merge.
- The filesystem path to the original YouTube APK used as the reference for that target version.
- The filesystem path to the corresponding Morphe Patched YouTube APK, based on the same YouTube version and patched with the target Morphe patches version.

Reuse inputs already supplied in the conversation and ask only for missing or incorrect inputs before proceeding. The user supplies both APKs; do not search for or download them, or run `morphe-desktop` to produce the patched APK. Verify that the supplied paths are readable and that the original and patched APKs are the intended comparison pair. If they are missing or mismatched, ask the user to provide the correct pair.

## 1. Establish the version and baseline

- Check status in the root repository and both submodules, `morphe-patches` and `morphe-patches-library`; preserve unrelated work and record both submodules' current commits. Verify their branches/remotes and fetch upstream tags and branch history to resolve the target versions.
- Record the old submodule commit, the upstream tag on which its last merge was based, and the exact target tag/commit. Inspect the first-parent history and merge parents (`git log --first-parent`, `git show --no-patch --pretty=raw <merge>`, `git tag --points-at <merge>^2`) rather than relying on `git describe` for a merge commit. Confirm the tag really identifies the old upstream baseline. If it does not, trace the actual upstream parent before diffing.
- Review the root repository's previous sync commit and subsequent port fixes, including changes ported ahead of the next upstream merge. Preserve those fixes when reconciling the new target. Record relevant existing fingerprint failures against the available original APKs before modifying the port, so they can be distinguished from regressions; exclude failures accepted by the fingerprint test rule in step 4.
- Review the release notes and upstream commits between the baseline and target. Determine the library merge target from the version catalog after merging `morphe-patches`, as described below.

## 2. Merge upstream in the submodule

Read [Submodule integration rules](references/submodule-integration.md) before resolving either merge. Capture each submodule's existing fork changes relative to its old upstream baseline; these include required extension adapters even when Git reports no conflicts.

1. First merge the chosen upstream tag/commit into `morphe-patches` on its `nexalloy` branch and resolve the merge.
2. Read `[versions].morphe-patches-library` in the merged `morphe-patches/gradle/libs.versions.toml`. Use that declared version to identify the corresponding upstream tag/commit for `morphe-patches-library`.
3. Then merge that library target into `morphe-patches-library` on its `nexalloy` branch and resolve the merge. If the required target is already an ancestor of the current library commit, record that it is already included. Do not independently select the latest library release.
4. Preserve both submodules' fork history, record their merge results, and update their pointers in the root repository after the merges are resolved. Do not merge upstream into the root repository's `main` branch.

Resolve changed upstream signatures and behavior together with the existing NexAlloy callback bridges, resource lookup, settings construction, and runtime adapters. Update the corresponding root-side consumers in the same sync. Verify the resulting fork changes against the new upstream parent as well as reviewing conflict resolutions.

## 3. Find patch changes to port

Compare the **old upstream baseline tag** with the **upstream commit merged in**. First inspect the full `git diff --name-status -M <old-tag> <new-upstream-commit>`, then inspect patch changes with the same command scoped to `patches/src/main/kotlin/app/morphe/patches`, from inside the submodule. Inspect the actual hunks for added, changed, deleted, and renamed files. Also inspect the library upstream delta, especially `extension-library`. Do not use the fork merge diff as the upstream change list: it also reflects local fork changes and conflict resolutions.

Group changed Kotlin files by the relevant leaf patch folder in the old and new upstream trees; include deleted and renamed folders. Inspect nearby `*Patch.kt`, `Fingerprints.kt`, dependencies, and changed shared code. Match each affected upstream patch to its implementation under `app/src/main/java/io/github/nexalloy/morphe` by behavior and references, not path alone. Include supported apps and shared dependencies beyond YouTube when they are affected.

Keep a compact coverage list for changed upstream patches with local counterparts and shared components consumed by local patches, recording the local consumers, the behavior changed, the action taken, and validation or remaining gaps. Distinguish changes already covered locally, changes supplied directly by merged extensions, changes requiring a port, and behavior left unimplemented. Sync existing local ports; do not independently add patches, including newly introduced upstream patches, unless the user explicitly requests them. When a local counterpart is missing, determine whether the change is a new feature or a dependency extracted from existing behavior. Exclude new features without local counterparts from the Markdown report. Port shared dependencies and structural changes needed to preserve existing features without enabling additional features. Within partial ports, leave explicit disabled Preferences and implementation TODOs for uncovered behavior.

Trace changed extension APIs and shared helpers to their local adapters, callers, and registrations even if those local patch folders have no upstream diff. Review `FingerprintCompat.kt`, instruction helpers, preference models, app build inputs, and `app/src/main/res` when their contracts or resources change. Review fork-specific conflict resolutions separately because they can change behavior beyond the upstream diff.

Port applicable changes using this repository's DexKit/Xposed conventions in `DEVELOPMENT.md`. Keep fingerprint selection, version conditions, injection point, hook behavior, and patch registration aligned with the merged upstream behavior. Remove or replace obsolete hooks when upstream removes or replaces their behavior. Keep any required fork integration changes in the submodules and the runtime port in the root repository; changing upstream patch sources alone does not update the runtime port. If an upstream change cannot be translated reliably, identify the exact patch and unresolved behavior.

For YouTube patch ports, read [YouTube porting rules](references/youtube-porting.md) for interface mappings and rules covering hook timing, scoped calls, fingerprint references, settings, and registration. Check each rule against the actual injection point; existing incomplete ports are not a reason to omit upstream behavior.

## 4. Verify

- Use [test-fingerprints](../test-fingerprints/SKILL.md) for APK naming, recursive input discovery, dot-prefixed exclusions, and focusing a run on one application.
- Build the integrated app with `./gradlew :app:assembleDebug` to check Kotlin, Java extension adapters, generated resources, and packaging together. Resolve failures caused by the sync; distinguish any existing environment or build failures.
- Put the user-provided original APKs in `app/binaries/` with filenames prefixed by the target package name; keep patched comparison APKs outside the test input directory. Run the `:app:FingerprintsKtTest` test class, for example from the repository root with `./gradlew :app:testDebugUnitTest --tests io.github.nexalloy.FingerprintsKtTest`. The test discovers APKs through `app/src/test/java/io/github/nexalloy/FilePathArgumentsProvider.kt`. Check affected fingerprints and shared dependencies against the available relevant app versions, including match count and method/class identity. Inspect executed tests and skips: a skipped or undiscovered fingerprint is not a validated match. Report version branches not covered by the available APKs; a passing test alone does not prove the intended injection point.
- `FingerprintsKtTest` does not have to pass completely. If an individual fingerprint fails on one APK/version but passes on all other available relevant APKs in actual, non-skipped executions, ignore that isolated failure. Short-lived A/B tests can leave two code variants; no explanation, investigation, or attempted fix is needed, and proving an A/B test is not required. Do not include these ignored failures in reports or validation gaps. Skipped tests do not count as passes.
- Read the JSON written directly by `FingerprintsKtTest` at `app/build/reports/fingerprints/testDebugUnitTest.json`. Use `python .agents/skills/merge-upstream-patches/scripts/summarize_fingerprints.py` for filtering and the isolated failure rule. Read [Fingerprint result analysis](references/fingerprint-results.md) for the schema, matched members, skip reasons, and coverage limits. Do not reconstruct results from HTML/XML or console logs.
- For ambiguous obfuscated classes or methods, inspect the same APK with `dexsql`: `dexsql tables --input <apk>` shows the schema; `dexsql query --input <apk> --sql "SELECT descriptor FROM classes WHERE descriptor LIKE 'Lcom/google/%' LIMIT 20"` locates candidates; `dexsql decode --input <apk> --class '<descriptor>'` or `--method '<descriptor>'` shows smali. Compare strings, calls, control flow, and target versions with the upstream fingerprint before adjusting a local matcher.
- When an injection point or fingerprint remains unclear, use the user-provided Morphe Patched YouTube APK as the comparison artifact. Locate the injected call or modified method in that APK with `dexsql`, compare it to the supplied original APK, and verify that the local hook targets the equivalent method and timing. Do not treat a patched APK's package or version as interchangeable with the original input.
- Review status, `git diff --check`, and unresolved merge entries in the root repository and both submodules. Verify that the root gitlinks identify the completed submodule merges. Reconcile the coverage list with final changes, disabled Preferences, and TODOs; check that obsolete hook registrations and resource references were removed or updated. Report the upstream baseline and target, the library version selected from the version catalog, affected leaf folders and decisions, conflict resolutions, tests/APKs used, and any unresolved validation gaps other than the ignored fingerprint failures above. Mark TODO behavior as unimplemented rather than reporting it as a completed port.
- In the Markdown report, list every changed upstream patch that has a local counterpart, including partial ports and changes requiring no new local edits. Include shared dependencies where they affect existing local patches. Omit patches with no local counterpart entirely; do not add rows for them labeled "outside scope", "no local counterpart", or similar. The full upstream diff must still be reviewed to identify affected local patches and dependencies.