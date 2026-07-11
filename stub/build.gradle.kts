plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "stub"
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
    sourceSets {
        named("main") {
            java.directories += arrayOf(
//                "../morphe-patches/patches/stub/src/main/java",
                "../morphe-patches/extensions/shared-youtube/stub/src/main/java",
                "../morphe-patches/extensions/youtube/stub/src/main/java",
                "../morphe-patches/extensions/reddit/stub/src/main/java",
            )
        }
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enableAndroidTest = false
    }
}
