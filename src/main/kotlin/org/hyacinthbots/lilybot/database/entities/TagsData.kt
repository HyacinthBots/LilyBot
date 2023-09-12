package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.hyacinthbots.lilybot.database.Collection

/**
 * The data of guild tags, which are stored in the database.
 *
 * @property guildId The ID of the guild the tag will be saved for
 * @property name The named identifier of the tag
 * @property tagTitle The title of the created tag
 * @property tagValue The value of the created tag
 * @property tagAppearance The appearance of the created tag
 * @since 3.1.0
 */
@Serializable
data class TagsData(
	val guildId: Snowflake,
	val name: String,
	val tagTitle: String,
	val tagValue: String,
	val tagAppearance: String
) {
	companion object : Collection("tagsData")
}
