plugins {
    id("claudecode.java")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":claude-code-sdk-java"))
}

tasks.withType<JavaCompile>().configureEach {
    // Allow using more modern APIs, like `List.of` and `Map.of`, in examples.
    options.release.set(11)
}

application {
    mainClass.set("net.cyclingbits.claudecode.example.MainKt")
}