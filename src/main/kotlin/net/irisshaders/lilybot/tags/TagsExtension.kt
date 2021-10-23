/**
 * This code was taken, with permission, from <https://github.com/QuiltServerTools/axolotl>
 * This code is Licensed under the MIT license (<https://mit-license.org/>) unlike the rest of hte project
 * @author Tom_The_Geek
 */
@file:OptIn(KordPreview::class)

package net.irisshaders.lilybot.tags

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.MODERATORS
import net.irisshaders.lilybot.utils.TAG_PREFIX
import org.koin.core.component.inject

class TagsExtension : Extension() {
    override val name = "tags"

    // Obtain the TagRepo that was loaded in App.kt
    private val tagRepo: TagRepo by inject()

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                tagRepo.reload()
            }
        }

        event<MessageCreateEvent> {
            check { failIf(!event.message.content.startsWith(TAG_PREFIX)) }

            action {
                val pts = event.message.content.removePrefix(TAG_PREFIX).split("\\s".toRegex())
                val tagName = pts.first()
                val args = pts.drop(1)
                val tag = tagRepo[tagName]

                if (tag != null) {
                    try {
                        event.message.channel.createMessage {
                            applyFromTag(kord, tag, args)
                        }
                    } catch (e: Exception) {
                        event.message.channel.createMessage {
                            content = "Failed to send that tag: `${e::class.java.simpleName}: ${e.message}`"
                        }
                    }
                }
            }
        }

        ephemeralSlashCommand {
            name = "reload-tags"
            description = "Reloads the tags"
            allowRole(MODERATORS)
            guild(GUILD_ID)

            action {
                val tagCount = tagRepo.reload()
                respond { content = "Loaded `$tagCount` tags!" }
            }
        }
    }
}