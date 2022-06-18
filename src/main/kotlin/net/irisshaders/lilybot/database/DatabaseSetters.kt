package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.DatabaseGetters.getWarn
import org.litote.kmongo.eq

// TODO Organise into A-Z
object DatabaseSetters {

	/**
	 * Adds the given [supportConfig] to the database.
	 * @param supportConfig The new config values for the support module you want to set.
	 * @author Miss Corruption
	 * @since 4.0.0
	 */
	suspend inline fun setSupportConfig(supportConfig: DatabaseTables.SupportConfigData) {
		val collection = configDatabase.getCollection<DatabaseTables.SupportConfigData>()
		collection.deleteOne(DatabaseTables.SupportConfigData::guildId eq supportConfig.guildId)
		collection.insertOne(supportConfig)
	}

	suspend inline fun setModerationConfig(moderationConfig: DatabaseTables.ModerationConfigData) {
		val collection = configDatabase.getCollection<DatabaseTables.ModerationConfigData>()
		collection.deleteOne(DatabaseTables.ModerationConfigData::guildId eq moderationConfig.guildId)
		collection.insertOne(moderationConfig)
	}

	suspend inline fun setLoggingConfig(loggingConfig: DatabaseTables.LoggingConfigData) {
		val collection = configDatabase.getCollection<DatabaseTables.LoggingConfigData>()
		collection.deleteOne(DatabaseTables.LoggingConfigData::guildId eq loggingConfig.guildId)
		collection.insertOne(loggingConfig)
	}

	/**
	 * Updates the number of points the provided [inputUserId] has in the provided [inputGuildId] in the database.
	 *
	 * @param inputUserId The ID of the user to get the point value for.
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @param remove Remove a warn strike, or add a warn strike.
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun setWarn(inputUserId: Snowflake, inputGuildId: Snowflake, remove: Boolean) {
		val currentStrikes = getWarn(inputUserId, inputGuildId)?.strikes ?: 0
		val collection = database.getCollection<DatabaseTables.WarnData>()
		collection.deleteOne(DatabaseTables.WarnData::userId eq inputUserId, DatabaseTables.WarnData::guildId eq inputGuildId)
		collection.insertOne(
			DatabaseTables.WarnData(
				inputUserId,
				inputGuildId,
				if (!remove) currentStrikes.plus(1) else currentStrikes.minus(1)
			)
		)
	}

	/**
	 * Add the given [inputRoles] to the database entry for the role menu for the provided [inputMessageId],
	 * [inputChannelId], and [inputGuildId].
	 *
	 * @param inputMessageId The ID of the message the role menu is in.
	 * @param inputChannelId The ID of the channel the role menu is in.
	 * @param inputGuildId The ID of the guild the role menu is in.
	 * @param inputRoles The [MutableList] of [Snowflake]s representing the role IDs for the role menu.
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun setRoleMenu(
		inputMessageId: Snowflake,
		inputChannelId: Snowflake,
		inputGuildId: Snowflake,
		inputRoles: MutableList<Snowflake>
	) {
		val newRoleMenu = DatabaseTables.RoleMenuData(inputMessageId, inputChannelId, inputGuildId, inputRoles)
		val collection = database.getCollection<DatabaseTables.RoleMenuData>()
		collection.deleteOne(DatabaseTables.RoleMenuData::messageId eq inputMessageId)
		collection.insertOne(newRoleMenu)
	}

	/**
	 * Add the given [newStatus] to the database.
	 *
	 * @param newStatus The new status you wish to set
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun setStatus(newStatus: String) {
		val collection = database.getCollection<DatabaseTables.StatusData>()
		collection.deleteOne(DatabaseTables.StatusData::key eq "LilyStatus")
		collection.insertOne(DatabaseTables.StatusData("LilyStatus", newStatus))
	}

	/**
	 * Adds a tag to the database, using the provided parameters.
	 *
	 * @param inputGuildId The ID of the guild the command was run in. Used to keep things guild specific.
	 * @param name The named identifier of the tag being created.
	 * @param tagTitle The title of the tag being created.
	 * @param tagValue The contents of the tag being created.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend inline fun setTag(inputGuildId: Snowflake, name: String, tagTitle: String, tagValue: String) {
		val collection = database.getCollection<DatabaseTables.TagsData>()
		collection.insertOne(DatabaseTables.TagsData(inputGuildId, name, tagTitle, tagValue))
	}

	/**
	 * Add or update the ownership of the given [inputThreadId] to the given [newOwnerId].
	 *
	 * @param inputThreadId The ID of the thread you wish to update or set the owner for
	 * @param newOwnerId The new owner of the thread
	 * @param preventArchiving Whether to stop the thread from being archived or not
	 *
	 * @return null or the thread owner's ID
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun setThreadOwner(
		inputThreadId: Snowflake,
		newOwnerId: Snowflake,
		preventArchiving: Boolean = false
	) {
		val collection = database.getCollection<DatabaseTables.ThreadData>()
		collection.deleteOne(DatabaseTables.ThreadData::threadId eq inputThreadId)
		collection.insertOne(DatabaseTables.ThreadData(inputThreadId, newOwnerId, preventArchiving))
	}

	/**
	 * Adds the time Lily bot left a guild with a config.
	 *
	 * @param inputGuildId The guild the event took place in
	 * @param time The current time
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend inline fun setLeaveTime(inputGuildId: Snowflake, time: Instant) {
		val collection = database.getCollection<DatabaseTables.GuildLeaveTimeData>()
		collection.insertOne(DatabaseTables.GuildLeaveTimeData(inputGuildId, time))
	}

	/**
	 * Stores a channel ID as input by the user, in the database, with it's corresponding guild, allowing us to find
	 * the channel later.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel that is being set as a gallery channel
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun setGalleryChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) {
		val collection = database.getCollection<DatabaseTables.GalleryChannelData>()
		collection.insertOne(DatabaseTables.GalleryChannelData(inputGuildId, inputChannelId))
	}

	/**
	 * Stores a reminder in the database.
	 *
	 * @param initialSetTime The time the reminder was set
	 * @param inputGuildId The ID of the guild the reminder was set in
	 * @param inputUserId The ID of the user that would like to be reminded
	 * @param inputChannelId The ID of the channel the reminder was set in
	 * @param remindTime The time the user would like to be reminded at
	 * @param originalMessageUrl The URL to the original message that set the reminder
	 * @param customMessage A custom message to attach to the reminder
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	suspend inline fun setReminder(
		initialSetTime: Instant,
		inputGuildId: Snowflake,
		inputUserId: Snowflake,
		inputChannelId: Snowflake,
		remindTime: Instant,
		originalMessageUrl: String,
		customMessage: String?,
		repeating: Boolean,
		id: Int
	) {
		val collection = database.getCollection<DatabaseTables.RemindMeData>()
		collection.insertOne(
			DatabaseTables.RemindMeData(
				initialSetTime,
				inputGuildId,
				inputUserId,
				inputChannelId,
				remindTime,
				originalMessageUrl,
				customMessage,
				repeating,
				id
			)
		)
	}
}
