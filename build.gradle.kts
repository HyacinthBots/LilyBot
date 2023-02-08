import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
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
version = "4.6.3"

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
		name = "Sonatype Snapshots S01"
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
			languageVersion = libs.plugins.kotlin.get().version.requiredVersion.substringBeforeLast(".")
			incremental = true
			freeCompilerArgs = listOf(
				"-opt-in=kotlin.RequiresOptIn"
			)
		}
	}

	jar {
		manifest {
			attributes(
				"Main-Class" to "org.hyacinthbots.lilybot.LilyBotKt"
			)
		}
	}

	wrapper {
		// To update the gradle wrapper version run `./gradlew wrapper --gradle-version=<NEW_VERSION>`
		distributionType = Wrapper.DistributionType.BIN
	}
}

detekt {
	buildUponDefaultConfig = true
	config = files("$rootDir/detekt.yml")

	autoCorrect = true
}

blossom {
	replaceToken("@build_id@", grgit.head().abbreviatedId)
	replaceToken("@version@", project.version.toString())
}
