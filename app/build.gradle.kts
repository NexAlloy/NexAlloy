import com.google.protobuf.gradle.proto
import groovy.xml.MarkupBuilder
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.NodeChild
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.protobuf)
}

val gitCommitHashProvider = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    workingDir = rootProject.rootDir
}.standardOutput.asText!!

val gitCommitDateProvider = providers.exec {
    commandLine("git log -1 --format=%ct".split(" "))
    workingDir = rootProject.rootDir
}.standardOutput.asText!!

val isGitHubActionsBuild = providers.environmentVariable("GITHUB_ACTIONS")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

android {
    namespace = "io.github.nexalloy"

    defaultConfig {
        applicationId = "io.github.chsbuffer.revancedxposed"
        versionCode = 107
        versionName = "2.0.$versionCode"
        val patchVersion = Properties().apply {
            rootProject.file("morphe-patches/gradle.properties").inputStream().use { load(it) }
        }["version"]
        buildConfigField("String", "PATCH_VERSION", "\"$patchVersion\"")
        buildConfigField("String", "COMMIT_HASH", "\"${gitCommitHashProvider.get().trim()}\"")
        buildConfigField("long", "COMMIT_DATE", "${gitCommitDateProvider.get().trim()}L")
        buildConfigField("boolean", "CI_BUILD", isGitHubActionsBuild.get().toString())
    }
    androidResources {
        additionalParameters += arrayOf("--allow-reserved-package-id", "--package-id", "0x4b")
    }
    packaging.resources {
        excludes.addAll(
            arrayOf(
                "META-INF/**", "**.bin"
            )
        )
    }
    val ksFile = rootProject.file("signing.properties")
    signingConfigs {
        if (ksFile.exists()) {
            create("release") {
                val properties = Properties().apply {
                    ksFile.inputStream().use { load(it) }
                }

                storePassword = properties["KEYSTORE_PASSWORD"] as String
                keyAlias = properties["KEYSTORE_ALIAS"] as String
                keyPassword = properties["KEYSTORE_ALIAS_PASSWORD"] as String
                storeFile = file(properties["KEYSTORE_FILE"] as String)
            }
        }
    }
    buildFeatures.buildConfig = true
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            if (ksFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
//            excludes += "**"
        }
    }

    lint {
        checkReleaseBuilds = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        named("main") {
            val srcDirs = arrayOf(
                "../morphe-patches/extensions/shared/library/src/main/java",
                "../morphe-patches/extensions/shared-youtube/library/src/main/java",
                "../morphe-patches/extensions/youtube/src/main/java",
                "../morphe-patches/extensions/music/src/main/java",
                "../morphe-patches/extensions/reddit/src/main/java",
                "../morphe-patches-library/extension-library/src/main/java"
            )
            java.directories += srcDirs
            kotlin.directories += srcDirs

            proto {
                srcDirs(
                    "../morphe-patches/extensions/youtube/src/main/proto",
                    "../morphe-patches/extensions/shared-youtube/library/src/main/proto",
                )
            }
        }
    }
}

// Exclude Morphe-specific files that depend on protobuf/innertube/javascriptengine
// which are not available in the Xposed module build context.
tasks.withType<JavaCompile>().configureEach {
    exclude(
        "**/patches/HideRelatedVideosPatch.java",
        "**/patches/playback/quality/PrioritizeVideoQualityPatch.java",
        "**/OAuth2Preference.java",
        "**/SpoofVideoStreamsSignInPreference.java",
        "**/SpoofSignaturePatch.java",
    )
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xno-param-assertions",
            "-Xno-receiver-assertions",
            "-Xno-call-assertions",
            "-Xcontext-parameters"
        )
        jvmTarget = JvmTarget.JVM_17
    }
}
tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
//    implementation(libs.dexkit)

    // DexKit fork with instruction operand introspection
    // https://github.com/NexAlloy/DexKit/commit/046c0484b37e6a2100dd7bcc16748132c45dd2d9
    implementation(":dexkit-android@aar")
    implementation("com.google.flatbuffers:flatbuffers-java:23.5.26") // dexkit dependency
    implementation(libs.annotation)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.fuel)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.jadx.core)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.xposed)
    testImplementation(libs.libxposed.api)
    debugImplementation(kotlin("reflect"))
    compileOnly(libs.xposed)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
//    implementation(project(":extensions"))
    compileOnly(project(":stub"))
    implementation(libs.androidx.javascriptengine)
    implementation(libs.protobuf.javalite)
    implementation(libs.collections4)
    implementation(libs.lang3)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
