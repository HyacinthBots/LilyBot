package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.TagsData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object contains the functions for interacting with the [Tags Database][TagsData]. This object has functions for
 * getting tags, getting all tags, adding tags, and removing a tag or tags.
 *
 * @since 4.0.0
 * @see getTag
 * @see getAllTags
 * @see setTag
 * @see removeTag
 * @see removeTags
 */
class TagsCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<TagsData>()

	/**
	 * Gets the given tag using it's [name] and returns its [TagsData]. If the tag does not exist.
	 * it will return null
	 *
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @param name The named identifier of the tag.
	 * @return null or the result from the database.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend inline fun getTag(inputGuildId: Snowflake, name: String): TagsData? =
		collection.findOne(TagsData::guildId eq inputGuildId, TagsData::name eq name)

	/**
	 * Gets all tags in the given [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild.
	 * @return A [List] of tags for the specified [inputGuildId].
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend inline fun getAllTags(inputGuildId: Snowflake): List<TagsData> =
		collection.find(TagsData::guildId eq inputGuildId).toList()

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
	suspend inline fun setTag(
        inputGuildId: Snowflake,
        name: String,
        tagTitle: String,
        tagValue: String,
        tagAppearance: String
    ) =
		collection.insertOne(TagsData(inputGuildId, name, tagTitle, tagValue, tagAppearance))

	/**
	 * Deletes the tag [name] from the [Database.mainDatabase].
	 *
	 * @param inputGuildId The guild the tag was created in.
	 * @param name The named identifier of the tag being deleted.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend inline fun removeTag(inputGuildId: Snowflake, name: String) =
		collection.deleteOne(TagsData::guildId eq inputGuildId, TagsData::name eq name)

	/**
	 * Clears all tags for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.1.0
	 */
	suspend inline fun removeTags(inputGuildId: Snowflake) =
		collection.deleteMany(TagsData::guildId eq inputGuildId)
}
