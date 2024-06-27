package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ActionData
import org.hyacinthbots.lilybot.database.entities.ModerationActionData
import org.hyacinthbots.lilybot.extensions.moderation.ModerationAction
import org.koin.core.component.inject
import org.litote.kmongo.eq

// TODO DOCS
class ModerationActionCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ModerationActionData>()

	suspend inline fun addAction(
		action: ModerationAction,
		guildId: Snowflake,
		targetUserId: Snowflake,
		data: ActionData,
		ignore: Boolean = false
	) =
		collection.insertOne(ModerationActionData(action, guildId, targetUserId, data, ignore))

	suspend inline fun removeAction(type: ModerationAction, guildId: Snowflake, targetUserId: Snowflake) =
		collection.deleteOne(
			ModerationActionData::actionType eq type,
			ModerationActionData::guildId eq guildId,
			ModerationActionData::targetUserId eq targetUserId
		)

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

	suspend inline fun declareActionToIgnore(type: ModerationAction, guildId: Snowflake, targetUserId: Snowflake) =
		addAction(type, guildId, targetUserId, ActionData(null, null, null, null, null, null, null), true)

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
