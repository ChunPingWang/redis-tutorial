plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(projects.common)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.caffeine)

    testImplementation(projects.common)
}
