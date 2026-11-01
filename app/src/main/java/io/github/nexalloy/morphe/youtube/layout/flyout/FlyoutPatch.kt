package io.github.nexalloy.morphe.youtube.layout.flyout

import android.app.Dialog
import android.widget.PopupWindow
import android.widget.TextView
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.youtube.patches.utils.FlyoutUtils
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.youtube.layout.hide.general.ContextualMenuItemBuilderFingerprint
import io.github.nexalloy.morphe.youtube.layout.hide.general.contextualMenuItemTextViewField
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.shared.startVideoInformerPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.lang.reflect.Field
import org.luckypray.dexkit.wrap.DexMethod

lateinit var protocolBufferField: Field
lateinit var protocolBufferFieldClass: Class<*>
lateinit var flyoutMenuVideoIdField: Field
lateinit var flyoutMenuVideoIdClass: Class<*>
private lateinit var playlistFlyoutMenuVideoIdField: Field

val flyoutPatch = patch(
    description = "Provides shared flyout menu hooks.",
) {
    dependsOn(
        startVideoInformerPatch,
        VideoInformationPatch,
        elementProtoParserHookPatch,
    )

    protocolBufferField = ::ProtocolBufferField.field
    protocolBufferFieldClass = ::ProtocolBufferFieldClass.clazz
    flyoutMenuVideoIdField = ::FlyoutMenuVideoIdField.field
    flyoutMenuVideoIdClass = ::FlyoutMenuVideoIdClass.clazz

    // Playlists in 'You' tab. Doesn't seem required for 21.x but is required for 20.21
    playlistFlyoutMenuVideoIdField = ::PlaylistFlyoutMenuVideoIdField.field

    FeedFlyoutBufferObjectFingerprint.hookMethod {
        before {
            val map = it.args[1] as Map<*, *>
            FlyoutUtils.extractFlyoutIdFromMap(wrapMap(map))
        }
    }

    val innertubeMenuAnchorTag = ResourceUtils.getIdIdentifier("innertube_menu_anchor_tag")
    FullHistoryFlyoutBufferObjectFingerprint.hookMethod(scopedHook(
        DexMethod("Landroid/view/View;->getTag(I)Ljava/lang/Object;").toMethod()
    ) {
        after {
            if (it.args[0] == innertubeMenuAnchorTag) {
                FlyoutUtils.extractFlyoutIdFromObject(wrap(it.result))
            }
        }
    })

    FeedBottomSheetFlyoutFingerprint.hookMethod {
        after {
            FlyoutUtils.setBottomSheetFlyout(it.result as? Dialog)
        }
    }

    ::FeedPopupWindowFlyout.dexMethodList.forEach {
        it.hookMethod(
            scopedHook(
                DexMethod($$"Landroid/widget/PopupWindow;->setOnDismissListener(Landroid/widget/PopupWindow$OnDismissListener;)V").toMethod(),
                {
                    before {
                        FlyoutUtils.setPopupWindowFlyout(it.thisObject as PopupWindow)
                    }
                })
        )
    }

    // Track and initialize flyout menu buttons generically.
    val getSubMessageMethod = ::getSubMessageReference.method
    val enumIntField = ::enumIntField.field
    val enumMethodCall = ::enumMethodCall.method

    FeedFlyoutButtonsInitializerFingerprint.hookMethod(scopedHook(::feedFlyoutButtonTextMethod.method) {
        after {
            val buttonInfo = getSubMessageMethod(null, it.args[0])
            val enumValue = enumMethodCall(null, enumIntField.get(buttonInfo))
            FlyoutUtils.setCurrentButtonInfo(enumValue as? Enum<*>, buttonInfo, it.result as? CharSequence)
        }
    })

    ContextualMenuItemBuilderFingerprint.hookMethod(scopedHook(::contextualMenuItemStyleMethod.method) {
        val textViewField = ::contextualMenuItemTextViewField.field
        before {
            val textView = textViewField.get(outerParam.thisObject) as TextView?

            val messageLite = it.args[0]
            val enumValue = enumMethodCall(null, enumIntField.get(getSubMessageMethod(null, messageLite)))
            FlyoutUtils.setCurrentButtonInfo(enumValue as? Enum<*>, textView)
        }
    })
}

private fun wrapMap(map: Map<*, *>): Map<*, *> {
    val newMap = mutableMapOf<Any?, Any?>()
    val tag = "com.google.android.libraries.youtube.innertube.endpoint.tag"
    val sender_view = "com.google.android.libraries.youtube.rendering.elements.sender_view"
    newMap[tag] = wrap(map[tag])
    newMap[sender_view] = map[sender_view]
    return newMap
}

private fun wrap(obj: Any?): Any? {
    if (obj == null) return null

    return if (flyoutMenuVideoIdClass.isAssignableFrom(obj::class.java)) {
        FlyoutUtils.FlyoutMenuVideoIdInterface { flyoutMenuVideoIdField.get(obj) as String? }
    } else if (playlistFlyoutMenuVideoIdField.declaringClass.isInstance(obj)) {
        FlyoutUtils.FlyoutMenuVideoIdInterface { playlistFlyoutMenuVideoIdField.get(obj) as String? }
    } else if (protocolBufferFieldClass.isAssignableFrom(obj::class.java)) {
        FlyoutUtils.ProtocolBufferFieldInterface { protocolBufferField.get(obj) as ByteArray? }
    } else {
        obj
    }
}
