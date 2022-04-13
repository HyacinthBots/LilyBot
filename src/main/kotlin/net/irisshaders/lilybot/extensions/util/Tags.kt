package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.getConfigPrivateResponse
import net.irisshaders.lilybot.utils.responseEmbedInChannel

class Tags : Extension() {
	override val name = "tags"

	override suspend fun setup() {

		publicSlashCommand(::TagArgs) {
			name = "tag"
			description = "Tag commands!"

			check { anyGuild() }

			action {

				if (DatabaseHelper.getTag(guild!!.id, arguments.tagName, "name") == null) {
					respondEphemeral {
						content = "Unable to find tag! Does this tag exist?"
					}
					return@action
				}

				respond {
					embed {
						color = DISCORD_BLURPLE
						title = DatabaseHelper.getTag(guild!!.id, arguments.tagName, "tagTitle")!!.toString()
						description = DatabaseHelper.getTag(guild!!.id, arguments.tagName, "tagValue")!!.toString()
						timestamp = Clock.System.now()
					}
				}
			}
		}

		ephemeralSlashCommand(::CreateTagArgs) {
			name = "create-tag"
			description = "Create a tag for your guild!"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			action {
				DatabaseHelper.setTag(guild!!.id, arguments.tagName, arguments.tagTitle, arguments.tagValue)

				respond {
					content = "Tag: `${arguments.tagName}` created"
				}

				val actionLogId = getConfigPrivateResponse("actionLog") ?: return@action
				val actionLog = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior

				responseEmbedInChannel(
					actionLog,
					"Tag created!",
					"The tag ${arguments.tagName} was created by ${user.asUser().mention}",
					DISCORD_GREEN,
					user.asUser()
				)
			}
		}

		ephemeralSlashCommand(::TagArgs) {
			name = "delete-tag"
			description = "Delete a tag from your guild"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			action {
				if (DatabaseHelper.getTag(guild!!.id, arguments.tagName, "name") == null) {
					respond {
						content = "Unable to find tag! Does this tag exist?"
					}
					return@action
				}

				DatabaseHelper.deleteTag(guild!!.id, arguments.tagName)

				respond {
					content = "Tag: `${arguments.tagName}` deleted"
				}


				val actionLogId = getConfigPrivateResponse("actionLog") ?: return@action
				val actionLog = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior

				responseEmbedInChannel(
					actionLog,
					"Tag deleted!",
					"The tag ${arguments.tagName} was deleted by ${user.asUser().mention}",
					DISCORD_RED,
					user.asUser()
				)
			}
		}
	}

	inner class TagArgs : Arguments() {
		val tagName by string {
			name = "name"
			description = "The name of the tag"

			autoComplete {
				val tags = DatabaseHelper.getAllTags(data.guildId.value!!)
				val map = mutableMapOf<String, String>()

				tags.forEach {
					map[it.name!!] = it.name
				}

				suggestStringMap(map)
			}
		}
	}

	inner class CreateTagArgs : Arguments() {
		val tagName by string {
			name = "tagName"
			description = "The name of the tag you're making"
		}
		val tagTitle by string {
			name = "title"
			description = "The title of the tag embed you're making"
		}
		val tagValue by string {
			name = "value"
			description = "The content of the tag embed you're making"
		}
	}
}
