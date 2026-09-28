// Throwaway spike: proves an Itera-owned offline recogniser (Vosk) on a real device. Not part of any production build.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "itera-vosk-spike"
include(":app")
