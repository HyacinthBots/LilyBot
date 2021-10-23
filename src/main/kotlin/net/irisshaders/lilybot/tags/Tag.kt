/**
 * This code was taken, with permission, from <https://github.com/QuiltServerTools/axolotl>
 * This code is Licensed under the MIT license (<https://mit-license.org/>) unlike the rest of hte project
 * @author Tom_The_Geek
 */
package net.irisshaders.lilybot.tags

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_WHITE
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import dev.kord.common.Color
import dev.kord.common.kColor
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
sealed class TagData {
    @Transient open val type: String = "unknown"

    @Serializable
    @SerialName("alias")
    class AliasTagData(val target: String) : TagData() {
        @Transient override val type = "alias"

        // This is transient so that it is not expected to be loaded from the data
        @Transient var targetTag: Tag? = null
    }

    @Serializable
    @SerialName("text")
    class TextTagData : TagData() {
        @Transient override val type = "text"
    }

    @Serializable
    @SerialName("embed")
    class EmbedTagData(
        val embed: EmbedData,
        val colour: String? = null,
    ) : TagData() {
        @Transient override val type = "embed"
    }
}

data class Tag(
    val name: String,
    val data: TagData,
    val content: String,
)

fun MessageCreateBuilder.applyFromTag(kord: Kord, tag: Tag, args: List<String>) {
    when (tag.data) {
        is TagData.AliasTagData -> this.applyFromTag(kord, tag.data.targetTag!!, args)
        is TagData.TextTagData -> content = tag.content.substitute(args)
        is TagData.EmbedTagData -> embed {
            Embed(tag.data.embed, kord).apply(this)
            description = tag.content
            if (tag.data.colour != null) {
                val colourString = tag.data.colour.lowercase(Locale.getDefault())

                color = colourFromName(colourString) ?: java.awt.Color.decode(colourString).kColor
            }
        }
    }

    allowedMentions { } // Do not allow mentioning users/roles/@everyone/@here
}

private fun String.substitute(args: List<String>): String {
    var tmp = this
    for ((i, arg) in args.withIndex()) {
        tmp = tmp.replace("{{$i}}", arg)
    }
    return tmp
}

fun colourFromName(name: String): Color? = when (name) {
    "black" -> DISCORD_BLACK
    "blurple" -> DISCORD_BLURPLE
    "fuchsia" -> DISCORD_FUCHSIA
    "green" -> DISCORD_GREEN
    "pink" -> DISCORD_PINK
    "red" -> DISCORD_RED
    "white" -> DISCORD_WHITE
    "yellow" -> DISCORD_YELLOW
    else -> null
}