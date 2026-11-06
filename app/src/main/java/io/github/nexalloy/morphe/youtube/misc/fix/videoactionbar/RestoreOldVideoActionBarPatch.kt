package io.github.nexalloy.morphe.youtube.misc.fix.videoactionbar

import app.morphe.extension.youtube.innertube.NextResponseOuterClass.SecondaryContents
import app.morphe.extension.youtube.patches.RestoreOldVideoActionBarPatch
import app.morphe.extension.youtube.patches.RestoreOldVideoActionBarPatch.ConfigInfoInterface
import app.morphe.extension.youtube.patches.RestoreOldVideoActionBarPatch.RequestInterface
import io.github.nexalloy.callMethod
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.request.buildRequestPatch
import io.github.nexalloy.morphe.shared.misc.request.hookBuildRequest
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.interaction.downloads.GeneratedRegistryFingerprint
import io.github.nexalloy.morphe.youtube.interaction.downloads.ProtobufClassParseByteArrayWithRegistryFingerprint
import io.github.nexalloy.morphe.youtube.misc.contexthook.Endpoint
import io.github.nexalloy.morphe.youtube.misc.contexthook.addClientVersionHook
import io.github.nexalloy.morphe.youtube.misc.contexthook.clientContextHookPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_30_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_39_or_greater
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.util.function.Supplier

internal val restoreOldVideoActionBarPatch = patch(
    description = "Overrides 'X-Youtube-Cold-Config-Data', fixes 'Hide video action buttons' and 'Return YouTube Dislike', "
) {
    dependsOn(VersionCheck, clientContextHookPatch, elementProtoParserHookPatch)

    PreferenceScreen.PLAYER.addPreferences(
        ListPreference("morphe_video_action_bar_layout")
    )

    // The layout of the action bar is a field of its element, so the Classic and Minimal layouts
    // do not depend on the layout that the server sends.
    hookElement(RestoreOldVideoActionBarPatch::setVideoActionBarLayout)

    // The two rows of the Classic Two-Rows layout are two elements, and the server sends one.
    val sectionsField = ::watchNextSectionsField.field
    val isModifiable = ::protobufListIsModifiableMethod.method
    val mutableCopy = ::protobufListMutableCopyMethod.method
    val makeImmutable = ::protobufListMakeImmutableMethod.method
    val parse = ProtobufClassParseByteArrayWithRegistryFingerprint.method
    val registry = GeneratedRegistryFingerprint.method.invoke(null)
    ::watchNextRowsMethod.hookMethod {
        before {
            @Suppress("UNCHECKED_CAST")
            val sections = sectionsField.get(it.args[0]) as MutableList<Any>
            if (sections.isEmpty()) return@before
            val first = sections[0]
            // Serialize only the first section; other patches can change later sections in place.
            val section = SecondaryContents.parseFrom(first.callMethod("toByteArray") as ByteArray)
            val bytes = RestoreOldVideoActionBarPatch.getTwoRowsSection(section) ?: return@before
            val updated = parse.invoke(null, first, bytes, registry)!!
            if (isModifiable.invoke(sections) as Boolean) {
                sections[0] = updated
            } else {
                @Suppress("UNCHECKED_CAST")
                val copy = mutableCopy.invoke(null, sections) as MutableList<Any>
                copy[0] = updated
                makeImmutable.invoke(copy)
                sectionsField.set(it.args[0], copy)
            }
        }
    }

    if (is_20_39_or_greater) {
        insertLiteralOverride(45640404L, RestoreOldVideoActionBarPatch::useMediaSessionFeatureFlag)
    }

    if (is_20_30_or_greater) {
        dependsOn(buildRequestPatch)
        hookBuildRequest(RestoreOldVideoActionBarPatch::fixVideoActionBar)

        val requestFields = ::innerTubeRequestFields.fieldList
        val endpointFields = ::innerTubeEndpointFields.fieldList
        val coldConfigField = ::coldConfigDataField.field
        val coldHashField = ::coldHashDataField.field
        val buildMethod = ::configInfoBuildMethod.method

        // Comment requests use the same 'next' endpoint as watch page requests.
        // Their config must not be overridden, so find out which requests are for the watch page.
        // Some app versions build requests in more than one class (21.16 has two, and the watch
        // page 'next' request uses the second one), so every request class is hooked.
        ::innerTubeRequestBodyMethods.dexMethodList.forEach { bodyMethod ->
            val requestClass = bodyMethod.toMethod().declaringClass
            val requestField = requestFields.single { it.declaringClass == requestClass }
            val endpointField = endpointFields.single { it.declaringClass.isAssignableFrom(requestField.type) }

            bodyMethod.hookMethod(scopedHook(buildMethod) {
                after {
                    val config = it.result ?: return@after
                    if (!coldConfigField.declaringClass.isInstance(config)) return@after
                    RestoreOldVideoActionBarPatch.fixVideoActionBar(object : ConfigInfoInterface {
                        override fun patch_setColdConfigData(value: String) = coldConfigField.set(config, value)
                        override fun patch_setColdHashData(value: String) = coldHashField.set(config, value)
                    })
                }
            })
            bodyMethod.hookMethod {
                after {
                    val body = it.result ?: return@after
                    val request = requestField.get(it.thisObject) ?: return@after
                    RestoreOldVideoActionBarPatch.onBuildRequestBody(
                        Supplier { body.callMethod("toByteArray") as ByteArray },
                        RequestInterface { endpointField.get(request) as String? }
                    )
                }
            }
        }

        // fix: related video overlay is broken due to patch.
        insertLiteralOverride(45614162L, RestoreOldVideoActionBarPatch::fixRelatedVideoOverlay)
        insertLiteralOverride(45661108L, RestoreOldVideoActionBarPatch::fixRelatedVideoOverlay)
    } else {
        // In YT 20.29 or earlier, there are no issues even if the app version is spoofed.
        // Simply spoofing the app version (in the player).
        setOf(Endpoint.GET_WATCH, Endpoint.NEXT).forEach { endpoint ->
            addClientVersionHook(endpoint, RestoreOldVideoActionBarPatch::getVideoActionBarAppVersionOverride)
        }
    }
}
