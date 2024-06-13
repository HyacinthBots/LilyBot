package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ReminderRestrictionData
import org.koin.core.component.inject
import org.litote.kmongo.eq

class ReminderRestrictionCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ReminderRestrictionData>()

	suspend inline fun getRestrictionData(inputGuildId: Snowflake): ReminderRestrictionData? =
		collection.findOne(ReminderRestrictionData::guildId eq inputGuildId)

	suspend inline fun addRestriction(data: ReminderRestrictionData) {
		if (getRestrictionData(data.guildId) != null) {
			collection.updateOne(ReminderRestrictionData::guildId eq data.guildId, data)
		} else {
			collection.insertOne(data)
		}
	}

	suspend inline fun removeRestriction(inputGuildId: Snowflake) =
		collection.deleteOne(ReminderRestrictionData::guildId eq inputGuildId)

	suspend inline fun addWhitelistedChannel(inputGuildId: Snowflake, inputChannelId: Snowflake): Boolean {
		val restrictionData = getRestrictionData(inputGuildId)
		if (restrictionData != null) {
			val channels = restrictionData.whitelistedChannels
			channels?.add(inputChannelId)
			collection.updateOne(
				ReminderRestrictionData::guildId eq inputGuildId,
				ReminderRestrictionData(restrictionData.guildId, restrictionData.restrict, channels)
			)
			return true
		} else {
			return false
		}
	}

	suspend inline fun removeWhitelistedChannel(inputGuildId: Snowflake, inputChannelId: Snowflake): Boolean {
		val restrictionData = getRestrictionData(inputGuildId)
		if (restrictionData != null) {
			val channels = restrictionData.whitelistedChannels
			channels?.remove(inputChannelId)
			collection.updateOne(
				ReminderRestrictionData::guildId eq inputGuildId,
				ReminderRestrictionData(restrictionData.guildId, restrictionData.restrict, channels)
			)
			return true
		} else {
			return false
		}
	}
}
