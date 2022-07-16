package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data of guild tags, which are stored in the database.
 *
 * @param guildId The ID of the guild the tag will be saved for
 * @param name The named identifier of the tag
 * @param tagTitle The title of the created tag
 * @param tagValue The value of the created tag
 * @param tagAppearance The appearance of the created tag
 * @since 3.1.0
 */
@Serializable
data class TagsData(
	val guildId: Snowflake,
	val name: String,
	val tagTitle: String,
	val tagValue: String,
	val tagAppearance: String
)
