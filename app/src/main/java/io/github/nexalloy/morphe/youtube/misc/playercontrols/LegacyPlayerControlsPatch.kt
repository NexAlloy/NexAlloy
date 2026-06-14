package io.github.nexalloy.morphe.youtube.misc.playercontrols

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.RelativeLayout
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.LegacyPlayerControlsPatch
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IHookCallback
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.LiteralFilter
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.litho.filter.featureFlagCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_28_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_30_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

class ControlInitializer(
    val id: Int,
    @JvmField val initializeButton: (controlsView: ViewGroup) -> Unit,
    // visibilityCheckCalls
    @JvmField val setVisibility: (Boolean, Boolean) -> Unit,
    @JvmField val setVisibilityImmediate: (Boolean) -> Unit,
    // Patch works without this hook, but it is needed to use the correct fade out animation
    // duration when tapping the overlay to dismiss.
    @JvmField val setVisibilityNegatedImmediate: () -> Unit
)

private data class TopControlLayout(
    val layout: Int,
    val startViewId: Int,
    val endViewId: Int
)

private val topControlLayouts = mutableListOf<TopControlLayout>()
private val bottomControlLayouts = mutableListOf<Int>()
private val topControls = mutableListOf<ControlInitializer>()
private val bottomControls = mutableListOf<ControlInitializer>()

@JvmField
var visibilityImmediateCallbacksExistModified = false

fun onFullscreenButtonVisibilityChanged(isVisible: Boolean) {
    topControls.forEach { it.setVisibilityImmediate(isVisible) }
    bottomControls.forEach { it.setVisibilityImmediate(isVisible) }
//    Logger.printDebug { ("setVisibilityImmediate($isVisible)") }
}

fun addTopControl(layout: Int, startViewId: Int, endViewId: Int) {
    topControlLayouts.add(TopControlLayout(layout, startViewId, endViewId))
}

fun addLegacyBottomControl(layout: Int) {
    bottomControlLayouts.add(layout)
}

fun initializeTopControl(control: ControlInitializer) {
    topControls.add(control)
    injectVisibilityCheckCall()
}

fun initializeLegacyBottomControl(control: ControlInitializer) {
    bottomControls.add(control)
    injectVisibilityCheckCall()
}

private fun injectVisibilityCheckCall() {
    if (!visibilityImmediateCallbacksExistModified) {
        visibilityImmediateCallbacksExistModified = true
    }
}

private fun onTopContainerInflate(viewStub: ViewStub, root: ViewGroup) {
    topControlLayouts.forEach { control ->
        viewStub.layoutInflater.inflate(control.layout, root, true)
    }

    var insertViewId = ResourceUtils.getIdIdentifier("player_video_heading")
    val anchorViewId = ResourceUtils.getIdIdentifier("music_app_deeplink_button")

    for (control in topControlLayouts) {
        val insertView = root.findViewById<View>(insertViewId) ?: continue
        val endView = root.findViewById<View>(control.endViewId) ?: continue

        (insertView.layoutParams as RelativeLayout.LayoutParams).addRule(
            RelativeLayout.START_OF, control.startViewId
        )

        (endView.layoutParams as RelativeLayout.LayoutParams).addRule(
            RelativeLayout.START_OF, anchorViewId
        )

        insertViewId = control.endViewId
    }

    topControls.forEach { control ->
        control.initializeButton(root)
    }
}

private fun onBottomContainerInflate(viewStub: ViewStub, root: ViewGroup) {
    if (LegacyPlayerControlsPatch.usePlayerBottomControlsExploderLayout(/*ignored*/ true)) {
        return
    }

    bottomControlLayouts.forEach { layout ->
        viewStub.layoutInflater.inflate(layout, root, true)
    }
    bottomControls.forEach { control ->
        control.initializeButton(root)
    }
}

val LegacyPlayerControls = patch(
    description = "Manages the code for the player controls of the YouTube player.",
) {
    dependsOn(
        PlayerControlsOverlayVisibility,
        VersionCheck,
    )

    if (is_20_31_or_greater) {
        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("morphe_restore_old_player_buttons", summary = true)
        )
    }

    DexMethod("Landroid/view/ViewStub;->inflate()Landroid/view/View;").hookMethod {
        after {
            val viewStub = it.thisObject as ViewStub
            val viewStubName = Utils.getContext().resources.getResourceName(viewStub.id)
//            Logger.printDebug { "ViewStub->inflate()" + viewStubName }

            when {
                viewStubName.endsWith("bottom_ui_container_stub") -> {
                    onBottomContainerInflate(viewStub, it.result as ViewGroup)
                }

                viewStubName.endsWith("controls_layout_stub") -> {
                    onTopContainerInflate(viewStub, it.result as ViewGroup)
                }

                else -> return@after
            }
//            Logger.printDebug { "inject into $viewStubName" }
        }
    }

    initInjectVisibilityCheckCall()

    val youtube_controls_bottom_ui_container =
        ResourceUtils.getIdIdentifier("youtube_controls_bottom_ui_container")

    val onLayoutHook: HookDsl<IHookCallback>.() -> Unit = {
        after {
            val controlsView = it.thisObject as ViewGroup
            if (controlsView.id != youtube_controls_bottom_ui_container) return@after

            val fullscreenButton =
                Utils.getChildViewByResourceName<View>(controlsView, "fullscreen_button")
            var rightButton = fullscreenButton

            for (bottomControl in bottomControls) {
                val leftButton = controlsView.findViewById<View>(bottomControl.id) ?: continue
                if (leftButton.visibility == View.GONE) continue
                // put this button to the left
                leftButton.x = rightButton.x - leftButton.width
                leftButton.y = rightButton.y
                leftButton.layoutParams = leftButton.layoutParams.apply {
                    height = fullscreenButton.height
                }
                rightButton = leftButton
            }
        }
    }

    DexMethod("Landroid/support/constraint/ConstraintLayout;->onLayout(ZIIII)V").hookMethod(onLayoutHook)
    DexMethod("Landroidx/constraintlayout/widget/ConstraintLayout;->onLayout(ZIIII)V").hookMethod(onLayoutHook)
}

private fun PatchExecutor.initInjectVisibilityCheckCall() {
    ControlsOverlayVisibilityFingerprint.hookMethod {
        before { param ->
            bottomControls.forEach {
                it.setVisibility(param.args[0] as Boolean, param.args[1] as Boolean)
            }
//            Logger.printDebug { "setVisibility(visible: ${param.args[0]}, animated: ${param.args[1]})" }
        }
    }

    // Hook the fullscreen close button. Used to fix visibility
    // when seeking and other situations.
    OverlayViewInflateFingerprint.hookMethod(scopedHook(DexMethod("Landroid/view/View;->findViewById(I)Landroid/view/View;").toMember()) {
        val fullscreenButtonId = fullscreen_button_id
        after {
            if (it.args[0] == fullscreenButtonId) {
                LegacyPlayerControlsPatch.setFullscreenCloseButton(it.result as ImageView)
            }
        }
    })

    //
    MotionEventFingerprint.hookMethod(scopedHook(DexMethod("Landroid/view/View;->setTranslationY(F)V").toMethod()) {
        after {
            // FIXME Animation lags behind
            bottomControls.forEach { it.setVisibilityNegatedImmediate() }
//            Logger.printDebug { "setVisibilityNegatedImmediate()" }
        }
    })

    fun overrideExploderLayout(fingerprint: Fingerprint) {
        val featureId = (fingerprint.filters!![0] as LiteralFilter).literal()
        ::featureFlagCheck.hookMethod {
            after {
                if (it.args[0] == featureId)
                    it.result =
                        LegacyPlayerControlsPatch.usePlayerBottomControlsExploderLayout(it.result as Boolean)
            }
        }
    }

    // A/B test for a slightly different bottom overlay controls,
    // that uses layout file youtube_video_exploder_controls_bottom_ui_container.xml
    // The change to support this is simple and only requires adding buttons to both layout files,
    // but for now force this different layout off since it's still an experimental test.

    overrideExploderLayout(PlayerBottomControlsExploderFeatureFlagFingerprint)

    // Turn off a/b tests of ugly player buttons that don't match the style of custom player buttons.
    overrideExploderLayout(PlayerControlsFullscreenLargeButtonsFeatureFlagFingerprint)

    if (is_20_28_or_greater) {
        overrideExploderLayout(PlayerControlsLargeOverlayButtonsFeatureFlagFingerprint)
    }

    if (is_20_30_or_greater) {
        overrideExploderLayout(PlayerControlsButtonStrokeFeatureFlagFingerprint)
    }

    // TODO Clear bottom gradient.
}