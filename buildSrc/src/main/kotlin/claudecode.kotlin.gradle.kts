import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("claudecode.java")
    kotlin("jvm")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xjdk-release=1.8",
            // Suppress deprecation warnings because we may still reference and test deprecated members.
            // TODO: Replace with `-Xsuppress-warning=DEPRECATION` once we use Kotlin compiler 2.1.0+.
            "-nowarn",
        )
        jvmTarget.set(JvmTarget.JVM_1_8)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
        coreLibrariesVersion = "1.8.0"
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("junit.jupiter.execution.parallel.enabled", true)
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("integration")
    }
    
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    
    shouldRunAfter("test")
}