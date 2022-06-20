package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.DbTables.GuildLeaveTimeData
import org.litote.kmongo.eq

// TODO Organise into A-Z
object DbRemovers {
	/**
	 * Removes a channel ID from the gallery channel database.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel being removed
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun removeGalleryChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) {
		val collection = database.getCollection<DbTables.GalleryChannelData>()
		collection.deleteOne(
			DbTables.GalleryChannelData::channelId eq inputChannelId,
			DbTables.GalleryChannelData::guildId eq inputGuildId
		)
	}

	/**
	 * Removes old reminders from the Database.
	 *
	 * @param inputGuildId The ID of the guild the reminder was set in
	 * @param inputUserId The ID of the user the reminder was set by
	 * @param id The numerical ID of the reminder
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	suspend inline fun removeReminder(
		inputGuildId: Snowflake,
		inputUserId: Snowflake,
		id: Int
	) {
		val collection = database.getCollection<DbTables.RemindMeData>()
		collection.deleteOne(
			DbTables.RemindMeData::guildId eq inputGuildId,
			DbTables.RemindMeData::userId eq inputUserId,
			DbTables.RemindMeData::id eq id
		)
	}

	/**
	 * This function deletes a [GuildLeaveTimeData] from the database.
	 *
	 * @param inputGuildId The guild to delete the [GuildLeaveTimeData] for
	 *
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun removeLeaveTime(inputGuildId: Snowflake) {
		val collection = database.getCollection<GuildLeaveTimeData>()
		collection.deleteOne(GuildLeaveTimeData::guildId eq inputGuildId)
	}

	/**
	 * Clears all warn strikes for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun removeWarn(inputGuildId: Snowflake) {
		val collection = database.getCollection<DbTables.WarnData>()
		collection.deleteMany(DbTables.WarnData::guildId eq inputGuildId)
	}

	/**
	 * Remove the given [inputRoleId] from the database entry associated with the given [inputMessageId].
	 *
	 * @param inputMessageId The ID of the message the role menu is in.
	 * @param inputRoleId The ID of the role to remove from the menu.
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun removeRoleFromMenu(inputMessageId: Snowflake, inputRoleId: Snowflake) {
		val collection = database.getCollection<DbTables.RoleMenuData>()
		val roleMenu = collection.findOne(DbTables.RoleMenuData::messageId eq inputMessageId) ?: return

		roleMenu.roles.remove(inputRoleId)

		collection.deleteOne(DbTables.RoleMenuData::messageId eq inputMessageId)
		collection.insertOne(roleMenu)
	}

	/**
	 * Deletes the tag [name] from the [database].
	 *
	 * @param inputGuildId The guild the tag was created in.
	 * @param name The named identifier of the tag being deleted.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend inline fun removeTag(inputGuildId: Snowflake, name: String) {
		val collection = database.getCollection<DbTables.TagsData>()
		collection.deleteOne(DbTables.TagsData::guildId eq inputGuildId, DbTables.TagsData::name eq name)
	}

	/**
	 * Clears all tags for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.1.0
	 */
	suspend inline fun removeTags(inputGuildId: Snowflake) {
		val collection = database.getCollection<DbTables.TagsData>()
		collection.deleteMany(DbTables.TagsData::guildId eq inputGuildId)
	}

	/**
	 * This function deletes the ownership data stored in the database for the given [inputThreadId].
	 *
	 * @param inputThreadId The ID of the thread to delete
	 *
	 * @author henkelmax
	 * @since 3.2.2
	 */
	suspend inline fun removeThread(inputThreadId: Snowflake) {
		val collection = database.getCollection<DbTables.ThreadData>()
		collection.deleteOne(DbTables.ThreadData::threadId eq inputThreadId)
	}
}
