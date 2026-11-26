# Development Guide

## Project Structure

The primary hook entry point is [MainHook.kt](app/src/main/java/io/github/nexalloy/MainHook.kt).

### Patch Organization and Conventions

Patches adhere to a specific structure:

```text
📦your.patches.app.category
 ├ Fingerprints.kt
 └ SomePatch.kt
```

-   **Project-specific patches:** [app/src/main/java/io/github/nexalloy/morphe](app/src/main/java/io/github/nexalloy/morphe)
-   **Upstream patches:** [Morphe patch sources](morphe-patches/patches/src/main/kotlin/app/morphe/patches)

The Morphe submodule provides upstream patch sources and shared extension code.

### Contoso Patch Example

#### Add Contoso to module scope

- `app/src/main/AndroidManifest.xml`: Query package for module settings
- `app/src/main/res/values/arrays.xml`: Xposed scope recommendation
- `README.md`

#### `Fingerprints.kt`
```kotlin
package io.github.nexalloy.morphe.contoso.misc.unlock.plus

import io.github.nexalloy.morphe.Fingerprint

internal object IsPlusUnlockedFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("genius"),
)
```

#### `UnlockPlusPatch.kt`
```kotlin
package io.github.nexalloy.morphe.contoso.misc.unlock.plus

import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import io.github.nexalloy.patch

val UnlockPlus = patch(name = "Unlock Plus") {
    IsPlusUnlockedFingerprint.hookMethod(returnConstant(true))
}
```

#### `ContosoHook.kt`
```kotlin
package io.github.nexalloy.morphe.contoso

import io.github.nexalloy.morphe.contoso.misc.unlock.plus.UnlockPlus

val ContosoPatches = arrayOf(UnlockPlus)
```

#### `AppPatchInfo.kt`
```kotlin
import io.github.nexalloy.morphe.contoso.ContosoPatches

val appPatchConfigurations = listOf(
    // ...
    AppPatchInfo("Contoso", "com.contoso.app", ContosoPatches)
)

```

### Porting Upstream Fingerprints

Declare reusable fingerprints as named `object`s extending `Fingerprint`. Use the
constructor arguments for the method signature, class constraints, strings, and
instruction filters. Inside a patch, `SomeFingerprint.hookMethod(...)` resolves and
hooks the method. In a DexKit context, `SomeFingerprint()` and
`SomeFingerprint.run()` resolve a unique method. See [FingerprintCompat.kt](app/src/main/java/io/github/nexalloy/morphe/FingerprintCompat.kt).

- Use DEX descriptors such as `"Z"` and `"Landroid/view/View;"` for types.
  `parameters = emptyList()` matches a method with no parameters. A descriptor
  fragment ending in `;`, such as `"/MusicActivity;"`, matches a type suffix.
- Use `definingClass` for a known class or descriptor fragment, and
  `classFingerprint` to search the classes found by another fingerprint.
- Declare method modifiers with `accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)`.
  The constructor matches the exact set of flags.
- `strings = listOf("text")` matches full literals in any order. Use
  `strings = listOf(string("text", StringComparisonType.CONTAINS))` when
  porting a substring match.
- Use `custom = { ... }` for additional DexKit `MethodMatcher` criteria. If
  resolution needs multiple queries or further Kotlin filtering, expose the
  result with a direct finder such as `findMethodDirect` or `findClassDirect`.

#### Resource literals

Use `resourceLiteral` to resolve resource IDs when the fingerprint runs.
`MainHook` initializes the resource mapping before applying patches.

```kotlin
internal object ToolbarFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(resourceLiteral(ResourceType.ID, "toolbar")),
)
```

#### Instruction filters and method resolution

Use `methodCall`, `fieldAccess`, and `opcode` to describe instructions. The
filters match in declaration order and can specify locations such as
`MatchAfterImmediately()` relative to the previous match.

Use `@StrictQuery` when instruction order or distance should disambiguate
method candidates.

```kotlin
@StrictQuery
internal object MotionEventFingerprint : Fingerprint(
    definingClass = "/MusicActivity;",
    filters = listOf(
        methodCall(smali = "Landroid/view/MotionEvent;->getY()F"),
        methodCall(
            smali = "Landroid/view/View;->setTranslationY(F)V",
            location = MatchAfterWithin(8),
        ),
    ),
)
```

Call `MotionEventFingerprint()` to resolve the method. If multiple candidates
remain after signature filtering, `@StrictQuery` checks the instruction sequence
to narrow them down. A single candidate resolves directly.

Use `match()` or `instructionMatches` when you need the matched instructions or
need to validate the sequence even for a single candidate.

### Extension Modules

Shared extension code lives in [morphe-patches/extensions](morphe-patches/extensions) and is organized into modules by app.

## Unit Testing

Place original APKs in `app/binaries/` and prefix their filenames with the
package name. The fingerprint test runner supports YouTube
(`com.google.android.youtube`), YouTube Music
(`com.google.android.apps.youtube.music`), and Reddit
(`com.reddit.frontpage`). See [FingerprintsKtTest.kt](app/src/test/java/io/github/nexalloy/FingerprintsKtTest.kt).

```sh
./gradlew :app:testDebugUnitTest --tests io.github.nexalloy.FingerprintsKtTest
```

Check the matched descriptors in `app/build/reports/fingerprints/testDebugUnitTest.json`.
