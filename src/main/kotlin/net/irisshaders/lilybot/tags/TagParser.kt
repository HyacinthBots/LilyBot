/**
 * This code was taken, with permission, from <https://github.com/QuiltServerTools/axolotl>
 * This code is Licensed under the MIT license (<https://mit-license.org/>) unlike the rest of hte project
 * @author Tom_The_Geek
 */
package net.irisshaders.lilybot.tags

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.OptionalInt
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private const val SEPARATOR = "---"

private val format = Yaml(configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property))
private val logger = KotlinLogging.logger { }

class TagParser {
    private val tags: MutableMap<String, Tag> = HashMap()

    fun loadTags(dir: Path) {
        logger.debug { "Loading tags from $dir..." }

        for (path in Files.walk(dir)) {
            if (path.isRegularFile() && path.fileName.toString().endsWith(".ytag")) {
                val tagName = path.fileName.toString().substringBefore(".ytag")

                if (tags.containsKey(tagName)) {
                    logger.error { "There is already a duplicate tag named $tagName (found at $path)" }

                    continue
                }

                @Suppress("TooGenericExceptionCaught")
                val tag = try {
                    parseTag(tagName, path.readText(charset = Charsets.UTF_8))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load tag $path" }

                    continue
                }

                tags[tagName] = tag
            }
        }

        resolveAliasTags()

        logger.info { "Loaded ${tags.size} tags!" }
    }

    fun reloadTags(dir: Path): Int {
        logger.info { "Reloading tags..." }
        tags.clear()

        loadTags(dir)

        return tags.size
    }

    fun getTag(name: String): Tag? = tags[name]

    private fun resolveAliasTags() {
        val invalidTags = ArrayList<String>()

        for ((name, tag) in tags) {
            if (tag.data is TagData.AliasTagData) {
                if (!tags.containsKey(tag.data.target)) {
                    logger.warn { "Alias tag $name points to missing tag ${tag.data.target}" }

                    invalidTags += name
                } else {
                    val targetTag = tags[tag.data.target]!!

                    if (targetTag.data is TagData.AliasTagData) {
                        logger.warn { "Alias tag should not point to another alias tag ($name -> ${tag.data.target})" }

                        invalidTags += name
                    } else {
                        tag.data.targetTag = targetTag
                    }
                }
            }
        }
        if (invalidTags.isNotEmpty()) {
            logger.warn { "Ignoring ${invalidTags.size} invalid tags" }

            invalidTags.forEach(this.tags::remove)
        }
    }

    private fun parseTag(name: String, content: String): Tag {
        if (!content.startsWith(SEPARATOR)) {
            throw IllegalArgumentException("no front matter")
        }

        val remaining = content.substringAfter(SEPARATOR)

        if (!remaining.contains(SEPARATOR)) {
            throw IllegalArgumentException("front matter is not closed")
        }

        val yaml = remaining.substringBefore(SEPARATOR).trim()
        val markdown = remaining.substringAfter(SEPARATOR).trim()
        val tagData = format.decodeFromString<TagData>(yaml)

        // Validate tag data
        when (tagData) {
            is TagData.AliasTagData -> if (markdown.isNotEmpty()) {
                throw IllegalArgumentException("alias tag should not have markdown content")
            }

            is TagData.TextTagData -> if (markdown.isEmpty()) {
                throw IllegalArgumentException("text tag is missing markdown content")
            }

            is TagData.EmbedTagData -> {
                if (tagData.embed.color !is OptionalInt.Missing) {
                    throw IllegalArgumentException("embed colour should be set at the root level not in the embed data")
                }

                if (tagData.embed.description !is Optional.Missing) {
                    throw IllegalArgumentException("embed description should be set in the markdown content")
                }
            }
        }

        return Tag(name, tagData, markdown)
    }
}

// Used to test the tag parser
fun main() {
    val parser = TagParser()
    parser.loadTags(Paths.get(".", "tags"))
}
