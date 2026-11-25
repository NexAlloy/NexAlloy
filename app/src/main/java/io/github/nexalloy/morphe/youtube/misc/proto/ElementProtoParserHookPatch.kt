package io.github.nexalloy.morphe.youtube.misc.proto

import io.github.nexalloy.morphe.shared.misc.proto.createElementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_40_or_greater

val elementProtoParserHookPatch = createElementProtoParserHookPatch(
    { is_21_40_or_greater },
    VersionCheck,
    reusesHookedElements = true,
)
