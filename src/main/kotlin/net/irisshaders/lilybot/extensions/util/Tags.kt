package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
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
import dev.kord.core.behavior.channel.createEmbed
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
			description = "The parent command for all /tag commands"

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

		publicSlashCommand {
			name = "tag-help"
			description = "Explains how the tag command works!"

			action {
				respond {
					embed {
						title = "How does the /tag command and others work?"
						description =
							"The tag command allows users to add guild specific 'tag' commands at runtime to their " +
									"guild. Tags are like custom commands, they can do say what ever you want them" +
									"to say.\n\nTo create a tag, if you have the Moderate Members permission, run " +
									"the following command:\n`/create-tag <name> <title> <value>`\nYou will be " +
									"prompted to enter a name for the tag, a title for the tag, and the value for the" +
									" tag. This is what will appear in the embed of your tag. You can enter any " +
									"character you like into all of these inputs.\n\nTo use the tag, run the " +
									"following command\n`/tag <name>`\nYou will be prompted to enter the tag name, " +
									"but will have an autocomplete window to aid you. The window will list all the " +
									"tags that the guild has.\n\nTo delete a tag, if you have the Moderate Members " +
									"permission, run the following command\n`/delete-tag <name>`\nYou will be " +
									"prompted to enter the name of the tag, again aided by autocomplete, and then " +
									"the tag will be completed.\n\nGuilds can have any number of tags they like. The " +
									"limit on `tagValue` for tags is 1024 characters, which is the embed description " +
									"limit enforced by discord"
						color = DISCORD_BLURPLE
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
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
				val actionLog = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior

				actionLog.createEmbed {
					title = "Tag created!"
					description = "${user.asUser().mention} created the tag ${arguments.tagName}"
					field {
						name = "Tag contents:"
						value = """
							```text
							title: ${arguments.tagTitle}
							
							value: ${arguments.tagValue}
							```
						""".trimIndent()
						inline = false
					}
					footer {
						icon = user.asUser().avatar?.url
						text = "Requested by ${user.asUser().tag}"
					}
					timestamp = Clock.System.now()
				}

				DatabaseHelper.setTag(guild!!.id, arguments.tagName, arguments.tagTitle, arguments.tagValue)

				respond {
					content = "Tag: `${arguments.tagName}` created"
				}
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

				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
				val actionLog = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior

				responseEmbedInChannel(
					actionLog,
					"Tag deleted!",
					"The tag ${arguments.tagName} was deleted by ${user.asUser().mention}",
					DISCORD_RED,
					user.asUser()
				)

				DatabaseHelper.deleteTag(guild!!.id, arguments.tagName)

				respond {
					content = "Tag: `${arguments.tagName}` deleted"
				}
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
