import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	application

	alias(libs.plugins.kotlin)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.shadow)
	alias(libs.plugins.detekt)
	alias(libs.plugins.git.hooks)
	alias(libs.plugins.grgit)
	alias(libs.plugins.blossom)
}

group = "org.hyacinthbots.lilybot"
version = "4.9.0"

val className = "org.hyacinthbots.lilybot.LilyBotKt"

repositories {
	mavenCentral()

	maven {
		name = "Sonatype Snapshots (Legacy)"
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}

	maven {
		name = "Sonatype Snapshots"
		url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
	}

	maven {
		name = "Kord Extensions (Snapshots)"
		url = uri("https://snapshots-repo.kordex.dev")
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
	implementation(libs.kord.extensions.welcome)

	implementation(libs.kotlin.stdlib)

	// Logging dependencies
	implementation(libs.logback)
	implementation(libs.logging)

	// Github API
	implementation(libs.github.api)

	// KMongo
	implementation(libs.kmongo)

	implementation(libs.docgenerator)
}

application {
	mainClass.set(className)
}

gitHooks {
	setHooks(
		mapOf("pre-commit" to "detekt")
	)
}

tasks {
	withType<KotlinCompile> {
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget("21"))
			languageVersion.set(KotlinVersion.fromVersion(libs.plugins.kotlin.get().version.requiredVersion.substringBeforeLast(".")))
			incremental = true
			freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
		}
	}

	java {  // Should match the Kotlin compiler options ideally
		sourceCompatibility = JavaVersion.toVersion("21")
		targetCompatibility = JavaVersion.toVersion("21")
	}

	jar {
		manifest {
			attributes("Main-Class" to className)
		}
	}

	wrapper {
		// To update the gradle wrapper version run `./gradlew wrapper --gradle-version=<NEW_VERSION>`
		distributionType = Wrapper.DistributionType.BIN
	}
}

detekt {
	buildUponDefaultConfig = true
	config.setFrom("$rootDir/detekt.yml")

	autoCorrect = true
}

sourceSets {
	main {
		blossom {
			kotlinSources {
				property("build_id", grgit.head().abbreviatedId)
				property("version", project.version.toString())
			}
		}
	}
}
