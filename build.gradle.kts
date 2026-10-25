import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
    throw GradleException(
        "Java 21 or newer is required to run Gradle for this workspace (current: ${JavaVersion.current()}). Configure JAVA_HOME or the IDE Gradle JVM."
    )
}

allprojects {
    group = (findProperty("mod.group") ?: "org.grantismantis") as String
    version = buildString {
        append((findProperty("mod.version") ?: "0.0.0-dev") as String)
        append((findProperty("mod.channel_tag") ?: "") as String)
    }

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.blamejared.com")
        maven("https://api.modrinth.com/maven")
        maven("https://maven.parchmentmc.org")
    }
}

subprojects {
    plugins.withId("java") {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
