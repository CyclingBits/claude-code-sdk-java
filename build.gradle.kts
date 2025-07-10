plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka") version "1.9.20"
    id("jacoco")
}

group = "net.cyclingbits"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Core dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okio:okio:3.10.2")
    
    // Optional for better error handling
    compileOnly("com.michael-bull.kotlin-result:kotlin-result:2.0.1")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    
    // Pass system properties to tests
    systemProperty("run.integration.tests", System.getProperty("run.integration.tests") ?: "false")
}

// JaCoCo configuration
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

kotlin {
    jvmToolchain(8)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        // Generate Java-friendly APIs
        freeCompilerArgs.add("-Xjvm-default=all")
    }
    // Explicit API mode for better library design
    explicitApi()
}

// Ensure Java compatibility
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

// Use Dokka for generating javadoc jar
tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

artifacts {
    archives(tasks["javadocJar"])
}

// Configure jar manifest
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Claude Code SDK",
            "Implementation-Version" to version
        )
    }
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            artifact(tasks["javadocJar"])
            
            pom {
                name.set("Claude Code SDK Java")
                description.set("A Kotlin/Java SDK for interacting with Claude Code CLI")
                url.set("https://github.com/cyclingbits/claude-code-sdk-java")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("cyclingbits")
                        name.set("CyclingBits")
                        email.set("cyclingbitsai@gmail.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/cyclingbits/claude-code-sdk-java.git")
                    developerConnection.set("scm:git:ssh://github.com/cyclingbits/claude-code-sdk-java.git")
                    url.set("https://github.com/cyclingbits/claude-code-sdk-java")
                }
            }
        }
    }
}