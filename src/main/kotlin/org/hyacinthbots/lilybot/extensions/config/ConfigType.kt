package org.hyacinthbots.lilybot.extensions.config

/**
 * The enum of the types of Config available in LilyBot.
 *
 * @since 4.0.0
 */
enum class ConfigType {
	/** The entry for the support config. */
	@Deprecated("Support configuration is deprecated and will be removed in a future update.")
	SUPPORT,

	/** The entry for the moderation config. */
	MODERATION,

	/** The entry for the logging config. */
	LOGGING,

	/** The entry for the utility config. */
	UTILITY,

	/** The entry for all config types. */
	ALL
}
