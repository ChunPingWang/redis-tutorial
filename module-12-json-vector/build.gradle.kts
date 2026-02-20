plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(projects.common)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.redis.om.spring)

    testImplementation(projects.common)
}
