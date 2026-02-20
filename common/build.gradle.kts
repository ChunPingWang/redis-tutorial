plugins {
    `java-library`
}

dependencies {
    api(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    api(platform(libs.testcontainers.bom))

    // Spring Data Redis - exposed as API for modules
    api(libs.spring.boot.starter.data.redis)
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)

    // Test infrastructure - exposed as API for modules
    api(libs.spring.boot.starter.test)
    api(libs.spring.boot.testcontainers)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.redis)
    api(libs.assertj.core)
    api(libs.mockito.core)
}
