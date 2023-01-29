rootProject.name = "LilyBot"

dependencyResolutionManagement {
	@Suppress("UnstableApiUsage")
	versionCatalogs {
		create("libs") {
			from(files("libs.versions.toml"))
		}
	}
}
