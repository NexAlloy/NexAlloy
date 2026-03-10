package io.github.chsbuffer.revancedxposed.threads.ads

import io.github.chsbuffer.revancedxposed.findMethodDirect
import io.github.chsbuffer.revancedxposed.strings

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            strings("SponsoredContentController.processValidatedContent")
        }
    }.single()
}

val adSponsoredContentFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            strings("SponsoredContentController.insertItem")
        }
    }.single()
}
