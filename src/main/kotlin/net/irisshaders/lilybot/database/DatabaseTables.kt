package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// TODO Organise into A-Z
object DatabaseTables {
	/**
	 * The data for image channels in a guild.
	 *
	 * @param guildId The ID of the guild the image channel is for
	 * @param channelId The ID of the image channel being set
	 * @since 3.3.0
	 */
	@Serializable
	data class GalleryChannelData(
		val guildId: Snowflake,
		val channelId: Snowflake
	)

	/**
	 * The data for when Lily leaves a guild.
	 *
	 * @param guildId The ID of the guild Lily left
	 * @param guildLeaveTime The [Instant] that Lily left the guild
	 * @since 3.2.0
	 */
	@Serializable
	data class GuildLeaveTimeData(
		val guildId: Snowflake,
		val guildLeaveTime: Instant
	)

	/**
	 * The data for reminders set by users.
	 *
	 * @param initialSetTime The time the reminder was set
	 * @param guildId The ID of the guild the reminder was set in
	 * @param userId The ID of the user that would like to be reminded
	 * @param channelId The ID of the channel the reminder was set in
	 * @param remindTime The time the user would like to be reminded at
	 * @param originalMessageUrl The URL to the original message that set the reminder
	 * @param customMessage A custom message to attach to the reminder
	 * @param repeating Whether the reminder should repeat
	 * @param id The numerical ID of the reminder
	 *
	 * @since 3.3.2
	 */
	@Serializable
	data class RemindMeData(
		val initialSetTime: Instant,
		val guildId: Snowflake,
		val userId: Snowflake,
		val channelId: Snowflake,
		val remindTime: Instant,
		val originalMessageUrl: String,
		val customMessage: String?,
		val repeating: Boolean,
		val id: Int
	)

	/**
	 * The data for role menus.
	 *
	 * @param messageId The ID of the message of the role menu
	 * @param channelId The ID of the channel the role menu is in
	 * @param guildId The ID of the guild the role menu is in
	 * @param roles A [MutableList] of the role IDs associated with this role menu.
	 * @since 3.4.0
	 */
	@Serializable
	data class RoleMenuData(
		val messageId: Snowflake,
		val channelId: Snowflake,
		val guildId: Snowflake,
		val roles: MutableList<Snowflake>
	)

	/**
	 * The data for the bot status.
	 *
	 * @param key This is just so we can find the status and should always be set to "LilyStatus"
	 * @param status The string value that will be seen in the bots presence
	 * @since 3.0.0
	 */
	@Serializable
	data class StatusData(
		val key: String,
		val status: String
	)

	/**
	 * The data of guild tags, which are stored in the database.
	 *
	 * @param guildId The ID of the guild the tag will be saved for
	 * @param name The named identifier of the tag
	 * @param tagTitle The title of the created tag
	 * @param tagValue The value of the created tag
	 * @since 3.1.0
	 */
	@Serializable
	data class TagsData(
		val guildId: Snowflake,
		val name: String,
		val tagTitle: String,
		val tagValue: String
	)

	/**
	 * The data for threads.
	 *
	 * @param threadId The ID of the thread
	 * @param ownerId The ID of the thread's owner
	 * @param preventArchiving Whether to stop the thread from being archived or not
	 * @since 3.2.0
	 */
	@Serializable
	@Suppress("DataClassShouldBeImmutable")
	data class ThreadData(
		val threadId: Snowflake,
		val ownerId: Snowflake,
		var preventArchiving: Boolean = false
	)

	/**
	 * The data for warnings in guilds.
	 *.
	 * @param userId The ID of the user with warnings
	 * @param guildId The ID of the guild they received the warning in
	 * @param strikes The amount of strikes they have received
	 * @since 3.0.0
	 */
	@Serializable
	data class WarnData(
		val userId: Snowflake,
		val guildId: Snowflake,
		val strikes: Int
	)

	/**
	 * The data for support configuration.
	 *
	 * @param guildId The ID of the guild the config is for
	 * @param enabled If the support module is enabled or not
	 * @param channel The ID of the support channel for the guild
	 * @param team The ID of the support team for the guild
	 * @param message The support message as a string, nullable
	 * @since 4.0.0
	 */
	@Serializable
	data class SupportConfigData(
		val guildId: Snowflake,
		val enabled: Boolean,
		val channel: Snowflake,
		val team: Snowflake,
		val message: String?
	)

	/**
	 * The data for moderation configuration.
	 *
	 * @param guildId The ID of the guild the config is for
	 * @param enabled If the support module is enabled or not
	 * @param channel The ID of the action log for the guild
	 * @param team The ID of the moderation role for the guild
	 * @since 4.0.0
	 */
	@Serializable
	data class ModerationConfigData(
		val guildId: Snowflake,
		val enabled: Boolean,
		val channel: Snowflake,
		val team: Snowflake,
	)

	/**
	 * The data for moderation configuration.
	 *
	 * @param guildId The ID of the guild the config is for
	 * @param enableMessageLogs If edited and deleted messages should be logged
	 * @param messageChannel The channel to send message logs to
	 * @param enableJoinLogs If joining and leaving users should be logged
	 * @param joinChannel The channel to send join logs to
	 * @since 4.0.0
	 */
	@Serializable
	data class LoggingConfigData(
		val guildId: Snowflake,
		val enableMessageLogs: Boolean,
		val messageChannel: Snowflake,
		val enableJoinLogs: Boolean,
		val joinChannel: Snowflake,
	)
}
