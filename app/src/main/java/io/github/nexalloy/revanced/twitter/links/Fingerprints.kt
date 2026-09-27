package io.github.nexalloy.revanced.twitter.links

import io.github.nexalloy.morphe.fingerprint

val addSessionTokenFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    parameters(
        "Ljava/lang/String;",
        "L",
        "Ljava/lang/String;",
    )
    strings(
        "<this>",
        "shareParam",
        "sessionToken",
    )
}

val redirectToXLiteFingerprint = fingerprint {
    returns("Z")
    strings(
        "x_lite_in_tfa_for_existing_users_enabled",
        "existing_user_redirected_to_x_lite",
        "x_lite_in_tfa_for_existing_users_exit_enabled",
    )
}
