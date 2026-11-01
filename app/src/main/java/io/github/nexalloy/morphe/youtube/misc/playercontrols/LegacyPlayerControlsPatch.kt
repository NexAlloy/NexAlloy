package io.github.nexalloy.morphe.youtube.misc.playercontrols

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.RelativeLayout
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.LegacyPlayerControlsPatch
import app.morphe.extension.youtube.patches.spans.MetapanelTextColorFilter
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IHookCallback
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.shared.layout.theme.lithoColorHookPatch
import io.github.nexalloy.morphe.shared.layout.theme.lithoDrawableColorHook
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.spans.addSpanFilter
import io.github.nexalloy.morphe.shared.misc.spans.inclusiveSpanPatch
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_28_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_30_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_04_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_05_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_08_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_11_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_15_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_29_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_30_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_36_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_38_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_41_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

class ControlInitializer(
    val id: Int,
    @JvmField val initializeButton: (controlsView: ViewGroup) -> Unit,
)

private data class TopControlLayout(
    val layout: Int, val startViewId: Int, val endViewId: Int
)

private val topControlLayouts = mutableListOf<TopControlLayout>()
private val bottomControlLayouts = mutableListOf<Int>()
private val topControls = mutableListOf<ControlInitializer>()
private val bottomControls = mutableListOf<ControlInitializer>()

fun addTopControl(layout: Int, startViewId: Int, endViewId: Int) {
    topControlLayouts.add(TopControlLayout(layout, startViewId, endViewId))
}

fun addLegacyBottomControl(layout: Int) {
    bottomControlLayouts.add(layout)
}

private var newPlayerControlsOverride = false

internal fun PatchExecutor.disableNewPlayerControlsFeatureFlag() {
    if (newPlayerControlsOverride) return
    newPlayerControlsOverride = true

    if (is_20_31_or_greater) {
        insertLiteralOverride(45698813, LegacyPlayerControlsPatch::forcePlayerSeekbar)
    }

    if (is_21_04_or_greater && !is_21_41_or_greater) {
        insertLiteralOverride(45752335L, LegacyPlayerControlsPatch::disableNewPlayerOverlays)

        if (is_21_36_or_greater) {
            // With the flag off the autonav end screen is laid out over the miniplayer
            // as if it was the full player, so it is given the value the app has.
            val flagMethod = NewPlayerOverlaysFeatureFlagFingerprint.member
            ::autonavEndscreenFlagCallers.dexMethodList.forEach { method ->
                method.hookMethod(scopedHook(flagMethod) {
                    after {
                        it.result = LegacyPlayerControlsPatch.getOriginalNewPlayerOverlays(it.result as Boolean)
                    }
                })
            }
        }
    }
}

fun initializeTopControl(control: ControlInitializer) {
    topControls.add(control)
}

fun initializeLegacyBottomControl(control: ControlInitializer) {
    bottomControls.add(control)
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
        inclusiveSpanPatch,
        lithoColorHookPatch { is_21_30_or_greater },
    )

    if (is_20_31_or_greater && !is_21_36_or_greater) {
        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("morphe_restore_old_player_buttons", summary = true)
        )
    }

    if (is_21_36_or_greater) {
        disableNewPlayerControlsFeatureFlag()
    }

    if (is_21_08_or_greater) {
        // The text of the video metadata panel can have the dark theme colors
        // of the elevated player, when the elevated player is turned off.
        addSpanFilter(MetapanelTextColorFilter())

        // The icons can also have the dark theme tint. The tint is read when the image is loaded,
        // without the conversion context, so the image component registers its images
        // with their conversion context when it prepares them.
        ::imageComponentPrepareMethod.hookMethod {
            val contextField = ::imageComponentConversionContextField.field
            val imageFields = ::imageComponentImageFields.fieldList
            before {
                val context = contextField.get(it.thisObject) ?: return@before
                imageFields.forEach { field ->
                    LegacyPlayerControlsPatch.registerImage(ConversionContext(context), field.get(it.thisObject))
                }
            }
        }

        ::imageTintColorMethod.hookMethod {
            before { LegacyPlayerControlsPatch.setTintImage(it.args[0]) }
            after { it.result = LegacyPlayerControlsPatch.getImageTintColor(it.result as Int) }
        }

        // The page indicator of the comments button can also have the dark theme colors.
        lithoDrawableColorHook(LegacyPlayerControlsPatch::getBackgroundColor)

        // The elevated player shows the video metadata without a background over the
        // ambient mode colors, so the light theme text can not be read.
        // The metadata panel also has its own forced dark and light themes, which do not depend
        // on the elevated player. With the forced dark theme and the elevated player off, the
        // white text is drawn on the light background of the watch page, so turn all of them off
        // and the metadata panel uses the app theme.
        insertLiteralOverride(45757309L, LegacyPlayerControlsPatch::useElevatedPlayer)
        if (is_21_29_or_greater) insertLiteralOverride(45813114L, false)
        if (is_21_38_or_greater) insertLiteralOverride(45840190L, false)
    }

    // Comments teaser A/B test: shows a small comments card below the player time bar,
    // which can be too narrow to read in portrait.
    // It also crashes the app on startup with the old player icons, so always turn it off.
    if (is_21_15_or_greater) insertLiteralOverride(45771730L, false)
    // Watch feed resize A/B test: every scroll event of the watch next feed
    // resizes the player and relayouts the watch page, and scrolling lags.
    // It also interferes with the old player icons, so always turn it off.
    if (is_21_11_or_greater) insertLiteralOverride(45763727L, false)
    fun overrideExploderLayout(id: Long) = insertLiteralOverride(
        id, LegacyPlayerControlsPatch::usePlayerBottomControlsExploderLayout
    )

    // A/B test for a slightly different bottom overlay controls,
    // that uses layout file youtube_video_exploder_controls_bottom_ui_container.xml
    // The change to support this is simple and only requires adding buttons to both layout files,
    // but for now force this different layout off since it's still an experimental test.
    overrideExploderLayout(45643739L)

    // Turn off a/b tests of ugly player buttons that don't match the style of custom player buttons.
    if (!is_21_36_or_greater) {
        overrideExploderLayout(45686474L)
    }

    if (is_20_28_or_greater) {
        overrideExploderLayout(45709810L)
    }

    if (is_20_30_or_greater) {
        overrideExploderLayout(45713296)
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

    DexMethod("Landroid/support/constraint/ConstraintLayout;->onLayout(ZIIII)V").hookMethod(
        onLayoutHook
    )
    DexMethod("Landroidx/constraintlayout/widget/ConstraintLayout;->onLayout(ZIIII)V").hookMethod(
        onLayoutHook
    )

    if (is_21_05_or_greater) {
        insertLiteralOverride(45750838L, LegacyPlayerControlsPatch::useModernPlayerTopControls)
    }

    // TODO Addon
}
