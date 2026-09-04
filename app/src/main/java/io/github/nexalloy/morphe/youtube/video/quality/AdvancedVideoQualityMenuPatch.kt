package io.github.nexalloy.morphe.youtube.video.quality

import app.morphe.extension.youtube.patches.components.AdvancedVideoQualityMenuFilter
import app.morphe.extension.youtube.patches.playback.quality.AdvancedVideoQualityMenuPatch
import io.github.nexalloy.createProxy
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.recyclerviewtree.addRecyclerViewTreeHook
import io.github.nexalloy.morphe.youtube.misc.recyclerviewtree.recyclerViewTreeHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private object SkipQuickQualityMenu : RuntimeException()

val AdvancedVideoQualityMenu = patch {
    dependsOn(
        LithoFilter,
        recyclerViewTreeHook,
    )

    settingsMenuVideoQualityGroup.add(
        SwitchPreference("morphe_advanced_video_quality_menu", summary = true)
    )

    // region Patch for the old type of the video quality menu.
    // Used for regular videos when spoofing to old app version,
    // and for the Shorts quality flyout on newer app versions.
    //
    // Upstream injects the conditional return immediately before
    // getSupportFragmentManager().  A method-level replacement would run even on
    // control-flow paths that never reach that call and would skip earlier host side effects.
    // Throw a private sentinel at the exact call site and swallow it at the outer hook to
    // reproduce a mid-method return-void without changing unrelated execution paths.
    val fragmentManagerMember = ::ShowVideoQualityQuickMenuFragmentManager.member
    ::ShowVideoQualityQuickMenuFingerprint.dexMethodList.forEach { outerMethod ->
        outerMethod.hookMethod(scopedHook(fragmentManagerMember) {
            before { innerParam ->
                if (innerDepth != 0) return@before
                if (AdvancedVideoQualityMenuPatch.showShortsQualityMenu()) {
                    innerParam.throwable = SkipQuickQualityMenu
                }
            }
        })
        outerMethod.hookMethod {
            after { outerParam ->
                if (outerParam.throwable === SkipQuickQualityMenu) {
                    outerParam.result = null
                }
            }
        }
    }

    ShortsQualityConstructorFingerprint.hookMethod {
        val showQualityMethod = ShortsQualityMenuFingerprint.method
        after {
            // Logger.printDebug { "ShortsQualityConstructor" }
            AdvancedVideoQualityMenuPatch.initialize(
                it.thisObject.createProxy { impl ->
                    AdvancedVideoQualityMenuPatch.ShortsQualityMenuInterface {
                        // Logger.printDebug { "Perform ShortsQualityMenu " }
                        showQualityMethod(impl.get(), true)
                    }
                }
            )
        }
    }

    // region Patch for the new type of the video quality menu.
    addRecyclerViewTreeHook.add(AdvancedVideoQualityMenuPatch::onFlyoutMenuCreate)
    // Required to check if the video quality menu is currently shown in order to click on the "Advanced" item.
    addLithoFilter(AdvancedVideoQualityMenuFilter())
    // endregion
}
