pluginManagement {
    plugins {
        // Update this in libs.version.toml when you change it here
        kotlin("jvm") version "1.6.10"

        id("com.github.johnrengelman.shadow") version "7.1.2"
    }
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "LilyBot"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
