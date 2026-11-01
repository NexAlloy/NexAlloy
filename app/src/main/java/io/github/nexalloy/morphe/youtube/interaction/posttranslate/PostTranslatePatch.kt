/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4163
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.posttranslate

import android.graphics.drawable.Drawable
import android.text.Layout
import app.morphe.extension.youtube.patches.PostTranslatePatch
import io.github.nexalloy.createProxy
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookLithoSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.lithoSpannableStringPatch
import io.github.nexalloy.morphe.youtube.layout.flyout.flyoutPatch
import io.github.nexalloy.morphe.youtube.misc.feedcards.feedVideoCardsHookPatch
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import java.util.WeakHashMap

val postTranslatePatch = patch(
    name = "Translate posts",
    description = "Adds a menu item to translate the text of posts.",
) {
    dependsOn(
        elementProtoParserHookPatch,
        lithoSpannableStringPatch,
        flyoutPatch,
        feedVideoCardsHookPatch,
    )

    PreferenceScreen.FEED.addPreferences(
        SwitchPreference("morphe_post_translate", summary = true),
        ListPreference("morphe_post_translate_language"),
    )

    setExtensionIsPatchIncluded(PostTranslatePatch::class.java)

    // The translation is shown by the text drawables mounted in each component host.
    val ensureMountItems = ::postEnsureMountItems.method
    val mountItemsField = ::postMountItemsField.field
    val size = ::postMountItemsSize.method
    val valueAt = ::postMountItemAt.method
    val content = ::postMountedContentField.field
    val textField = ::postDrawableTextField.field
    val layoutField = ::postDrawableLayoutField.field
    val adapters = WeakHashMap<Drawable, PostTranslatePatch.TextDrawableInterface>()
    PostTranslatePatch.setComponentAdapters({ host ->
        PostTranslatePatch.ComponentHostInterface {
            ensureMountItems.invoke(host)
            val items = mountItemsField.get(host)
            List(size.invoke(items) as Int) { index -> content.get(valueAt.invoke(items, index)) }
        }
    }, { item ->
        if (item !is Drawable || !textField.declaringClass.isInstance(item)) null
        else adapters.getOrPut(item) {
            item.createProxy { reference ->
                object : PostTranslatePatch.TextDrawableInterface {
                    override fun patch_getLayout() = reference.get()?.let { layoutField.get(it) as? Layout }
                    override fun patch_setLayout(layout: Layout) {
                        reference.get()?.let { layoutField.set(it, layout) }
                    }
                    override fun patch_getText() = reference.get()?.let { textField.get(it) as? CharSequence }
                    override fun patch_setText(text: CharSequence) {
                        reference.get()?.let { textField.set(it, text) }
                    }
                    override fun patch_getDrawable(): Drawable? = reference.get()
                }
            }
        }
    })

    // The element of a post includes its whole text, while a long text is shown cut off.
    hookElement(PostTranslatePatch::onElementLoaded)

    // Texts that Litho creates again are replaced with their translation.
    hookLithoSpannableString(PostTranslatePatch::onLithoTextLoaded)
}
