pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Itera"
include(":app")

// Milestone 013: one on-demand Play Asset Delivery pack per offline voice model (ADR-0022). The model files are
// fetched and checksum-verified at bundle time (see voicemodels/README.md); none are committed.
listOf("en", "ru", "de", "es").forEach { language ->
    include(":voice_model_$language")
    project(":voice_model_$language").projectDir = file("voicemodels/voice_model_$language")
}
