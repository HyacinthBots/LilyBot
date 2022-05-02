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
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.responseEmbedInChannel

/**
 * The class that holds the commands to create tags commands.
 *
 * @since 3.1.0
 */
class Tags : Extension() {
	override val name = "tags"

	override suspend fun setup() {
		/**
		 * The command for calling tags.
		 *
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		publicSlashCommand(::TagArgs) {
			name = "tag"
			description = "Call a tag from this guild! Use /tag-help for more info."

			check { anyGuild() }

			action {
				if (DatabaseHelper.getTag(guild!!.id, arguments.tagName)?.name == null) {
					respondEphemeral {
						content = "Unable to find tag! Does this tag exist?"
					}
					return@action
				}

				respond {
					embed {
						color = DISCORD_BLURPLE
						title = DatabaseHelper.getTag(guild!!.id, arguments.tagName)?.tagTitle!!.toString()
						description = DatabaseHelper.getTag(guild!!.id, arguments.tagName)?.tagValue!!.toString()
						timestamp = Clock.System.now()
					}
				}
			}
		}

		/**
		 * A command to provide information on what tags are and how they work.
		 *
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		publicSlashCommand {
			name = "tag-help"
			description = "Explains how the tag command works!"

			action {
				respond {
					embed {
						title = "How does the tag system work?"
						description =
							"The tag command allows users to add guild specific 'tag' commands at runtime to their " +
									"guild. **Tags are like custom commands**, they can do say what ever you want them " +
									"to say.\n\n**To create a tag**, if you have the Moderate Members permission, run " +
									"the following command:\n`/tag-create <name> <title> <value>`\nYou will be " +
									"prompted to enter a name for the tag, a title for the tag, and the value for the" +
									" tag. This is what will appear in the embed of your tag. You can enter any " +
									"character you like into all of these inputs.\n\n**To use a tag**, run the " +
									"following command\n`/tag <name>`\nYou will be prompted to enter a tag name, " +
									"but will have an autocomplete window to aid you. The window will list all the " +
									"tags that the guild has.\n\n**To delete a tag**, if you have the Moderate Members " +
									"permission, run the following command\n`/tag-delete <name>`\nYou will be " +
									"prompted to enter the name of the tag, again aided by autocomplete, and then " +
									"the tag will be completed.\n\n**Guilds can have any number of tags they like.**"
						color = DISCORD_BLURPLE
						timestamp = Clock.System.now()
					}
				}
			}
		}

		/**
		 * The command for creating tags.
		 *
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		ephemeralSlashCommand(::CreateTagArgs) {
			name = "tag-create"
			description = "Create a tag for your guild! Use /tag-help for more info."

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannel(config.modActionLog) as GuildMessageChannelBehavior

				actionLog.createEmbed {
					color = DISCORD_GREEN
					title = "Tag created!"
					description = "The tag `${arguments.tagName}` has been created"
					field {
						name = "Tag title:"
						value = "`${arguments.tagTitle}`"
						inline = false
					}
					field {
						name = "Tag value:"
						value = "`${arguments.tagValue}`"
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

		/**
		 * The command for deleting tags.
		 *
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		ephemeralSlashCommand(::TagArgs) {
			name = "tag-delete"
			description = "Delete a tag from your guild. Use /tag-help for more info"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }
			check { configPresent() }

			action {
				// Check to make sure the tag exists in the datbase
				if (DatabaseHelper.getTag(guild!!.id, arguments.tagName)?.name == null) {
					respond {
						content = "Unable to find tag! Does this tag exist?"
					}
					return@action
				}

				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannel(config.modActionLog) as GuildMessageChannelBehavior

				responseEmbedInChannel(
					actionLog,
					"Tag deleted!",
					"The tag ${arguments.tagName} was deleted",
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
		/** The named identifier of the tag the user would like. */
		val tagName by string {
			name = "name"
			description = "The name of the tag"

			autoComplete {
				val tags = DatabaseHelper.getAllTags(data.guildId.value!!)
				val map = mutableMapOf<String, String>()

				// Add eac tag in the database to the tag variable
				tags.forEach {
					map[it.name] = it.name
				}

				// Provide the autocomplete with the tags map
				suggestStringMap(map)
			}
		}
	}

	inner class CreateTagArgs : Arguments() {
		/** The named identifier of the tag being created. */
		val tagName by string {
			name = "tagName"
			description = "The name of the tag you're making"
		}

		/** The title of the tag being created. */
		val tagTitle by string {
			name = "title"
			description = "The title of the tag embed you're making"
		}

		/** The value of the tag being create. */
		val tagValue by string {
			name = "value"
			description = "The content of the tag embed you're making"

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
			}
		}
	}
}
