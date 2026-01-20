import dev.kordex.gradle.plugins.kordex.DataCollection
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	application
	distribution

	alias(libs.plugins.kotlin)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.detekt)
	alias(libs.plugins.git.hooks)
	alias(libs.plugins.grgit)
	alias(libs.plugins.blossom)
	alias(libs.plugins.kord.extensions.plugin)
	alias(libs.plugins.kord.extensions.i18n)
}

group = "org.hyacinthbots.lilybot"
version = "5.0.0"

val className = "org.hyacinthbots.lilybot.LilyBotKt"
val javaVersion = "21"

repositories {
	mavenCentral()

	maven {
		name = "Kord Extensions (Releases)"
		url = uri("https://releases-repo.kordex.dev")
	}

	maven {
		name = "Kord Extensions (Snapshots)"
		url = uri("https://snapshots-repo.kordex.dev")
	}

	maven {
		name = "Kord Extensions External (Releases)"
		url = uri("https://repo.kordex.dev/external-releases")
	}

	maven {
		name = "Kord Extensions External (Snapshots)"
		url = uri("https://repo.kordex.dev/external-releases")
	}

	maven {
		name = "Kord Snapshots"
		url = uri("https://repo.kordex.dev/snapshots")
	}

	maven {
		name = "Kord Mirror"
		url = uri("https://mirror-repo.kordex.dev")
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
	implementation(libs.groovy)
	implementation(libs.jansi)
	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)

	// Github API
	implementation(libs.github.api)

	// KMongo
	implementation(libs.kmongo)

	implementation(libs.docgenerator)

	implementation(libs.ktor.java)
}

distributions {
	main {
		distributionBaseName = project.name

		contents {
			from("LICENSE")
			exclude("README.md")
		}
	}
}

kordEx {
	addDependencies = false
	addRepositories = false
	kordExVersion = libs.versions.kord.extensions
	ignoreIncompatibleKotlinVersion = true

	bot {
		dataCollection(DataCollection.None)
	}
}

i18n {
	bundle("lilybot.strings", "lilybot.i18n")
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
			jvmTarget.set(JvmTarget.fromTarget(javaVersion))
			languageVersion.set(
				KotlinVersion.fromVersion(
					libs.plugins.kotlin.get().version.requiredVersion.substringBeforeLast(".")
				)
			)
			incremental = true
			freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
		}
	}

	java {  // Should match the Kotlin compiler options ideally
		sourceCompatibility = JavaVersion.toVersion(javaVersion)
		targetCompatibility = JavaVersion.toVersion(javaVersion)
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
