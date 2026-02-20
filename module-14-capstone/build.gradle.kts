plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(projects.common)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.redis.om.spring)
    implementation(libs.redisson.spring.boot.starter)
    implementation(libs.caffeine)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(projects.common)
}
