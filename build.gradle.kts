plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.asset.pack) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.spotless)
}

val composeNaming = providers.fileContents(
    layout.projectDirectory.file(".editorconfig")
).asText.map {
    it.lineSequence().first { line ->
        line.startsWith("ktlint_function_naming_ignore_when_annotated_with")
    }.substringAfter("=").trim()
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(
                mapOf("ktlint_function_naming_ignore_when_annotated_with" to composeNaming.get())
            )
    }
    kotlinGradle {
        target(
            "*.gradle.kts",
            "app/*.gradle.kts",
            "buildSrc/*.gradle.kts",
            "voicemodels/*/*.gradle.kts"
        )
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(
                mapOf("ktlint_function_naming_ignore_when_annotated_with" to composeNaming.get())
            )
    }
}
