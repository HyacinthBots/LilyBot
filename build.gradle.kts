import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")
    id("com.github.jakemarsden.git-hooks")
}

group = "net.irisshaders.lilybot"
version = "3.4.3"

val javaVersion = 17
val kotlinVersion = "${libs.versions.kotlin.get().split(".")[0]}.${libs.versions.kotlin.get().split(".")[1]}"

repositories {
    mavenCentral()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }

    maven {
        name = "QuiltMC (Releases)"
        url = uri("https://maven.quiltmc.org/repository/release/")
    }

    maven {
        name = "QuiltMC (Snapshots)"
        url = uri("https://maven.quiltmc.org/repository/snapshot/")
    }

    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    detektPlugins(libs.detekt)

    implementation(libs.kord.extensions)
    implementation(libs.kord.phishing)
    implementation(libs.kord.mappings)

    // UnsafeAPI KordEx
    implementation(libs.kord.unsafe)

    implementation(libs.kotlin.stdlib)

    // Logging dependencies
    implementation(libs.groovy)
    implementation(libs.logback)
    implementation(libs.logging)

    // Github API
    implementation(libs.github.api)

    // KMongo
    implementation(libs.kmongo)

    // TOML Reader
    implementation(libs.koma)
}

application {
    mainClass.set("net.irisshaders.lilybot.LilyBotKt")
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            languageVersion = kotlinVersion
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
            incremental = true
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }

    processResources {
        from("docs/commanddocs.toml")
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "net.irisshaders.lilybot.LilyBotKt"
            )
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config = files("$rootDir/detekt.yml")

    autoCorrect = true
}
