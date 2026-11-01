package io.github.nexalloy.morphe.youtube.misc.settings

import io.github.nexalloy.AllowAbstract
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.resourceMappings
import java.lang.reflect.Modifier
import org.luckypray.dexkit.query.enums.StringMatchType

val PreferenceFragmentCompatClass = findClassDirect {
    findClass {
        matcher {
            usingStrings(
                "Could not create RecyclerView",
                "Content has view with id attribute 'android.R.id.list_container' that is not a ViewGroup class",
                "androidx.preference.PreferenceFragmentCompat.PREFERENCE_ROOT"
            )
        }
    }.single()
}

val PreferenceFragmentCompat_addPreferencesFromResource = findMethodDirect {
    PreferenceFragmentCompatClass().let { preferenceFragmentCompat ->
        preferenceFragmentCompat.findMethod {
            matcher {
                returnType = "void"
                paramTypes("int")
            }
        }.singleOrNull() ?: preferenceFragmentCompat.findMethod {
            matcher {
                name = "addPreferencesFromResource"
            }
        }.single()
    }
}

val licenseActivityClass = findClassDirect {
    findClass {
        matcher {
            className(".LicenseActivity", StringMatchType.EndsWith)
        }
    }.single()
}

val licenseActivitySuperOnCreate = findMethodDirect {
    licenseActivityClass().superClass!!.findMethod { matcher { name = "onCreate" } }.single()
}

val licenseActivityOnCreateFingerprint = findMethodDirect {
    licenseActivityClass().findMethod { matcher { name = "onCreate" } }.single()
}

val licenseActivityNOTonCreate = findMethodListDirect {
    licenseActivityClass().methods.filter { it.name != "onCreate" && it.isMethod }
}

val appearanceStringId get() = resourceMappings["string", "app_theme_appearance_dark"]

val setThemeFingerprint = findMethodDirect {
    findMethod {
        matcher {
            modifiers = Modifier.PUBLIC or Modifier.FINAL
            paramCount = 0
            addUsingNumber(appearanceStringId)
        }
    }.single { it.returnTypeName != "void" }
}
/**
 * Looks up the account scoped command router, which only the command router of an activity does.
 */
internal object CommandRouterFragmentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    strings = listOf("com.google.android.apps.youtube.app.endpoint.routers.AccountScopeCommandRouterFragment")
)

/**
 * The command router of an activity, created with it.
 */
internal object CommandRouterConstructorFingerprint : Fingerprint(
    classFingerprint = CommandRouterFragmentFingerprint,
    name = "<init>",
    parameters = listOf("Landroid/app/Activity;", "L", "Ljava/util/concurrent/Executor;"),
    filters = listOf(
        methodCall(smali = "Ljava/lang/Object;-><init>()V")
    )
)

@get:AllowAbstract
internal val executeSnackbarCommandMethod = findMethodDirect {
    CommandRouterConstructorFingerprint().declaredClass!!.interfaces.flatMap { it.methods }
        .filter { method -> method.returnType?.descriptor == "V" && method.paramCount == 2 &&
            method.paramTypes[0].descriptor != "Ljava/util/List;" &&
            method.paramTypes[1].descriptor == "Ljava/util/Map;" }.single()
}

internal val snackbarCommandDefaultInstance = io.github.nexalloy.morphe.findFieldDirect {
    val command = executeSnackbarCommandMethod().paramTypes.first()
    command.fields.single { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == command }
}
