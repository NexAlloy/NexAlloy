package io.github.nexalloy.morphe.youtube.misc.navigation

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import app.morphe.extension.shared.ResourceType
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.VersionCheckPatch
import app.morphe.extension.youtube.shared.NavigationBar
import io.github.nexalloy.createProxy
import io.github.nexalloy.enumValueOf
import io.github.nexalloy.morphe.youtube.shared.mainActivityOnBackPressedFingerprint
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

fun onNavigationTabCreated(button: NavigationBar.NavigationButton, tabView: View) {
    hookNavigationButtonCreated.forEach { it(button, tabView) }
}

val hookNavigationButtonCreated: MutableList<(NavigationBar.NavigationButton, View) -> Unit> =
    mutableListOf()

val NavigationBarHook = patch(
    description = "Hooks the active navigation or search bar.",
) {

    // Hook the current navigation bar enum value. Note, the 'You' tab does not have an enum value.
    ::initializeButtonsFingerprint.hookMethod(scopedHook(::getNavigationEnumMethod.member) {
        after { NavigationBar.setLastAppNavigationEnum(it.result as Enum<*>) }
    })

    // Hook the creation of navigation tab views.
    ::initializeButtonsFingerprint.hookMethod(scopedHook(::pivotBarButtonsCreateDrawableViewFingerprint.member) {
        after { NavigationBar.navigationTabLoaded(it.result as View) }
    })

//    if (!is_20_28_or_greater) {
//        // TODO
//    }

    ::initializeButtonsFingerprint.hookMethod(
        scopedHook(
            PivotBarButtonsCreateResourceStyledViewFingerprint.member
        ) {
            after {
                val isYouTab = runCatching {
                    Utils.getChildViewByResourceName<View>(
                        it.result as ViewGroup,
                        "you_tab_border"
                    ) ?: throw Exception("You tab border not found")
                }.isSuccess
                if (isYouTab) {
                    NavigationBar.setLastAppNavigationEnumYou()
                }

                NavigationBar.navigationTabLoaded(it.result as View)
            }
        })

    val selectedTab = ThreadLocal<View>()
    ::pivotBarButtonsViewSetSelectedFingerprint.hookMethod {
        before { selectedTab.remove() }
        after { selectedTab.get()?.let { NavigationBar.navigationTabSelected(it, true) } }
    }

    ::pivotBarButtonsViewSetSelectedFingerprint.hookMethod(scopedHook(::pivotBarButtonsViewSetSelectedSubFingerprint.member) {
        after {
            // `setSelect` dispatch to subviews, we need to wait for the root view.
            val isSelected = it.args[0] as Boolean
            if (isSelected) {
                selectedTab.set(it.thisObject as View)
            }
        }
    })

    // Hook onto back button pressed.  Needed to fix race problem with
    // Litho filtering based on navigation tab before the tab is updated.
    ::mainActivityOnBackPressedFingerprint.hookMethod {
        before { NavigationBar.onBackPressed(it.thisObject as Activity) }
    }

    // Hook the search bar.
    DexMethod("Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;").hookMethod {
        after {
            val layout = Utils.getContext().resources.getResourceName(it.args[0] as Int)
            if (layout.contains("/action_bar_search_results_view_")) {
                NavigationBar.searchBarResultsViewLoaded(it.result as View)
            }
        }
    }

    // Hook the back button visibility.
    ToolbarLayoutFingerprint.hookMethod(scopedHook(DexMethod("Landroid/view/ViewGroup;->findViewById(I)Landroid/view/View;").toMember()) {
        val appCompatToolbarClass =
            classLoader.loadClass(AppCompatToolbarBackButtonFingerprint.dexMethod.className)
        val getNavigationIcon = AppCompatToolbarBackButtonFingerprint.method
        val toolbarContainerId = toolbarContainerId
        after {
            if (it.args[0] != toolbarContainerId) return@after
            val layout = it.result as FrameLayout
            val toolbar = Utils.getChildView<View>(
                layout, false
            ) { it: View -> appCompatToolbarClass.isAssignableFrom(it.javaClass) }
            NavigationBar.setToolbar(toolbar.createProxy { impl ->
                NavigationBar.AppCompatToolbarPatchInterface {
                    getNavigationIcon(impl.get()) as Drawable?
                }
            })
        }
    })

    // Fix YT bug of notification tab missing the filled icon.
    val tabActivityCairo = ::navigationEnumClass.clazz.enumValueOf("TAB_ACTIVITY_CAIRO")
    if (tabActivityCairo != null) {
        ::getNavIconResIdFingerprint.dexMethodList.forEach {
            it.hookMethod {
                val fillBellCairoBlack = ResourceUtils.getIdentifier(
                    ResourceType.DRAWABLE,
                    if (VersionCheckPatch.IS_20_31_OR_GREATER)
                        "yt_fill_experimental_bell_vd_theme_24"
                    else
                        "morphe_fill_bell_cairo_black_24"
                )
                after {
                    val navEnum = it.args[0] as Enum<*>
                    val selected = it.args[1] as Boolean
                    if (navEnum == tabActivityCairo && selected)
                        it.result = fillBellCairoBlack
                }
            }
        }
    }
}