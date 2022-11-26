pluginManagement {
    plugins {
        val kotlinVersion = "1.7.21"
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        id("com.github.johnrengelman.shadow") version "7.1.2"

        id("io.gitlab.arturbosch.detekt") version "1.22.0"

        id("com.github.jakemarsden.git-hooks") version "0.0.2"
    }
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "LilyBot"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
