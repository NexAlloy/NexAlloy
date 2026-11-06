# YouTube patch porting rules

This document describes general translation rules and interface mappings from upstream bytecode patches to this repository's runtime hooks.

## 1. Reuse extensions and translate patch execution

`app/build.gradle.kts` already includes the submodules' extension Java/Kotlin and proto sources in the app build. Implementations, settings, and filters under `app.morphe.extension.*` can be referenced directly without copying them.

Translate upstream `bytecodePatch { execute { ... } }` to `io.github.nexalloy.patch { ... }`, placing hook installation and callback registration in the local patch body.
Use this repository's equivalents for upstream `app.morphe.patches.*` patch dependencies and settings models, and for `app.morphe.patcher.*` fingerprint APIs. Map upstream `execute`, `finalize`, smali injection, and resource processing stages used to modify APKs to local build tasks or runtime entry points.

## 2. Derive hook arguments, results, and timing from register data sources

First determine whether the upstream injection point uses an argument, a call result, a field, or a local variable, then choose the hook. Common mappings:

| Upstream injection behavior | Local equivalent |
| --- | --- |
| Read or rewrite arguments at method entry | Read or write `it.args[n]` in `before` |
| Transform the original method's return value through an extension | Transform and assign `it.result` in `after` |
| Pass a call's `move-result` to an extension | Hook the called method's `after`, read `it.result`, and restrict the call scope if needed |
| Execute `return-void` when a condition holds, otherwise continue the original method | Set `it.result = null` in `before` under the same condition |
| Replace the entire original method with a constant return or no operation | Use `returnConstant` / replacement APIs |

For instance methods, smali `p0` corresponds to `it.thisObject`; explicit parameters are accessed through `it.args` in parameter order. Do not use DEX register numbers directly as `args` indices. Obtain method call results from `it.result`.

Use constant return APIs according to upstream's current conditions: if upstream returns early only when a setting is enabled, the local hook must preserve that condition.

For injection points inside a method, check when values are produced and consumed. Hook the corresponding getter, setter, or child method to obtain the target value. If the hook also affects other branches, add conditions equivalent to the original injection point.

Recheck the register contents at the actual injected call in the patched APK. Local variable names and apparent intent can be misleading: an argument may still hold a protocol object rather than display text. Match the surrounding calls and visibility changes as well as the argument source when choosing `before` or `after`.

## 3. Preserve call scope and prefer existing shared hooks

When upstream modifies only one call site but the called method has other uses, use `outerFingerprint.hookMethod(scopedHook(innerMember) { ... })`.
`scopedHook` restricts the inner callback through thread context while the outer method executes. Inside the inner callback, `it` refers to the inner invocation, while `outerParam` provides the outer invocation information.

When hooking shared methods, filter by call scope, resource ID, or another stable condition, and confirm that the intended calls are reached.

Continue reusing existing shared entry points for lifecycle events, Litho, image URLs, proto, and similar behavior.
Register lifecycle callbacks through `onCreateHook.add(callback)`, filters through `addLithoFilter(filter)`, and image URL callbacks through `addImageUrlHook`, `addImageUrlSuccessCallbackHook`, and `addImageUrlErrorCallbackHook`.
Look for the corresponding registration entry point before adding new behavior.

## 4. Preserve fingerprint semantics, then separate the members to hook

Recent upstream APIs such as `Fingerprint`, `AccessFlags`, `Opcode`, `ResourceType`, `methodCall`, `fieldAccess`, and `resourceLiteral` already have local compatibility implementations.
Prefer preserving method signatures, string comparison modes, filter order, `MatchAfterImmediately` / `MatchAfterWithin`, and version conditions while replacing the corresponding imports. Adapt `custom` callback forms to the local API.

Local DexKit candidate queries and full instruction matching are separate stages. `Fingerprint()` / `run()` checks query-result uniqueness; `@StrictQuery` applies instruction matching only when there are multiple candidates. Use `match()` / `instructionMatches` to validate instruction order, distance, and the actual injection point even when the query returns one method.

Generic types such as `"L"` may contribute no concrete constraint to a DexKit query. After conversion through `getTypeNameCompat`, add a field query only if at least one usable field constraint remains; an empty field matcher fails during query construction. Retain generic type, opcode, and location checks in instruction matching.

`classFingerprint` obtains parent classes through `queryAll()`, bypassing the parent's full instruction matching and `@StrictQuery` refinement. It can still find a unique child across multiple parent classes; check the final chain before changing the parent. If unrelated parent classes introduce ambiguous children, put the distinguishing parent conditions into its DexKit query. If the compatibility layer truly lacks a capability needed by this upstream change, extend the shared implementation and check other consumers.

`AnyInstruction.addQuery()` currently adds no DexKit constraint. For an OR of exact strings used to identify a method or parent class, translate each alternative into a separate `custom` / `addAnyOf` branch containing `addEqString`. Preserve the full strings and comparison mode; a shared substring can also occur in unrelated command handlers. Retain instruction filters when their position matters.

When upstream obtains references through `instructionMatches`, use the matched instructions' `methodRef`, `fieldRef`, and `classRef` locally, then expose independent finders through `findMethodDirect`, `findFieldDirect`, and `findClassDirect`.
Access runtime members in the patch through `::finder.method`, `.field`, `.clazz`, and related properties.

For calls to bundled libraries whose names or implementations change between APKs, locate the call relative to a matched instruction and constrain its invoke opcode, parameter types, and return type. Check both ordinary and range invoke forms when applicable, and require a unique result within that region. Verify the value reaches the same upstream injection point before choosing the runtime hook; a familiar library method name alone is insufficient.

When inspecting an APK with `dexsql`, a descriptor can appear in several DEX files as a definition or reference. If decoding is ambiguous, select the `file_id` containing the method body and pass `--file-id`; a reference alone does not show the implementation.

When a caller still reads references through `instructionMatches[n]`, recheck the meaning of each index after changing filters.
Matching a parent method may only serve to locate a child method that is the actual hook target.
For multiple targets, use `findMethodListDirect { }` + `Fingerprint.matchAll` + `::finder.dexMethodList` and install hooks individually. For a single target, check uniqueness.

Upstream Morphe's `Fingerprint.method` takes only the first match, whereas this project's `Fingerprint.run` checks uniqueness. This can expose multiple results in ambiguous upstream fingerprints. If such a fingerprint is only an intermediate step in a matching chain and the next step is unaffected (the dependent fingerprint yields the same result for any parent match), use `matchAll().last()` to handle it and add a comment explaining why.

When `anyInstruction` contains conditions beyond the `string()` filter, inspect its branches and consumers, then rewrite it as multiple Fingerprints. Compare each branch's enclosing method signature and called-method signature with upstream independently; copying one branch and leaving both signatures identical silently drops compatibility. Validate the final finder that selects the branches, even when its helper fingerprints use `@SkipTest`.

## 5. Use the shared override interface for feature flags

Upstream `insertLiteralOverride` for client Boolean flags usually maps to `youtube.insertLiteralOverride(flagId, valueOrCallback)`. The shared override entry point hooks flag query methods and passes the original value to the corresponding callback, so there is no need to port injection code separately for every query location.

Preserve the original version conditions and the extension's handling of the original value. First confirm that the constant belongs to this flag query mechanism.

## 6. Maintain runtime version branches and test annotations together

Version adaptation usually involves three parts: adding or updating version comparison results and their initialization; choosing the corresponding fingerprint in the patch body; and adding `@RequireAppVersion` / `@get:RequireAppVersion` to fingerprints or finder getters. Preserve supported older version paths and select match conditions and hook targets by version.

Distinguish explicit upstream version guards from comments describing signature variants. Preserve explicit guards. When upstream accepts multiple structural alternatives without a version guard, retain that acceptance through matching or a fallback finder. Check the available APKs before deriving a hard version boundary from a comment; a signature may already exist in earlier releases. Keep the upstream comments synchronized while basing runtime selection and test applicability on the supported behavior.

`@RequireAppVersion` is read by fingerprint tests and does not automatically guard runtime hooks. The current `UnitTestAnnotations.kt` treats both min and max boundaries as inclusive; check the implementation when specifying test ranges. `@SkipTest` is for helper fingerprints that are not used directly or intentionally match multiple targets and are covered by another finder. The final finder actually used should still be validated.
If the last version targeted by a fingerprint is `x.y.z`, `@RequireAppVersion` can use `x.y+1.000` as `maxVersion`, because YouTube does not release versions where `z` is zero.

`FingerprintsKtTest` does not have to pass completely. If the same Fingerprint fails only on one APK/version but actually runs and passes on all other available relevant APKs (without being skipped), ignore that isolated failure. Short-lived A/B tests may leave two code variants. There is no need to prove the cause, explain, investigate, or attempt a fix; do not list such failures in reports or validation gaps. Skipped tests do not count as passes.

## 7. Port settings keys and structure alongside behavior

Copy all Preference declarations in `*Patch.kt` from upstream, preserving entries, order, grouping, version conditions, and related comments. When a feature is not implemented, comment out its Preference declaration; if an entire group is unimplemented, the whole group may be commented out. Preserve complete declarations and their arguments for comparison and later restoration.

Preserve the upstream meaning of `PreferenceScreen.*.addPreferences` and preference keys, summaries, entries, and entryValues, using local `shared.misc.settings.preference` models. Replace string class names for upstream custom preferences with the corresponding `Class` references passed to `tag`.

When upstream moves or renames settings, check removal of old entries, duplicates, and local layout references. The build already generates strings and copies resources; inspect the input scope of those tasks before adding resource handling.

Reflect setting structure changes in local screen/group construction interfaces and Preference models, including icons, layouts, ordering, dependencies, and new attributes. When multiple features append to the same settings group, reuse a shared registration entry point and insert the group only once. When a custom Preference depends on properties assigned at runtime, preserve the order: build it, assign properties, then attach it.

## 8. Adapt dependencies and registration to local execution

Local `dependsOn` executes dependency patches first and records their success. Preserve dependencies for initialization and shared hooks actually used. When local build tasks or global entry points handle work performed by upstream dependencies, verify the corresponding implementation before adjusting dependencies.

Keep synchronized patches reachable through `YouTubePatches` or dependencies. An extracted dependency that preserves an existing feature does not need a new user-facing patch entry. Add new patches only when explicitly requested by the user. Build settings after all features have registered their settings; the shared flag hook applies registered overrides. Defining a patch variable alone does not execute it.

## 9. Translate added bytecode members to reflection and adapter interfaces

When upstream adds helper methods, interfaces, or field access bridges to host classes, first check whether local fingerprints can resolve the members, then perform the same work through reflection and existing proxy/interface adapters.

When reading fields, calling methods, or passing host objects through adapter interfaces, also check object types, field availability, and read/write timing.

For reflective static calls, pass `null` as the receiver before the explicit method arguments (`Method.invoke(null, ...)`).
Preserve upstream nullability, including enum lookups that may return null and are accepted by the extension.

## 10. Sync upstream comments and preserve implementation locations and progress markers

Sync comments in `*Patch.kt` with upstream, preserving the original wording, feature descriptions, and `region` / `endregion` sections wherever possible for comparison. Minimize new local comments; add brief explanations only when necessary to describe porting differences or additional constraints.

Organize implementations using upstream feature descriptions or regions. For unimplemented features, retain the original description or region at the corresponding location and add a `TODO`. For partial implementations, place the `TODO` at the missing part. This makes coverage visible from the implementation and ties it to the commented-out Preference.

Once a feature is complete, restore its Preference and remove TODOs for the completed parts, keeping upstream descriptions and sections.

## 11. Sync feature additions, removals, replacements, and moves

When upstream removes a hook, flag override, or call chain, or replaces it with a new mechanism, identify and remove the corresponding obsolete local behavior. If the replacement is not implemented, comment out the corresponding Preference and mark a TODO under the original feature description or region. Do not keep modifications withdrawn upstream merely because the replacement is missing.

When upstream renames or extracts logic into shared directories, follow references to update local imports, dependencies, callback registration, screen definitions, and resource names, and remove unused fingerprints and registrations. Check whether an existing shared hook already covers the behavior to avoid duplicate hooks.

New upstream leaf directories may contain new features or newly extracted dependencies of existing features. Sync existing local ports without independently adding patches, including new upstream patches, unless the user explicitly requests them. Omit patches without local counterparts from the Markdown report. Port extracted dependencies and structural changes required to preserve existing behavior without enabling additional features, and report them with their affected local patches. Changes fully provided by the extension itself with compatible interfaces can be recorded as taking effect through the submodule merge.

## Limits of these rules

Record commented-out features, unregistered patches, and incomplete branches separately. Check the execution paths affected by the sync, identify covered behavior and gaps, and validate changes using the original APK, fingerprint tests, and the upstream patched APK.
