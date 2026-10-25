pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
        maven("https://maven.blamejared.com") { name = "BlameJared" }
        maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.8"
}

stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) {
            loaders.forEach { loader ->
                version("$version-$loader", version).buildscript = "build.$loader.gradle.kts"
            }
        }

        match("1.21.1", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.1", "fabric", "neoforge")
        match("26.2", "fabric", "neoforge")
        match("26.3", "fabric")

        vcsVersion = "1.21.11-fabric"
    }
}

rootProject.name = "Bits and Balance"
