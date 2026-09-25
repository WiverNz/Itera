import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

val appVersion = Properties().apply {
    load(
        providers.fileContents(
            rootProject.layout.projectDirectory.file("version.properties")
        ).asText.get().reader()
    )
}
val appVersionName = appVersion.getProperty("versionName")
val appVersionCode = appVersion.getProperty("versionCode")?.toIntOrNull()
require(
    appVersionName != null &&
        Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(appVersionName)
) {
    "version.properties must contain a stable versionName (X.Y.Z)"
}
require(appVersionCode != null && appVersionCode in 1..2100000000) {
    "version.properties must contain versionCode in 1..2100000000"
}
val signingEnvironment = listOf(
    "ITERA_KEYSTORE_PATH",
    "ITERA_STORE_PASSWORD",
    "ITERA_KEY_ALIAS",
    "ITERA_KEY_PASSWORD"
).associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = signingEnvironment.values.all { !it.isNullOrBlank() }
require(signingEnvironment.values.all { it.isNullOrBlank() } || hasReleaseSigning) {
    "Provide all four ITERA signing environment variables or none"
}
require(
    providers.environmentVariable("ITERA_REQUIRE_SIGNING").orNull != "true" || hasReleaseSigning
) {
    "Release signing credentials are required"
}

android {
    namespace = "com.wivernz.itera"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.wivernz.itera"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingEnvironment.getValue("ITERA_KEYSTORE_PATH")!!)
                storePassword = signingEnvironment.getValue("ITERA_STORE_PASSWORD")
                keyAlias = signingEnvironment.getValue("ITERA_KEY_ALIAS")
                keyPassword = signingEnvironment.getValue("ITERA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        disable += setOf("GradleDependency", "NewerVersionAvailable")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    lintChecks(files(rootProject.layout.projectDirectory.file("buildSrc/build/libs/buildSrc.jar")))
    implementation(libs.kotlin.stdlib.common)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    androidTestImplementation(libs.hilt.testing)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.typed)
    implementation(libs.work.runtime)
    androidTestImplementation(libs.work.testing)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.immutable)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    kspAndroidTest(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
room {
    schemaDirectory("$projectDir/schemas")
}

kover {
    reports {
        filters { includes { classes("com.wivernz.itera.domain.*") } }
        verify {
            rule("domain line coverage") {
                minBound(85)
            }
        }
    }
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.graphicsMode", "NATIVE")
    systemProperty("user.timezone", "UTC")
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

tasks.named("check") {
    dependsOn("koverVerify", rootProject.tasks.named("spotlessCheck"))
}

dependencyLocking { lockAllConfigurations() }
