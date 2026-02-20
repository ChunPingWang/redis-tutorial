plugins {
    java
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "com.tutorial.redis"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}
