package io.github.chsbuffer.revancedxposed.threads.network

import io.github.chsbuffer.revancedxposed.findMethodDirect

val networkInterceptorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass("com.instagram.api.tigon.TigonServiceLayer")
            name = "startRequest"
        }
    }.single()
}
