import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileOutputStream
import java.util.*

plugins {
    application

    val kotlinVersion = "1.7.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("com.github.jakemarsden.git-hooks") version "0.0.2"
    id("org.ajoberstar.grgit") version "5.0.0"
    id("net.kyori.blossom") version "1.3.1"
}

group = "org.hyacinthbots.lilybot"
version = "4.6.2"

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
		name = "Sonatype Releases S01"
		url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
	}

	maven {
		name = "Sonatype Snapshots S01"
		url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
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

    implementation(libs.kord.extensions.core)
    implementation(libs.kord.extensions.phishing)
    implementation(libs.kord.extensions.pluralkit)
    implementation(libs.kord.extensions.unsafe)

    implementation(libs.kotlin.stdlib)

    // Logging dependencies
    implementation(libs.logback)
    implementation(libs.logging)

    // Github API
    implementation(libs.github.api)

    // KMongo
    implementation(libs.kmongo)

	  // Cozy's welcome module
	  implementation(libs.cozy.welcome)

    implementation(libs.dma)
    implementation(libs.docgenerator)
    //implementation(files("./build/doc-generator-0.1.0.jar"))
}

application {
    mainClass.set("org.hyacinthbots.lilybot.LilyBotKt")
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

val generatedVersionDir = "$buildDir/generated-version"

sourceSets {
    main {
        kotlin {
            output.dir(generatedVersionDir)
        }
    }
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
         * the `gradleVersion` below
         *
         * Then run the following command twice to update the gradle
         * scripts suitably
         * `./gradlew wrapper`
         */
        gradleVersion = "8.0-rc-2"
        distributionType = Wrapper.DistributionType.BIN
    }

    register("generateVersionProperties") {
        doLast {
            val propertiesFile = file("$generatedVersionDir/version.properties")
            propertiesFile.parentFile.mkdirs()
            val properties = Properties()
            properties.setProperty("version", "$version")
            properties.store(FileOutputStream(propertiesFile), null)
        }
    }

    named("processResources") {
        dependsOn("generateVersionProperties")
    }
}

detekt {
    buildUponDefaultConfig = true
    config = files("$rootDir/detekt.yml")

    autoCorrect = true
}

blossom {
    replaceToken("@version@", grgit.head().abbreviatedId)
}
