package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.morphe.extension.shared.settings.preference.SeekBarPreference
import app.morphe.extension.youtube.innertube.BrowseResponseOuterClass.BrowseTab
import app.morphe.extension.youtube.innertube.BrowseResponseOuterClass.TabRenderer
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.ButtonRenderer
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.PivotBarItemBytes
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.PivotBarItemRenderer
import app.morphe.extension.youtube.patches.ClassicSubscriptionsLayoutPatch
import app.morphe.extension.youtube.patches.NavigationBarPatch
import app.morphe.extension.youtube.settings.preference.YouTubeGlassNavigationBarPreference
import app.morphe.extension.youtube.settings.preference.NavigationButtonsOrderPreference
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.MessageLite
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IScopedHookCallback
import io.github.nexalloy.callMethod
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.interaction.downloads.GeneratedRegistryFingerprint
import io.github.nexalloy.morphe.youtube.interaction.downloads.ProtobufClassParseByteArrayWithRegistryFingerprint
import io.github.nexalloy.morphe.youtube.layout.hide.shorts.ShortsBottomBarContainerFingerprint
import io.github.nexalloy.morphe.youtube.misc.contexthook.Endpoint
import io.github.nexalloy.morphe.youtube.misc.contexthook.addOSNameHook
import io.github.nexalloy.morphe.youtube.misc.contexthook.clientContextHookPatch
import io.github.nexalloy.morphe.youtube.misc.fix.padding.BrowseListBottomPaddingFingerprint
import io.github.nexalloy.morphe.youtube.misc.fix.padding.browseListSetPaddingMethod
import io.github.nexalloy.morphe.youtube.misc.fix.padding.fixPagePaddingPatch
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarHook
import io.github.nexalloy.morphe.youtube.misc.navigation.addBottomBarContainerHook
import io.github.nexalloy.morphe.youtube.misc.navigation.hookNavigationButtonCreated
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_23_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_35_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.lang.reflect.Field
import java.lang.reflect.Method
import org.luckypray.dexkit.wrap.DexMethod

private lateinit var navigationBarPaddingField: Field
private lateinit var navigationBarUpdateHeightMethod: Method
private lateinit var pivotBarRendererClass: Class<*>
private lateinit var pivotBarMessageField: Field

fun isPivotBar(view: View?): Boolean = navigationBarPaddingField.declaringClass.isInstance(view)

fun getNavigationBarSystemButtonsRoom(view: View): Int = navigationBarPaddingField.getInt(view)

fun setNavigationBarSystemButtonsRoom(view: View, room: Int) {
    navigationBarPaddingField.setInt(view, room)
    navigationBarUpdateHeightMethod.invoke(view)
}

fun getBrowseTabRenderer(tab: Any): TabRenderer =
    BrowseTab.parseFrom(tab.callMethod("toByteArray") as ByteArray).tabRenderer

fun getPivotBarItemProto(item: Any): PivotBarItemBytes =
    PivotBarItemBytes.parseFrom(item.callMethod("toByteArray") as ByteArray)

fun getPivotBarItemRenderer(item: Any): PivotBarItemRenderer? {
    val message = pivotBarMessageField.get(item)
    if (pivotBarRendererClass.isInstance(message)) {
        return PivotBarItemRenderer.parseFrom(message!!.callMethod("toByteArray") as ByteArray)
    }
    return null
}

val NavigationBar = patch(
    name = "Navigation bar",
    description = "Adds options to hide and change the bottom navigation bar (such as the Shorts button) "
            + "and the upper navigation toolbar.",
) {
    dependsOn(
        fixPagePaddingPatch,
        NavigationBarHook,
        VersionCheck,
        clientContextHookPatch,
    )

    val navPreferences = mutableSetOf(
        SwitchPreference("morphe_hide_home_button"),
        SwitchPreference("morphe_hide_shorts_button"),
        SwitchPreference("morphe_hide_create_button"),
        SwitchPreference("morphe_hide_subscriptions_button"),
        SwitchPreference("morphe_hide_notifications_button"),
//        SwitchPreference("morphe_show_search_button"),
//        SwitchPreference("morphe_show_watch_later_button"),
//        SwitchPreference("morphe_show_settings_button"),
//        SwitchPreference("morphe_show_settings_button_type", summary = true),
        // TODO Custom buttons: item factory, selected Watch later icon and ordering adapters.
//        NonInteractivePreference(
//            "morphe_navigation_buttons_order",
//            tag = NavigationButtonsOrderPreference::class.java,
//            selectable = true
//        ),
        SwitchPreference("morphe_swap_create_with_notifications_button", summary = true),
//        SwitchPreference("morphe_hide_navigation_bar"),
//        SwitchPreference("morphe_narrow_navigation_buttons", summary = true),
        SwitchPreference("morphe_hide_navigation_button_labels"),
        SwitchPreference("morphe_disable_icon_only_navigation_buttons", summary = true),
    )

    if (is_21_35_or_greater) {
        navPreferences += SwitchPreference("morphe_disable_toolbar_navigation_tabs", summary = true)
    }

    navPreferences += setOf(
        SwitchPreference("morphe_hide_navigation_new_content_dot"),
        SwitchPreference("morphe_navigation_bar_animations", summary = true),
        // TODO Glass style: measurement, translation, padding and tint bridges.
//        ListPreference("morphe_navigation_bar_style"),
//        SwitchPreference("morphe_navigation_bar_glass", summary = true),
//        NonInteractivePreference(
//            key = "morphe_navigation_bar_glass_opacity",
//            tag = SeekBarPreference::class.java
//        ),
//        NonInteractivePreference(
//            key = "morphe_navigation_bar_glass_height",
//            titleKey = "morphe_navigation_bar_glass_size_title",
//            summaryKey = "morphe_navigation_bar_glass_size_summary",
//            tag = YouTubeGlassNavigationBarPreference::class.java,
//            selectable = true
//        )
    )

    if (is_20_31_or_greater) {
        navPreferences += SwitchPreference("morphe_disable_auto_hide_navigation_bar", summary = true)
    }

    PreferenceScreen.GENERAL.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_navigation_bar_screen",
            sorting = Sorting.UNSORTED,
            preferences = navPreferences
        )
    )

    // Some pages have their own toolbar, whose buttons are created with their renderer.
    // NexAlloy: the extension receives module protos. Keep host extensions as unknown fields
    // here; the host registry is applied when converting the result back below.
    NavigationBarPatch::class.java.getDeclaredMethod(
        "parseWithRegistry", MessageLite::class.java, ByteArray::class.java, ExtensionRegistryLite::class.java
    ).hookMethod {
        before {
            it.result = (it.args[0] as MessageLite).parserForType.parseFrom(it.args[1] as ByteArray)
        }
    }
    val parseFrom = ProtobufClassParseByteArrayWithRegistryFingerprint.method
    val registry = GeneratedRegistryFingerprint.method.invoke(null)
    val toolbarConstructor = ::toolbarButtonConstructor.constructor
    val countIdsIndex = toolbarConstructor.parameterTypes.indexOfFirst { it == List::class.java }
    check(countIdsIndex > 0)
    val rendererIndex = countIdsIndex - 1
    toolbarConstructor.hookMethod {
        before {
            val renderer = it.args[rendererIndex] ?: return@before
            val moduleRenderer = ButtonRenderer.parseFrom(renderer.callMethod("toByteArray") as ByteArray)
            val updated = NavigationBarPatch.getToolbarButtonRenderer(moduleRenderer)
            if (updated !== moduleRenderer) {
                it.args[rendererIndex] = parseFrom.invoke(null, renderer, updated.toByteArray(), registry)
            }
            @Suppress("UNCHECKED_CAST")
            val countIds = it.args[countIdsIndex] as List<String>
            it.args[countIdsIndex] = NavigationBarPatch.getToolbarButtonCountIds(updated, countIds)
        }
    }

    // Swap create with notifications button.
    addOSNameHook(
        Endpoint.GUIDE,
        NavigationBarPatch::swapCreateWithNotificationButton
    )
    setExtensionIsPatchIncluded(NavigationBarPatch::class.java)

    // Hide navigation button labels.
    CreatePivotBarFingerprint.hookMethod(scopedHook(DexMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V").toMethod()) {
        before { param ->
            NavigationBarPatch.hideNavigationButtonLabels(param.thisObject as TextView)
        }
    })

    // Hook navigation button created, in order to hide them.
    hookNavigationButtonCreated(NavigationBarPatch::navigationTabCreated)

    // TODO Hide navigation bar

    // region Translucent navigation bar

    // Turning on the feature flags of the translucent navigation bar, and the fixes for what
    // they break. Every fix checks that the flags are turned on by this patch, so the fixes do
    // nothing if the flags are left to the account, as with the default navigation bar style.
    insertLiteralOverride(45630927L, NavigationBarPatch::useTranslucentNavigationBarBackground)
    insertLiteralOverride(45632194L, NavigationBarPatch::useTranslucentNavigationBar)
    insertLiteralOverride(45631257L, NavigationBarPatch::useFeedBehindNavigationBar)
    insertLiteralOverride(45633329L, NavigationBarPatch::useTranslucentNavigationBarStyle)

    // Transparent and opaque styles omit the blur, which can crash Android 12 and 13.
    if (Build.VERSION.SDK_INT >= 29) {
        val canvases = listOf(
            Canvas::class.java,
            Class.forName("android.graphics.RecordingCanvas")
        )
        canvases.map { it.getMethod("isHardwareAccelerated") }.distinct().forEach { hardwareAccelerated ->
            NavigationBarBlurDrawFingerprint.hookMethod(scopedHook(hardwareAccelerated) {
                after {
                    it.result = NavigationBarPatch.useNavigationBarBlur(it.result as Boolean)
                }
            })
        }
    }

    // With the keyboard open, the bottom inset also holds the keyboard, so the bar covering
    // the system navigation buttons grows as tall as the keyboard and covers the page.
    // The padding is first stored in a field of the bar, and the same register is then passed
    // to setPadding. Some targets set it in two places.
    val paddingField = ::navigationBarBottomPaddingField.field.also { navigationBarPaddingField = it }
    navigationBarUpdateHeightMethod = ::navigationBarRelayoutMethods.dexMethodList.single().toMethod()
    val updateHeight: HookDsl<IScopedHookCallback>.() -> Unit = {
        before {
            if (innerDepth == 0 && paddingField.declaringClass.isInstance(it.thisObject)) {
                val view = it.thisObject as View
                paddingField.setInt(view, NavigationBarPatch.getNavigationBarBottomPadding(view, paddingField.getInt(view)))
            }
        }
    }
    val updatePadding: HookDsl<IScopedHookCallback>.() -> Unit = {
        before {
            if (innerDepth == 0 && paddingField.declaringClass.isInstance(it.thisObject)) {
                it.args[3] = NavigationBarPatch.getNavigationBarBottomPadding(it.thisObject as View, it.args[3] as Int)
            }
        }
    }
    val setPadding = paddingField.declaringClass.getMethod(
        "setPadding", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
    )
    val paddingHooks = (::navigationBarRelayoutMethods.dexMethodList.map {
        it.toMethod() to updateHeight
    } + (setPadding to updatePadding)).toTypedArray()
    ::navigationBarBottomPaddingMethods.dexMethodList.forEach {
        it.toMethod().hookMethod(scopedHook(*paddingHooks))
    }

    // With the bar covering the system navigation buttons, a flag adds the navigation bar height
    // below the bar fixed at the bottom of some pages, such as the reply bar of the comment
    // replies, so an empty band is left between them.
    insertLiteralOverride(45752241L, NavigationBarPatch::useFixedBottomBarRoom)

    // The slim status bar ("No connection", "Back online") is laid out below the bar covering
    // the system navigation buttons, so the buttons cover it. The room for the buttons is moved
    // below it while it is shown.
    addBottomBarContainerHook(NavigationBarPatch::watchSlimStatusBar)

    // When a browse page is laid out behind the navigation bar, its list is set to draw into
    // its padding, and the padding is set later, once the window insets are known. If the list
    // is refreshed in between, as when coming back from the Shorts player, it's set to clip to
    // its padding again, so the bottom padding is left as an empty band between the feed and
    // the navigation bar. The list is set to draw into its padding again with the padding.
    BrowseListBottomPaddingFingerprint.hookMethod(scopedHook(::browseListSetPaddingMethod.method) {
        after {
            if (innerDepth == 0 && NavigationBarPatch.isTranslucentNavigationBarBackgroundForced()) {
                (it.thisObject as ViewGroup).clipToPadding = false
            }
        }
    })

    // endregion

    // With the translucent navigation bar, the navigation bar makes room for the system
    // navigation buttons. If the navigation bar is hidden in the Shorts player, nothing makes
    // that room, so the Shorts player makes it itself. The first parameter is the layout
    // of the Shorts player fragment.
    ShortsBottomBarContainerFingerprint.hookMethod {
        before { NavigationBarPatch.setShortsPlayerBottomInset(it.args[0] as View) }
    }
    // Replace the height at the constructor call, after other hooks may have changed getHeight().
    val reserveRoom = ::shortsBottomRoomConstructor.constructor
    val heightParameter = reserveRoom.parameterTypes.toList().zipWithNext().indexOfFirst { (view, height) ->
        view == View::class.java && height == Int::class.javaPrimitiveType
    } + 1
    ShortsBottomBarContainerFingerprint.hookMethod(scopedHook(reserveRoom) {
        before {
            it.args[heightParameter] = NavigationBarPatch.getShortsPlayerBottomRoom(it.args[heightParameter] as Int)
        }
    })

    // TODO Paint over the translucent status bar and navigation bar

    // Animated navigation tabs.
    insertLiteralOverride(45680008L, NavigationBarPatch::useAnimatedNavigationButtons)

    // TODO Narrow navigation buttons

    // disableAutoHidingNavigationBar

    if (is_20_31_or_greater) {
        buildList {
            add(AutoHideNavigationBarOnFeedScrollingFingerprint)
            if (is_21_23_or_greater) add(AutoHideNavigationBarOnDismissMiniplayerFingerprint)
        }.forEach {
            it.hookMethod {
                before { param ->
                    if (NavigationBarPatch.disableAutoHidingNavigationBar()) {
                        param.result = null
                    }
                }
            }
        }
    }

    // TODO Navigation search and settings button
    // TODO The translucent theme hands the window a transparent color for the system navigation
    // buttons, so an opaque one is swapped in there instead of turning off the feature flag.
    // Disable the A/B layout with Subscriptions as a tab of Home.
    if (is_21_35_or_greater) {
        pivotBarRendererClass = ::pivotBarItemRendererClass.clazz
        pivotBarMessageField = ::pivotBarItemMessageField.field

        // Add back the Subscriptions button, if the server moved it to a tab of Home.
        // Use the host's offline navigation factory so the browse endpoint extension is set.
        val itemFactory = PivotBarItemProtoFactoryFingerprint.method
        val iconLookup = ::pivotBarIconTypeLookupMethod.method
        val renderItem = PivotBarRendererFingerprint.method
        val listBuilder = ::pivotBarRendererListBuilderMethod.method
        PivotBarRendererListFingerprint.hookMethod(scopedHook(::pivotBarRendererListMethod.method) {
            after {
                if (innerDepth != 0 || it.args[0] !== outerParam.args[0]) return@after
                @Suppress("UNCHECKED_CAST")
                val items = it.result as? List<Any> ?: return@after
                if (ClassicSubscriptionsLayoutPatch.needsSubscriptionsPivotBarItem(items)) {
                    val icon = iconLookup.invoke(null, ClassicSubscriptionsLayoutPatch.getSubscriptionsIconType())
                    val proto = itemFactory.invoke(
                        null, "FEsubscriptions", ClassicSubscriptionsLayoutPatch.getSubscriptionsLabel(), icon, false
                    )
                    val optional = renderItem.invoke(null, proto)!!
                    val item = optional.callMethod("orElse", null)
                    val updated = ClassicSubscriptionsLayoutPatch.addSubscriptionsPivotBarItem(items, item)
                    // NexAlloy: the field requires the host's immutable list, not an ArrayList.
                    if (updated !== items) it.result = listBuilder.invoke(null, updated)
                }
            }
        })

        // Show only the selected tab of the Home and Subscriptions feeds.
        // Keep the original tab objects so their proto extensions are retained.
        BrowseResponseTabsFingerprint.hookMethod(scopedHook(::browseResponseTabsStreamMethod.method) {
            before {
                if (innerDepth == 0) {
                    @Suppress("UNCHECKED_CAST")
                    val tabs = it.args[0] as? List<Any> ?: return@before
                    it.args[0] = ClassicSubscriptionsLayoutPatch.filterBrowseTabs(tabs)
                }
            }
        })

        // Navigation bar used before the guide response is loaded.
        insertLiteralOverride(45780222L, ClassicSubscriptionsLayoutPatch::hideOfflineSubscriptionsButton)
    }

    // Disable the A/B layout with navigation buttons without labels.
    val itemDefault = ::pivotBarItemDefaultInstance.field.get(null)
    PivotBarRendererFingerprint.hookMethod {
        before {
            ClassicSubscriptionsLayoutPatch.convertIconOnlyPivotBarItem(it.args[0])?.let { bytes ->
                // The endpoints are extensions, which are lost if parsed without the registry.
                it.args[0] = parseFrom.invoke(null, itemDefault, bytes, registry)
            }
        }
    }

//    val toolbarPreferences = mutableSetOf(
//        SwitchPreference("morphe_hide_toolbar_chat_button"),
//        SwitchPreference("morphe_hide_toolbar_create_button", titleKey = "morphe_hide_create_button_title"),
//        SwitchPreference("morphe_hide_toolbar_microphone_button"),
//        SwitchPreference("morphe_hide_toolbar_notification_button", titleKey = "morphe_hide_notifications_button_title"),
//        SwitchPreference("morphe_hide_toolbar_search_button"),
//        SwitchPreference("morphe_show_toolbar_settings_button", titleKey = "morphe_show_settings_button_title"),
//        SwitchPreference("morphe_show_toolbar_settings_button_type", titleKey = "morphe_show_settings_button_type_title", summary = true),
//        NonInteractivePreference(
//            "morphe_toolbar_buttons_order",
//            tag = NavigationButtonsOrderPreference::class.java,
//            selectable = true
//        )
//    )
//
//    PreferenceScreen.GENERAL.addPreferences(
//        PreferenceScreenPreference(
//            key = "morphe_toolbar_screen",
//            sorting = Sorting.UNSORTED,
//            preferences = toolbarPreferences
//        )
//    )
    // TODO upper navigation toolbar
}
