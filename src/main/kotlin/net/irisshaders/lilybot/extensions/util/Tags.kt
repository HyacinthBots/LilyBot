package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalUser
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.EphemeralResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.Locale
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.TagsCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.botHasChannelPerms
import net.irisshaders.lilybot.utils.configPresent

/**
 * The class that holds the commands to create tags commands.
 *
 * @since 3.1.0
 */
class Tags : Extension() {
	override val name = "tags"

	override suspend fun setup() {
		/**
		 * The command that allows users to preview tag contents before sending it in public
		 *
		 * @author NoComment1105
		 * @since 3.4.3
		 */
		ephemeralSlashCommand(::DeleteTagArgs) {
			name = "tag-preview"
			description = "Preview a tag's contents without sending it publicly."

			check {
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val tagFromDatabase = TagsCollection().getTag(guild!!.id, arguments.tagName) ?: run {
					respond {
						content = "Unable to find tag `${arguments.tagName}` for preview. " +
								"Be sure it exists and you've typed it correctly."
					}
					return@action
				}

				if (tagFromDatabase.tagValue.length > 1024) {
					respond {
						content =
							"The body of this tag is too long! Somehow this tag has a body of 1024 characters or" +
									"more, which is above the Discord limit. Please re-create this tag!"
					}
					return@action
				}

				channel.createMessage {
					if (tagFromDatabase.tagAppearance == "embed") {
						embed {
							title = tagFromDatabase.tagTitle
							description = tagFromDatabase.tagValue
							footer {
								text = "Tag preview"
							}
							color = DISCORD_BLURPLE
						}
					} else {
						content = "**${tagFromDatabase.tagTitle}**\n${tagFromDatabase.tagValue}"
					}
				}
			}
		}

		/**
		 * The command for calling tags.
		 *
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		ephemeralSlashCommand(::CallTagArgs) {
			name = "tag"
			description = "Call a tag from this guild! Use /tag-help for more info."

			check {
				anyGuild()
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val tagFromDatabase = TagsCollection().getTag(guild!!.id, arguments.tagName) ?: run {
					respond {
						content = "Unable to find tag `${arguments.tagName}`. " +
								"Be sure it exists and you've typed it correctly."
					}
					return@action
				}

				if (tagFromDatabase.tagValue.length > 1024) {
					respond {
						content =
							"The body of this tag is too long! Somehow this tag has a body of 1024 characters or" +
									"more, which is above the Discord limit. Please re-create this tag!"
					}
					return@action
				}

				respond { content = "Tag sent" }

				// This is not the best way to do this. Ideally the ping would be in the same message as the embed in
				// a `respond` builder. A Discord limitation makes this not possible.
				channel.createMessage {
					if (tagFromDatabase.tagAppearance == "embed") {
						content = arguments.user?.mention
						embed {
							title = tagFromDatabase.tagTitle
							description = tagFromDatabase.tagValue
							footer {
								text = "Tag requested by ${user.asUser().tag}"
								icon = user.asUser().avatar!!.url
							}
							color = DISCORD_BLURPLE
						}
					} else {
						content =
							"${arguments.user?.mention ?: ""}\n**${tagFromDatabase.tagTitle}**\n${tagFromDatabase.tagValue}"
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

			check {
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				respond {
					embed {
						title = "How does the tag system work?"
						description =
							"The tag command allows users to add guild specific 'tag' commands at runtime to their " +
									"guild. **Tags are like custom commands**, they can do say what ever you want " +
									"them to say.\n\n**To create a tag**, if you have the Moderate Members " +
									"permission, run the following command:\n`/tag-create <name> <title> <value>`\n " +
									"You will be prompted to enter a name for the tag, a title for the tag, and the " +
									"value for the  tag. This is what will appear in the embed of your tag. You can " +
									"enter any character you like into all of these inputs.\n\n**To use a tag**, " +
									"run the following command:\n`/tag <name>`\nYou will be prompted to enter a " +
									"tag name, but will have an autocomplete window to aid you. The window will " +
									"list all the tags that the guild has.\n\n**To delete a tag**, if you have " +
									"the Moderate Members permission, run the following command:\n" +
									"`/tag-delete <name>`\nYou will be prompted to enter the name of the tag, " +
									"again aided by autocomplete.\n`/tag-edit`\nYou will be prompted to enter a " +
									"tag name, but will have an autocomplete window to aid you. The window will " +
									"list all the tags that the guild has. From there you can enter a new name, title " +
									"or value. None of these are mandatory.\n`/tag-list\nDisplays a paginated list " +
									"of all tags for this guild. There are 10 tags on each page.\n\n**Guilds can " +
									"have any number of tags they like.** The limit on `tagValue` for tags is 1024 " +
									"characters, which is the embed description limit enforced by Discord."
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

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

				if (TagsCollection().getTag(guild!!.id, arguments.tagName) != null) {
					respond { content = "A tag with that name already exists in this guild." }
					return@action
				}

				if (arguments.tagValue.length > 1024) {
					respond {
						content = "That tag is body is too long! Due to Discord limitations tag bodies can only be " +
								"1024 characters or less!"
					}
					return@action
				}

				TagsCollection().setTag(
					guild!!.id,
					arguments.tagName,
					arguments.tagTitle,
					arguments.tagValue,
					arguments.tagAppearance
				)

				actionLog.createEmbed {
					title = "Tag created!"
					description = "The tag `${arguments.tagName}` has been created"
					field {
						name = "Tag title:"
						value = "`${arguments.tagTitle}`"
						inline = false
					}
					field {
						name = "Tag value:"
						value = "```${arguments.tagValue}```"
						inline = false
					}
					field {
						name = "Tag appearance"
						value = arguments.tagAppearance
					}
					footer {
						icon = user.asUser().avatar?.url
						text = "Requested by ${user.asUser().tag}"
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}

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
		ephemeralSlashCommand(::DeleteTagArgs) {
			name = "tag-delete"
			description = "Delete a tag from your guild. Use /tag-help for more info."

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				// Check to make sure the tag exists in the database
				if (TagsCollection().getTag(guild!!.id, arguments.tagName)?.name == null) {
					respond {
						content = "Unable to find tag `${arguments.tagName}`! Does this tag exist?"
					}
					return@action
				}

				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

				TagsCollection().removeTag(guild!!.id, arguments.tagName)

				actionLog.createEmbed {
					title = "Tag deleted!"
					description = "The tag ${arguments.tagName} was deleted"
					footer {
						text = user.asUser().tag
						icon = user.asUser().avatar?.url
					}
					color = DISCORD_RED
				}
				respond {
					content = "Tag: `${arguments.tagName}` deleted"
				}
			}
		}

		ephemeralSlashCommand(::EditTagArgs) {
			name = "tag-edit"
			description = "Edit a tag in your guild. Use /tag-help for more info."

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

				if (TagsCollection().getTag(guild!!.id, arguments.tagName) == null) {
					respond { content = "Unable to find tag `${arguments.tagName}`! Does this tag exist?" }
					return@action
				}

				val originalName = TagsCollection().getTag(guild!!.id, arguments.tagName)!!.name
				val originalTitle = TagsCollection().getTag(guild!!.id, arguments.tagName)!!.tagTitle
				val originalValue = TagsCollection().getTag(guild!!.id, arguments.tagName)!!.tagValue
				val originalAppearance = TagsCollection().getTag(guild!!.id, arguments.tagName)!!.tagAppearance

				TagsCollection().removeTag(guild!!.id, arguments.tagName)

				if (arguments.newValue != null && arguments.newValue!!.length > 1024) {
					respond {
						content =
							"That tag is body is too long! Due to Discord limitations tag bodies can only be " +
									"1024 characters or less!"
					}
					return@action
				}

				TagsCollection().setTag(
					guild!!.id,
					arguments.newName ?: originalName,
					arguments.newTitle ?: originalTitle,
					arguments.newValue ?: originalValue,
					arguments.newAppearance ?: originalAppearance
				)

				actionLog.createMessage {
					embed {
						title = "Tag Edited"
						description = "The tag `${arguments.tagName}` was edited"
						field {
							name = "Name"
							value = if (arguments.newName.isNullOrEmpty()) {
								originalName
							} else {
								"$originalName -> ${arguments.newName!!}"
							}
						}
						field {
							name = "Title"
							value = if (arguments.newTitle.isNullOrEmpty()) {
								originalTitle
							} else {
								"${arguments.newTitle} -> ${arguments.newTitle!!}"
							}
						}
						field {
							name = "Value"
							value = if (arguments.newValue.isNullOrEmpty()) {
								originalValue
							} else {
								"$originalValue -> ${arguments.newValue!!}"
							}
						}
						field {
							name = "Tag appearance"
							value = if (arguments.newAppearance.isNullOrEmpty()) {
								originalAppearance
							} else {
								"$originalAppearance -> ${arguments.newAppearance}"
							}
						}
						footer {
							text = "Edited by ${user.asUser().tag}"
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				}

				respond {
					content = "Tag edited!"
				}
			}
		}

		ephemeralSlashCommand {
			name = "tag-list"
			description = "List all tags for this guild"

			check {
				anyGuild()
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val pagesObj = Pages()
				val tags = TagsCollection().getAllTags(guild!!.id)

				tags.chunked(10).forEach { tag ->
					var response = ""
					tag.forEach {
						response += "• ${it.name} - ${it.tagTitle}\n"
					}
					pagesObj.addPage(
						Page {
							title = "Tags for this guild"
							description = "Here are all the tags for this guild"
							field {
								name = "Name | Title"
								value = response
							}
						}
					)
				}

				val paginator = EphemeralResponsePaginator(
					pages = pagesObj,
					owner = event.interaction.user,
					timeoutSeconds = 500,
					locale = Locale.ENGLISH_GREAT_BRITAIN.asJavaLocale(),
					interaction = interactionResponse,
				)

				paginator.send()
			}
		}
	}

	inner class CallTagArgs : Arguments() {
		/** The named identifier of the tag the user would like. */
		val tagName by string {
			name = "name"
			description = "The name of the tag you want to call"

			autoComplete {
				val tags = TagsCollection().getAllTags(data.guildId.value!!)
				val map = mutableMapOf<String, String>()

				tags.forEach {
					map[it.name] = it.name
				}

				suggestStringMap(map)
			}
		}

		val user by optionalUser {
			name = "user"
			description = "The user to mention with the tag (optional)"
		}
	}

	inner class DeleteTagArgs : Arguments() {
		val tagName by string {
			name = "name"
			description = "The name of the tag"

			autoComplete {
				val tags = TagsCollection().getAllTags(data.guildId.value!!)
				val map = mutableMapOf<String, String>()

				// Add each tag in the database to the tag variable
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
			name = "name"
			description = "The name of the tag you're making"
		}

		/** The title of the tag being created. */
		val tagTitle by string {
			name = "title"
			description = "The title of the tag embed you're making"
		}

		/** The value of the tag being created. */
		val tagValue by string {
			name = "value"
			description = "The content of the tag embed you're making"

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
					.replace("\n", "\n")
			}
		}

		val tagAppearance by stringChoice {
			name = "appearance"
			description = "The appearance of the tag embed you're making"
			choices = mutableMapOf("embed" to "embed", "message" to "message")
		}
	}

	inner class EditTagArgs : Arguments() {
		/** The named identifier of the tag being edited. */
		val tagName by string {
			name = "name"
			description = "The name of the tag you're editing"

			autoComplete {
				val tags = TagsCollection().getAllTags(data.guildId.value!!)
				val map = mutableMapOf<String, String>()

				// Add each tag in the database to the tag variable
				tags.forEach {
					map[it.name] = it.name
				}

				// Provide the autocomplete with the tags map
				suggestStringMap(map)
			}
		}

		/** The new name for the tag being edited. */
		val newName by optionalString {
			name = "newName"
			description = "The new name for the tag you're editing"
		}

		/** The new title for the tag being edited. */
		val newTitle by optionalString {
			name = "newTitle"
			description = "The new title for the tag you're editing"
		}

		/** The new value for the tag being edited. */
		val newValue by optionalString {
			name = "newValue"
			description = "The new value for the tag you're editing"

			mutate {
				it?.replace("\\n", "\n")
					?.replace("\n ", "\n")
					?.replace("\n", "\n")
			}
		}

		/** The new appearance for the tag being edited. */
		val newAppearance by optionalStringChoice {
			name = "newAppearance"
			description = "The new appearance for the tag you're editing"
			choices = mutableMapOf("embed" to "embed", "message" to "message")
		}
	}
}
