package io.github.nexalloy.morphe.youtube.misc.contexthook

import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.patch

internal enum class ClientContextEndpoint {
    BROWSE,
    GET_WATCH,
    GUIDE,
    NEXT,
    PLAYER,
    REEL,
    SEARCH;

    @Volatile
    var osNameHooks: Array<(String) -> String> = emptyArray()
        private set

    fun addOSNameHook(hook: (String) -> String) {
        synchronized(this) {
            osNameHooks = osNameHooks + hook
        }
    }
}

internal fun addOSNameHook(
    endpoint: ClientContextEndpoint,
    hook: (String) -> String
) {
    endpoint.addOSNameHook(hook)
}

internal val ClientContextHook = patch {
    dependsOn(VersionCheck)

    val builderField = ::messageLiteBuilderField.field.apply { isAccessible = true }
    val clientField = ::clientInfoField.field.apply { isAccessible = true }
    val osField = ::osNameField.field.apply { isAccessible = true }
    val builderMethod = ::messageLiteBuilderMethod.method.apply { isAccessible = true }

    fun applyHooks(target: Any, endpoint: ClientContextEndpoint) {
        val hooks = endpoint.osNameHooks
        if (hooks.isEmpty()) return
        val builder = builderMethod.invoke(target) ?: return
        val contextBody = builderField.get(builder) ?: return
        val clientInfo = clientField.get(contextBody) ?: return
        var osName = osField.get(clientInfo) as? String ?: return
        for (hook in hooks) {
            osName = hook(osName)
        }
        osField.set(clientInfo, osName)
    }

    fun hook(method: java.lang.reflect.Member, endpoint: ClientContextEndpoint) {
        method.hookMethod {
            after {
                if (it.hasThrowable()) return@after
                applyHooks(it.thisObject, endpoint)
            }
        }
    }

    hook(::browseRequestBodyMethod.member, ClientContextEndpoint.BROWSE)
    hook(::guideRequestBodyMethod.member, ClientContextEndpoint.GUIDE)
    hook(::nextRequestBodyMethod.member, ClientContextEndpoint.NEXT)
    hook(::playerRequestBodyMethod.member, ClientContextEndpoint.PLAYER)
    hook(::searchRequestBodyMethod.member, ClientContextEndpoint.SEARCH)
    ::getWatchRequestBodyMethods.dexMethodList.forEach { method ->
        hook(method.toMember(), ClientContextEndpoint.GET_WATCH)
    }
    ::reelRequestBodyMethods.dexMethodList.forEach { method ->
        hook(method.toMember(), ClientContextEndpoint.REEL)
    }
}
