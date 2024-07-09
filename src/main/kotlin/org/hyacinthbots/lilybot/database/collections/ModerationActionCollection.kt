package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ActionData
import org.hyacinthbots.lilybot.database.entities.ModerationActionData
import org.hyacinthbots.lilybot.extensions.moderation.ModerationAction
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains the function for interacting with the [Moderation Action Database][ModerationActionData]. This
 * class contains functions for getting, setting, removing and ignoring actions
 *
 * @since 5.0.0
 * @see addAction
 * @see removeAction
 * @see getAction
 * @see declareActionToIgnore
 * @see shouldIgnoreAction
 */
class ModerationActionCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ModerationActionData>()

	/**
	 * Adds an action that occurred.
	 *
	 * @param action The type of action you're adding
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user this action happened to
	 * @param data The [ActionData] for the action
	 * @param ignore Whether to ignore the action or not. Defaults to false
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun addAction(
		action: ModerationAction,
		guildId: Snowflake,
		targetUserId: Snowflake,
		data: ActionData,
		ignore: Boolean = false
	) = collection.insertOne(ModerationActionData(action, guildId, targetUserId, data, ignore))

	/**
	 * Removes an action that occurred.
	 *
	 * @param type The type of action you're removing
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user this action happened to
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun removeAction(type: ModerationAction, guildId: Snowflake, targetUserId: Snowflake) =
		collection.deleteOne(
			ModerationActionData::actionType eq type,
			ModerationActionData::guildId eq guildId,
			ModerationActionData::targetUserId eq targetUserId
		)

	/**
	 * Gets an action that occurred.
	 *
	 * @param type The type of action you're looking for
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user this action happened to
	 * @return The [data][ModerationActionData] for the event. Can be null if there is no action.
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun getAction(
		type: ModerationAction,
        guildId: Snowflake,
        targetUserId: Snowflake
	): ModerationActionData? =
		collection.findOne(
			ModerationActionData::actionType eq type,
			ModerationActionData::guildId eq guildId,
			ModerationActionData::targetUserId eq targetUserId
		)

	/**
	 * Sets an action as ignored. Convenience function more than anything
	 *
	 * @param type The type of action you're looking for
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user this action happened to
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun declareActionToIgnore(type: ModerationAction, guildId: Snowflake, targetUserId: Snowflake) =
		addAction(type, guildId, targetUserId, ActionData(null, null, null, null, null, null, null), true)

	/**
	 * Checks if an action should be ignored or not. Convenience function more than anything.
	 *
	 * @param type The type of action you're looking for
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user this action happened to
	 * @return True if the action should be ignored, false if otherwise
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun shouldIgnoreAction(
		type: ModerationAction,
		guildId: Snowflake,
		targetUserId: Snowflake
	): Boolean? =
		collection.findOne(
			ModerationActionData::actionType eq type,
			ModerationActionData::guildId eq guildId,
			ModerationActionData::targetUserId eq targetUserId
		)?.ignore
}
