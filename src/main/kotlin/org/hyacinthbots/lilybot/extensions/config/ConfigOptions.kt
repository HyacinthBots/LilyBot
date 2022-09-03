package org.hyacinthbots.lilybot.extensions.config

/**
 * The enum for the configuration options provided by LilyBot.
 *
 * @since 4.0.0
 */
enum class ConfigOptions {
	/** The option that stores whether the support config is enabled or not. */
	SUPPORT_ENABLED,

	/** The option that stores the support channel. */
	SUPPORT_CHANNEL,

	/** The option that stores the support team role. */
	SUPPORT_ROLE,

	/** The options that stores whether the moderation config is enabled or not.*/
	MODERATION_ENABLED,

	/** The option that stores the moderator role. */
	MODERATOR_ROLE,

	/** The option that stores the action logging channel. */
	ACTION_LOG,

	/** The option that stores whether to log a moderation action publicly. */
	LOG_PUBLICLY,

	/** The option that stores whether the logging config is enabled or not. */
	MESSAGE_LOGGING_ENABLED,

	/** The options that stores the message logging channel. */
	MESSAGE_LOG,

	/** The option that stores whether the member logging config is enabled or not. */
	MEMBER_LOGGING_ENABLED,

	/** The option that stores the member logging channel. */
	MEMBER_LOG,

	/** The option that stores whether log uploads are enabled or not. */
	LOG_UPLOADS_ENABLED,
}
