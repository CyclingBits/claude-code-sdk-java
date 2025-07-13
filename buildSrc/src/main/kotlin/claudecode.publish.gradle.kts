import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.vanniktech.maven.publish")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

extra["signingInMemoryKey"] = System.getenv("GPG_SIGNING_KEY")
extra["signingInMemoryKeyId"] = System.getenv("GPG_SIGNING_KEY_ID")
extra["signingInMemoryKeyPassword"] = System.getenv("GPG_SIGNING_PASSWORD")

configure<MavenPublishBaseExtension> {
    // Only sign if credentials are available
    if (!System.getenv("GPG_SIGNING_KEY").isNullOrEmpty()) {
        signAllPublications()
    }
    publishToMavenCentral()

    coordinates(project.group.toString(), project.name, project.version.toString())
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaJavadoc"),
            sourcesJar = true,
        )
    )

    pom {
        name.set("Claude Code SDK Java")
        description.set("A Kotlin/Java SDK for interacting with Claude Code CLI")
        url.set("https://github.com/cyclingbits/claude-code-sdk-java")

        licenses {
            license {
                name.set("MIT")
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