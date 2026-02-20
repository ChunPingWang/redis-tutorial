plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(projects.common)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.caffeine)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(projects.common)
    testImplementation(testFixtures(projects.common))
}
