package io.github.nexalloy.morphe.youtube.misc.fix.accessibility

import android.os.Build
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_51_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_23_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_30_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_42_or_greater
import io.github.nexalloy.patch

internal val fixAccessibilityPatch = patch {
    dependsOn(VersionCheck)
    if (!is_21_23_or_greater) {
        CaptionTextViewConstructorFingerprint.hookMethod {
            after { (it.thisObject as View).accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        }
    }
    if (is_20_51_or_greater && !is_21_42_or_greater) {
        if (is_21_30_or_greater) {
            TouchExplorationHoverEventFingerprint.hookMethod(returnConstant(false))
        } else if (Build.VERSION.SDK_INT >= 29) {
            // Older Android versions have no inherited hover forwarding method.
            val delegateClass = TouchDelegateConstructorFingerprint.declaredClass
            TouchDelegate::class.java.getMethod("onTouchExplorationHoverEvent", MotionEvent::class.java).hookMethod {
                before { if (delegateClass.isInstance(it.thisObject)) it.result = false }
            }
        }
    }
}
