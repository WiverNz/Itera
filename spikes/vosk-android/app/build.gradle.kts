plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wivernz.itera.voskspike"
    compileSdk = 35

    defaultConfig {
        // separate id: installs beside Itera and never touches its data
        applicationId = "com.wivernz.itera.voskspike"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "spike"
        ndk { abiFilters += listOf("arm64-v8a") } // the vivo; production would ship per-ABI splits
    }

    sourceSets {
        // Itera's real, pure-Kotlin command parser, compiled from production sources (not copied)
        getByName("main").java.srcDir("../../../app/src/main/java/com/wivernz/itera/domain/voice")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
}
