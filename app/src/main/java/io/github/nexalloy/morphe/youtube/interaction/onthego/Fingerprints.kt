package io.github.nexalloy.morphe.youtube.interaction.onthego

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance

@RequireAppVersion(minVersion = "21.05.000")
internal object OnTheGoSkipToNextActionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("com.google.android.youtube.SKIP_TO_NEXT")
)

@RequireAppVersion(minVersion = "21.05.000")
internal object OnTheGoSkipToNextActionConstructorFingerprint : Fingerprint(
    definingClass = { OnTheGoSkipToNextActionFingerprint().declaredClass!!.superClass!!.descriptor },
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    name = "<init>"
)

@RequireAppVersion(minVersion = "21.05.000")
internal object OnTheGoStateConstructorFingerprint : Fingerprint(
    definingClass = { OnTheGoSkipToNextActionConstructorFingerprint().paramTypes.first().descriptor },
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    name = "<init>",
    filters = listOf(
        methodCall(smali = "Ljava/lang/Object;-><init>()V"),
        newInstance("Ljava/util/concurrent/atomic/AtomicBoolean;")
    )
)

@RequireAppVersion(minVersion = "21.05.000")
internal object OnTheGoStateGetterFingerprint : Fingerprint(
    definingClass = { OnTheGoSkipToNextActionConstructorFingerprint().paramTypes.first().descriptor },
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(methodCall(smali = "Ljava/util/concurrent/atomic/AtomicBoolean;->get()Z"))
)

@RequireAppVersion(minVersion = "21.05.000")
internal object OnTheGoStateSetterFingerprint : Fingerprint(
    definingClass = { OnTheGoSkipToNextActionConstructorFingerprint().paramTypes.first().descriptor },
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        methodCall(smali = "Ljava/util/concurrent/atomic/AtomicBoolean;->set(Z)V"),
        methodCall(smali = "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;", location = MatchAfterWithin(4))
    )
)
