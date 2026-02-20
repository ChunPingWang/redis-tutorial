plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(projects.common)
    implementation(libs.spring.boot.starter.web)

    testImplementation(projects.common)
}
