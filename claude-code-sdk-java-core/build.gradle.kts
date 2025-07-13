plugins {
    id("claudecode.kotlin")
    id("claudecode.publish")
}

dependencies {
    // Core dependencies
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    implementation("com.squareup.okio:okio:3.10.2")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
    
    // Fix JUnit platform version alignment
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
}
