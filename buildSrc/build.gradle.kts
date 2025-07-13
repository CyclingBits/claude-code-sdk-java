plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
    id("com.vanniktech.maven.publish") version "0.33.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.2.0")

    implementation("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
}