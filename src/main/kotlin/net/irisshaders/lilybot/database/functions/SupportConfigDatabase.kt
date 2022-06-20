package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database.tables.SupportConfigData
import org.litote.kmongo.eq

// TODO KDocs
object SupportConfigDatabase {
	suspend inline fun getSupportConfig(inputGuildId: Snowflake): SupportConfigData? {
		val collection = configDatabase.getCollection<SupportConfigData>()
		return collection.findOne(SupportConfigData::guildId eq inputGuildId)
	}

	/**
	 * Adds the given [supportConfig] to the database.
	 * @param supportConfig The new config values for the support module you want to set.
	 * @author Miss Corruption
	 * @since 4.0.0
	 */
	suspend inline fun setSupportConfig(supportConfig: SupportConfigData) {
		val collection = configDatabase.getCollection<SupportConfigData>()
		collection.deleteOne(SupportConfigData::guildId eq supportConfig.guildId)
		collection.insertOne(supportConfig)
	}

	// TODO Removal/Clear
}
