import com.google.protobuf.gradle.proto
import groovy.json.JsonGenerator
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

android {
    namespace = "io.github.nexalloy"

    defaultConfig {
        applicationId = "io.github.chsbuffer.revancedxposed"
        versionCode = 114
        versionName = "2.0.$versionCode"
        val patchVersion = Properties().apply {
            rootProject.file("morphe-patches/gradle.properties").inputStream().use { load(it) }
        }["version"]
        buildConfigField("String", "PATCH_VERSION", "\"$patchVersion\"")
        buildConfigField("String", "COMMIT_HASH", "\"${gitCommitHashProvider.get().trim()}\"")
        buildConfigField("long", "COMMIT_DATE", "${gitCommitDateProvider.get().trim()}L")
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
    val nativeLibraries = layout.projectDirectory.dir(
        if (System.getProperty("os.name") == "Linux" &&
            System.getProperty("os.arch") in listOf("aarch64", "arm64")
        ) "src/test/jniLibs/linux-arm64" else "src/test/jniLibs"
    )
    systemProperty("java.library.path", nativeLibraries.asFile.absolutePath)
    inputs.files(fileTree(nativeLibraries)).withPropertyName("dexkitNativeLibraries")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    val fingerprintReport = layout.buildDirectory.file("reports/fingerprints/$name.json")
    systemProperty("fingerprints.report", fingerprintReport.get().asFile.absolutePath)
    inputs.files(fileTree("binaries")).withPropertyName("fingerprintApks")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(fingerprintReport)
}

dependencies {
//    implementation(libs.dexkit)

    // DexKit fork with instruction operand introspection
    // https://github.com/NexAlloy/DexKit/commit/59d5d332a1ac1bc543d1588b9c9f0377ea3c6ea5
    implementation(":dexkit-android@aar")
    implementation("com.google.flatbuffers:flatbuffers-java:23.5.26") // dexkit dependency
    implementation(libs.annotation)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.fuel)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.client)
    implementation(libs.okhttp.coroutines)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.jadx.core)
    testImplementation(libs.slf4j.simple)
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
    implementation(libs.hiddenapibypass)
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

abstract class GenerateStringsTask @Inject constructor(
) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val assetsDirectory: DirectoryProperty

    private fun writeNode(builder: MarkupBuilder, node: Any?) {
        if (node !is NodeChild) return
        val attributes = node.attributes()
        builder.withGroovyBuilder {
            if (node.children().any()) {
                node.name()(attributes) {
                    node.children().forEach {
                        writeNode(builder, it)
                    }
                }
            } else {
                node.name()(attributes, node.text())
            }
        }
    }

    private fun readResources(inputFiles: List<File>): List<NodeChild> = buildList {
        val keys = mutableSetOf<String>()
        for (inputFile in inputFiles) {
            if (!inputFile.exists()) continue
            XmlSlurper().parse(inputFile).children().forEach {
                if (it !is NodeChild) return@forEach
                val key = it.attributes()["name"] as? String ?: return@forEach
                if (keys.add(key)) add(it)
            }
        }
    }

    private fun writeResources(resources: List<NodeChild>, output: File, translationsFile: String? = null) {
        output.parentFile.mkdirs()
        output.writer().use { writer ->
            val builder = MarkupBuilder(writer)
            builder.doubleQuotes = true
            builder.withGroovyBuilder {
                "resources" {
                    resources.forEach { writeNode(builder, it) }
                    if (translationsFile != null) {
                        "string"(mapOf("name" to "morphe_translations_file"), translationsFile)
                    }
                }
            }
        }
    }

    // Same Android string decoding as upstream TranslationsJson.resolve.
    private fun resolveString(text: String): String {
        val result = StringBuilder()
        var quoted = false
        var pendingSpace = false
        var index = 0
        while (index < text.length) {
            val char = text[index++]
            if (!quoted && (char == ' ' || char == '\n' || char == '\t' || char == '\r')) {
                pendingSpace = result.isNotEmpty()
                continue
            }
            if (pendingSpace) {
                result.append(' ')
                pendingSpace = false
            }
            when (char) {
                '"' -> quoted = !quoted
                '\\' -> if (index < text.length) {
                    when (val escaped = text[index++]) {
                        'n' -> result.append('\n')
                        't' -> result.append('\t')
                        'u' -> {
                            result.append(text.substring(index, index + 4).toInt(16).toChar())
                            index += 4
                        }
                        else -> result.append(escaped)
                    }
                }
                else -> result.append(char)
            }
        }
        return result.toString()
    }

    // Subdirectories within each variant that contain resource files.
    private val subDirs = listOf("shared", "shared-youtube", "youtube", "music", "reddit", "sponsorblock")

    // YouTube reads the injected settings labels directly from native preference XML.
    private val nativeSettingsLabels = setOf("morphe_settings_title", "morphe_settings_submenu_title")

    @TaskAction
    fun action() {
        val inputDir = inputDirectory.get().asFile
        val outputDir = outputDirectory.get().asFile
        val assetsDir = assetsDirectory.get().asFile
        val defaultNames = readResources(subDirs.map { File(inputDir, "values/$it/strings.xml") })
            .mapTo(hashSetOf()) { it.attributes()["name"] as String }

        outputDir.deleteRecursively()
        assetsDir.deleteRecursively()
        assetsDir.mkdirs()

        runCatching {
            // Process each variant directory (values, values-xx-rYY, ...)
            inputDir.listFiles()?.filter { it.isDirectory }?.forEach { variant ->
                val genResDir = File(outputDir, variant.name).apply { mkdirs() }

                val stringFiles = subDirs.map { File(variant, "$it/strings.xml") }
                val locale = variant.name.removePrefix("values-")
                if (variant.name == "values") {
                    writeResources(readResources(stringFiles), File(genResDir, "strings.xml"), "")
                } else {
                    val strings = readResources(stringFiles)
                    val translations = strings
                        .filter { it.attributes()["name"] in defaultNames }
                        .associate { it.attributes()["name"] as String to resolveString(it.text()) }
                    File(assetsDir, "morphe/strings/$locale.json").apply {
                        parentFile.mkdirs()
                        writeText(JsonGenerator.Options().disableUnicodeEscaping().build().toJson(translations))
                    }
                    writeResources(
                        strings.filter { it.attributes()["name"] in nativeSettingsLabels },
                        File(genResDir, "strings.xml"), locale
                    )
                }

                // Merge arrays.xml from all subdirectories
                val arrayFiles = subDirs.map { File(variant, "$it/arrays.xml") }
                if (arrayFiles.any { it.exists() }) {
                    writeResources(readResources(arrayFiles), File(genResDir, "arrays.xml"))
                }
            }
        }.onFailure {
            System.err.println(it)
            throw it
        }
    }
}

abstract class CopyResourcesTask @Inject constructor() : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        val baseDir = inputDirectory.get().asFile
        val outputDir = outputDirectory.get().asFile
        outputDir.deleteRecursively()

        val resourcePaths = mapOf(
            "change-header/drawable" to null,
            "channelsearch/drawable" to null,
            "qualitybutton/drawable" to null,
            "settings/drawable" to null,
            "settings/menu" to null,
            "settings/layout" to listOf("morphe_settings_with_toolbar.xml"),
            "sponsorblock/drawable" to null,
            "sponsorblock/layout" to listOf("morphe_sb_skip_sponsor_button.xml"),
            "swipecontrols/drawable" to null,
            "copyvideolinkbutton/drawable" to null,
            "downloads/drawable" to null,
            "dearrowbutton/drawable" to null,
            "speedbutton/drawable" to null,
            "navigationbar/drawable" to null,
            "speed/drawable" to null,
        )

        for ((resourcePath, excludes) in resourcePaths) {
            val dir = resourcePath.substringAfter('/')
            val sourceDir = File(baseDir, resourcePath)
            val targetDir = File(outputDir, dir)
            sourceDir.listFiles()?.forEach { file ->
                if (excludes == null || !excludes.contains(file.name)) {
                    file.copyTo(File(targetDir, file.name), overwrite = true)
                }
            }
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.packaging.resources.excludes.add("kotlin/**")
    }

    onVariants { variant ->
        val variantName = variant.name.uppercaseFirstChar()
        val strTask = project.tasks.register<GenerateStringsTask>("generateStrings$variantName") {
            inputDirectory.set(project.file("../morphe-patches/patches/src/main/resources/addresources"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(
            strTask, GenerateStringsTask::outputDirectory
        )
        variant.sources.assets?.addGeneratedSourceDirectory(
            strTask, GenerateStringsTask::assetsDirectory
        )

        val resTask = project.tasks.register<CopyResourcesTask>("copyResources$variantName") {
            inputDirectory.set(project.file("../morphe-patches/patches/src/main/resources"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(
            resTask, CopyResourcesTask::outputDirectory
        )
    }
}
