plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    api(platform(libs.testcontainers.bom))

    // Spring Data Redis - exposed as API for modules
    api(libs.spring.boot.starter.data.redis)
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.module.parameter.names)

    // Test infrastructure - exposed as API for modules
    api(libs.spring.boot.starter.test)
    api(libs.spring.boot.testcontainers)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.redis)
    api(libs.assertj.core)
    api(libs.mockito.core)

    // Test fixtures dependencies (for AbstractRedis*IntegrationTest)
    testFixturesApi(libs.spring.boot.starter.data.redis)
    testFixturesApi(libs.spring.boot.starter.test)
    testFixturesApi(libs.spring.boot.testcontainers)
    testFixturesApi(libs.testcontainers.junit.jupiter)
}
