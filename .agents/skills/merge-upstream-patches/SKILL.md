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

Before beginning the merge and porting work, establish these inputs:

- The target Morphe patches version/tag to merge.
- The filesystem path to the original YouTube APK used as the reference for that target version.
- The filesystem path to the corresponding Morphe Patched YouTube APK, based on the same YouTube version and patched with the target Morphe patches version.

Reuse inputs already supplied in the conversation. Obtain missing original APKs with [download-apks](../download-apks/SKILL.md), including the latest and the selected Morphe release's supported and experimental versions for affected apps. Use the user's requested source or method for the patched comparison APK; when instructed to use Morphe Desktop CLI, patch the same original with the selected patches release. Verify that the original and patched APKs are the intended comparison pair before porting.

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

Use a working checklist to account for every changed upstream patch with a local counterpart and every changed shared component consumed by local patches. For each, track the local consumers, behavior changes, porting decisions, and validation gaps, including cases where no local edit is needed. Sync existing local ports; do not independently add patches unless the user explicitly requests them. When a local counterpart is missing, determine whether the change is a new feature or a dependency extracted from existing behavior. Port shared dependencies and structural changes needed to preserve existing features without enabling additional features. Within partial ports, leave explicit disabled Preferences and implementation TODOs for uncovered behavior.

Trace changed extension APIs and shared helpers to their local adapters, callers, and registrations even if those local patch folders have no upstream diff. Review `FingerprintCompat.kt`, instruction helpers, preference models, app build inputs, and `app/src/main/res` when their contracts or resources change. Review fork-specific conflict resolutions separately because they can change behavior beyond the upstream diff.

### Sync patcher fingerprint APIs

Read `[versions].morphe-patcher` from the old and merged `morphe-patches/gradle/libs.versions.toml`. Compare the corresponding patcher versions' [Fingerprint.kt](https://github.com/MorpheApp/morphe-patcher/blob/main/src/main/kotlin/app/morphe/patcher/Fingerprint.kt) and [InstructionFilter.kt](https://github.com/MorpheApp/morphe-patcher/blob/main/src/main/kotlin/app/morphe/patcher/InstructionFilter.kt); resolve those links to the relevant tags or commits rather than assuming current `main` is the merge target. Their local counterparts are `app/src/main/java/io/github/nexalloy/morphe/FingerprintCompat.kt` and `InstructionFilter.kt`.

Review constructor/factory overloads, parameter types and defaults, nullability, comments, and query/matching semantics, including dynamic type and reference suppliers. Update the shared compatibility APIs before introducing per-patch workarounds for missing upstream forms. Preserve the local DexKit types and execution model: suppliers resolve lazily with the current `DexKitBridge`, query and instruction matching use consistent values, and singleton fingerprints/filters must not cache one APK's resolved values globally across bridges.

After adding API support, audit existing `findMethodDirect` wrappers and parameterized fingerprint factories. Migrate those now expressible directly to `Fingerprint` objects, updating imports and hook/member access. Preserve version, target-app, and abstract-method test annotations when moving getter declarations to objects. Keep direct finders where extracting a different member, filtering multiple matches, or other additional logic is still required.

### Port patch behavior

Port applicable changes using this repository's DexKit/Xposed conventions in `DEVELOPMENT.md`. Keep fingerprint selection, version conditions, injection point, hook behavior, and patch registration aligned with the merged upstream behavior. Remove or replace obsolete hooks when upstream removes or replaces their behavior. Keep any required fork integration changes in the submodules and the runtime port in the root repository; changing upstream patch sources alone does not update the runtime port. If an upstream change cannot be translated reliably, identify the exact patch and unresolved behavior.

For YouTube patch ports, read [YouTube porting rules](references/youtube-porting.md) for interface mappings and rules covering hook timing, scoped calls, fingerprint references, settings, and registration. Check each rule against the actual injection point; existing incomplete ports are not a reason to omit upstream behavior.

## 4. Verify

- Use [test-fingerprints](../test-fingerprints/SKILL.md) for APK naming, recursive input discovery, dot-prefixed exclusions, and focusing a run on one application.
- Build the integrated app with `./gradlew :app:assembleDebug` to check Kotlin, Java extension adapters, generated resources, and packaging together. Resolve failures caused by the sync; distinguish any existing environment or build failures.
- Follow the APK placement rules in [test-fingerprints](../test-fingerprints/SKILL.md). Run the `:app:FingerprintsKtTest` test class, for example from the repository root with `./gradlew :app:testDebugUnitTest --tests io.github.nexalloy.FingerprintsKtTest`. The test discovers APKs through `app/src/test/java/io/github/nexalloy/FilePathArgumentsProvider.kt`. Check affected fingerprints and shared dependencies against the available relevant app versions, including match count and method/class identity. Inspect executed tests and skips: a skipped or undiscovered fingerprint is not a validated match. Report version branches not covered by the available APKs; a passing test alone does not prove the intended injection point.
- `FingerprintsKtTest` does not have to pass completely. If an individual fingerprint fails on one APK/version but passes on all other available relevant APKs in actual, non-skipped executions, ignore that isolated failure. Short-lived A/B tests can leave two code variants; no explanation, investigation, or attempted fix is needed, and proving an A/B test is not required. Do not include these ignored failures in reports or validation gaps. Skipped tests do not count as passes.
- Read the JSON written directly by `FingerprintsKtTest` at `app/build/reports/fingerprints/testDebugUnitTest.json`. Use `python .agents/skills/merge-upstream-patches/scripts/summarize_fingerprints.py` for filtering and the isolated failure rule. Read [Fingerprint result analysis](references/fingerprint-results.md) for the schema, matched members, skip reasons, and coverage limits. Do not reconstruct results from HTML/XML or console logs.
- When changing compatibility APIs or migrating finders, prefer exercising them through the migrated real fingerprints in the existing runner. Compare the baseline and resulting JSON by APK and fingerprint, accounting for renames; verify matched member identities and actual execution/skip coverage. Check full instruction matching as well as DexKit query selection.
- For ambiguous obfuscated classes or methods, inspect the same APK with `dexsql`: `dexsql tables --input <apk>` shows the schema; `dexsql query --input <apk> --sql "SELECT descriptor FROM classes WHERE descriptor LIKE 'Lcom/google/%' LIMIT 20"` locates candidates; `dexsql decode --input <apk> --class '<descriptor>'` or `--method '<descriptor>'` shows smali. Compare strings, calls, control flow, and target versions with the upstream fingerprint before adjusting a local matcher.
- When an injection point or fingerprint remains unclear, use the Morphe Patched YouTube APK as the comparison artifact. Locate the injected call or modified method in that APK with `dexsql`, compare it to the original APK, and verify that the local hook targets the equivalent method and timing. Do not treat a patched APK's package or version as interchangeable with the original input.
- Review status, `git diff --check`, and unresolved merge entries in the root repository and both submodules. Verify that the root gitlinks identify the completed submodule merges. Reconcile the working checklist with final changes, disabled Preferences, and TODOs; check that obsolete hook registrations and resource references were removed or updated.
- Give a brief final reply with the patch and library versions, the main porting changes, what was tested, and any unimplemented behavior or remaining validation gaps. Keep the per-patch checklist out of the reply. Create a sync report file only on explicit request, save it under the ignored root `build/` directory, and keep it out of version control.
