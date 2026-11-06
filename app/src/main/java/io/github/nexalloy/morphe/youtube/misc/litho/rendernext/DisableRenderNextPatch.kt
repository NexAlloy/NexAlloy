package io.github.nexalloy.morphe.youtube.misc.litho.rendernext

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_29_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_47_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_35_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val disableRenderNextPatch = patch(
    description = "Disables RenderNext, so the app is always rendered with Litho.",
) {

    dependsOn(VersionCheck)

    // Do not provide ElementsServices to the section list, required to convert Litho components.
    insertLiteralOverride(45661418L)

    // Do not present elements with RenderNext, even if the server enables it.
    // An element is RenderNext if the enablement check of its proto
    // or the RenderNext field of its config is true.
    RenderNextEnablementCheckFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))

    val configParentField = ::RenderNextConfigParentField.field
    val configField = ::RenderNextConfigField.field
    ::RenderNextConfigFieldReadFingerprint.dexMethodList.forEach {
        it.hookMethod {
            before {
                it.args[0].let { configParentField.get(it) }
                    ?.let { configField.set(it, false) }
            }
        }
    }

    if (is_20_29_or_greater) {
        // Do not convert Litho components to RenderNext.
        RenderNextTemplateCheckFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    }

    if (is_21_35_or_greater) {
        // Do not open Home with the page fragment, which renders the whole page with RenderNext.
        PageFragmentRouteFingerprint.hookMethod(scopedHook(::pageFragmentRouteFlagMethod.method) {
            after {
                if (it.args[0] == 1073830434) it.result = false
            }
        })
    }

    if (is_20_47_or_greater) {
        // TOD Safeguard: always present elements with Litho, even if they are RenderNext elements.
    }
}
