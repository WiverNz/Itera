plugins { java }

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.lint.api)
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

tasks.jar {
    manifest.attributes["Lint-Registry-v2"] = "com.wivernz.itera.lint.BootstrapIssueRegistry"
}

tasks.test {
    useJUnit()
    systemProperty("java.awt.headless", "true")
}
