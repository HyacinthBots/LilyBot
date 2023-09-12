package org.hyacinthbots.lilybot.internal

/**
 * This object stores the constants for the Build ID and version of Lily in her current state
 */
object BuildInfo {
	/** The short commit hash of this build of Lily. */
	const val BUILD_ID: String = "{{ build_id }}"

	/** The current version of LilyBot. */
	const val LILY_VERSION: String = "{{ version }}"
}
