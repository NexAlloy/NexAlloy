package io.github.nexalloy.revanced.facebook.ads

import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.returns

val storyAdRunCandidates = findMethodListDirect {
    findClass {
        matcher {
            addField {
                name = "__redex_internal_original_name"
            }
        }
    }.findMethod {
        matcher {
            name = "run"
            returns("V")
            paramCount = 0
        }
    }.toList()
}
