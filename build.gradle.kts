import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.github.johnrengelman.shadow")
}

group = "net.irisshaders.lilybot"
version = "1.0"

repositories {
    // You can remove this if you're not testing locally-installed KordEx builds
    mavenLocal()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {

    implementation(libs.kord.extensions)
    implementation(libs.kotlin.stdlib)

    // Logging dependencies
    implementation(libs.groovy)
    implementation(libs.logback)
    implementation(libs.logging)

    // Tags
    implementation(libs.kotlinx.serialization)
    implementation(libs.kaml)
    implementation(libs.jgit)
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClassName = "net.irisshaders.lilybot.LilyBotkt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"

    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.jar {
    manifest {
        attributes(
                "Main-Class" to "net.irisshaders.lilybot.LilyBotKt"
        )
    }
}

// This is to fix an issue with pushing that I cannot seem to fix.
tasks.create<Delete>("detekt") {

}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}