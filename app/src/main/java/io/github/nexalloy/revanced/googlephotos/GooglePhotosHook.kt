package io.github.nexalloy.revanced.googlephotos

import io.github.nexalloy.revanced.googlephotos.misc.backup.EnableDCIMFoldersBackupControl
import io.github.nexalloy.revanced.googlephotos.misc.features.SpoofFeaturesPatch
import io.github.nexalloy.revanced.shared.restrictions.AllowScreenCapture

val GooglePhotosPatches = arrayOf(
    AllowScreenCapture,
    SpoofFeaturesPatch,
    EnableDCIMFoldersBackupControl,
)