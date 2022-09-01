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
version = "4.0.0-alpha"

repositories {
    mavenCentral()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
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
    implementation(libs.kord.extensions.phishing)
    implementation(libs.kord.extensions.mappings)
    implementation(libs.kord.extensions.pluralkit)
    implementation(libs.kord.extensions.unsafe)

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
    mainClass.set("org.hyacinthbots.lilybot.LilyBotKt")
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            languageVersion = "1.7"
            incremental = true
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    processResources {
        from("docs/commanddocs.toml")
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "org.hyacinthbots.lilybot.LilyBotKt"
            )
        }
    }

    wrapper {
        /*
         * To update the gradle wrapper version, change
         * the `gradleVersion` below and in `gradle-wrapper.properties`
         *
         * Then run the following command twice to update the gradle
         * scripts suitably
         * `./gradlew wrapper`
         */
        gradleVersion = "7.5.1"
        distributionType = Wrapper.DistributionType.BIN
    }
}

detekt {
    buildUponDefaultConfig = true
    config = files("$rootDir/detekt.yml")

    autoCorrect = true
}
