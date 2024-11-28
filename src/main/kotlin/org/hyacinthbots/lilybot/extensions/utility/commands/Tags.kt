package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.Locale
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.optionalUser
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.pagination.EphemeralResponsePaginator
import dev.kordex.core.pagination.pages.Page
import dev.kordex.core.pagination.pages.Pages
import dev.kordex.core.utils.suggestStringMap
import kotlinx.datetime.Clock
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.trimmedContents

/**
 * The class that holds the commands to create tags commands.
 *
 * @since 3.1.0
 */
class Tags : Extension() {
	override val name = "tags"

	// TODO Rewrite to use modals?
	override suspend fun setup() {
		/**
		 * The command that allows users to preview tag contents before sending it in public
		 *
		 * @author NoComment1105
		 * @since 3.4.3
		 */
		ephemeralSlashCommand(::DeleteTagArgs) {
			name = Translations.Utility.Tags.Preview.name
			description = Translations.Utility.Tags.Preview.description

			check {
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val tagFromDatabase = TagsCollection().getTag(guild!!.id, arguments.tagName) ?: run {
					respond { content = Translations.Utility.Tags.unableToFind.translate(arguments.tagName) }
					return@action
				}

				if (tagFromDatabase.tagValue.length > 4096) {
					respond { content = Translations.Utility.Tags.bodyTooLongSomehow.translate() }
					return@action
				}

				respond {
					if (tagFromDatabase.tagAppearance == "embed") {
						embed {
							title = tagFromDatabase.tagTitle
							description = tagFromDatabase.tagValue
							footer {
								text = Translations.Utility.Tags.Preview.footer.translate()
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
			name = Translations.Utility.Tags.Tag.name
			description = Translations.Utility.Tags.Tag.description

			check {
				anyGuild()
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val translations = Translations.Utility.Tags.Tag
				val tagFromDatabase = TagsCollection().getTag(guild!!.id, arguments.tagName) ?: run {
					respond { content = Translations.Utility.Tags.unableToFind.translate(arguments.tagName) }
					return@action
				}

				if (tagFromDatabase.tagValue.length > 4096) {
					respond { content = Translations.Utility.Tags.bodyTooLongSomehow.translate() }
					return@action
				}

				respond { content = translations.response.translate() }

				// This is not the best way to do this. Ideally the ping would be in the same message as the embed in
				// a `respond` builder. A Discord limitation makes this not possible.
				channel.createMessage {
					if (tagFromDatabase.tagAppearance == "embed") {
						content = arguments.user?.mention
						embed {
							title = tagFromDatabase.tagTitle
							description = tagFromDatabase.tagValue
							footer {
								text = translations.footer.translate(user.asUserOrNull()?.username)
								icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
							}
							color = DISCORD_BLURPLE
						}
					} else {
						content =
							"${arguments.user?.mention ?: ""}\n**${tagFromDatabase.tagTitle}**\n${tagFromDatabase.tagValue}"
					}
				}

				// Log when a message tag is sent to allow identification of tag spammers
				if (tagFromDatabase.tagAppearance == "message") {
					val utilityLog = UtilityConfigCollection().getConfig(guild!!.id)?.utilityLogChannel ?: return@action
					guild!!.getChannelOfOrNull<GuildMessageChannel>(utilityLog)?.createMessage {
						embed {
							title = translations.embedTitle.translate()
							field {
								name = Translations.Basic.userField.translate()
								value = "${user.asUserOrNull()?.mention} (${user.asUserOrNull()?.username})"
							}
							field {
								name = translations.embedName.translate()
								value = "`${arguments.tagName}`"
							}
							field {
								name = translations.embedLocation.translate()
								value = "${channel.mention} ${channel.asChannelOrNull()?.data?.name?.value}"
							}
							footer {
								text = translations.embedFooter.translate(user.asUserOrNull()?.id)
								icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
							}
							timestamp = Clock.System.now()
						}
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
			name = Translations.Utility.Tags.TagHelp.name
			description = Translations.Utility.Tags.TagHelp.description

			check {
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				respond {
					embed {
						title = Translations.Utility.Tags.TagHelp.title.translate()
						description = Translations.Utility.Tags.TagHelp.description.translate()
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
			name = Translations.Utility.Tags.Create.name
			description = Translations.Utility.Tags.Create.description

			requirePermission(Permission.ModerateMembers)

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val translations = Translations.Utility.Tags.Create
				if (TagsCollection().getTag(guild!!.id, arguments.tagName) != null) {
					respond { content = translations.already.translate() }
					return@action
				}

				if (arguments.tagValue.length > 4096) {
					respond { content = Translations.Utility.Tags.bodyTooLong.translate() }
					return@action
				} else if (arguments.tagTitle.length > 256) {
					respond { content = Translations.Utility.Tags.tooLongTitle.translate() }
				}

				TagsCollection().setTag(
					guild!!.id,
					arguments.tagName,
					arguments.tagTitle,
					arguments.tagValue,
					arguments.tagAppearance
				)

				val utilityLog =
					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!) ?: return@action
				// TODO Do if inside builder to reduce duplication, not a translations PR thing
				if (arguments.tagValue.length <= 1024) {
					utilityLog.createEmbed {
						title = translations.embedTitle.translate()
						description = translations.embedDesc.translate(arguments.tagName)
						field {
							name = translations.embedTagTitle.translate()
							value = "`${arguments.tagTitle}`"
							inline = false
						}
						field {
							name = translations.embedTagValue.translate()
							value = "```${arguments.tagValue}```"
							inline = false
						}
						appearanceFooter(arguments.tagAppearance, user)
					}
				} else {
					utilityLog.createMessage {
						embed {
							title = translations.embedTitle.translate()
							description = translations.embedDesc.translate(arguments.tagName)
							field {
								name = translations.embedTagTitle.translate()
								value = "`${arguments.tagTitle}`"
								inline = false
							}
							field {
								name = translations.embedTagValue.translate()
								value = "${arguments.tagValue.trimmedContents(1024)}"
								inline = false
							}
							color = DISCORD_GREEN
						}
						embed {
							description = arguments.tagValue.substring(1018)
							appearanceFooter(arguments.tagAppearance, user)
						}
					}
				}

				respond {
					content = translations.response.translate(arguments.tagName)
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
			name = Translations.Utility.Tags.Delete.name
			description = Translations.Utility.Tags.Delete.description

			requirePermission(Permission.ModerateMembers)

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val translations = Translations.Utility.Tags.Delete
				// Check to make sure the tag exists in the database
				if (TagsCollection().getTag(guild!!.id, arguments.tagName)?.name == null) {
					respond {
						content = Translations.Utility.Tags.unableToFind.translate(arguments.tagName)
					}
					return@action
				}

				TagsCollection().removeTag(guild!!.id, arguments.tagName)

				val utilityLog =
					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!) ?: return@action
				utilityLog.createEmbed {
					title = translations.embedTitle.translate()
					description = translations.embedDesc.translate()
					footer {
						text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
						icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
					}
					color = DISCORD_RED
				}
				respond {
					content = translations.response.translate(arguments.tagName)
				}
			}
		}

		ephemeralSlashCommand(::EditTagArgs) {
			name = Translations.Utility.Tags.Edit.name
			description = Translations.Utility.Tags.Edit.description

			requirePermission(Permission.ModerateMembers)

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val translations = Translations.Utility.Tags.Edit
				val originalTag = TagsCollection().getTag(guild!!.id, arguments.tagName)
				if (originalTag == null) {
					respond { content = Translations.Utility.Tags.unableToFind.translate() }
					return@action
				}

				// TODO Why was this ever done, this can probably be yeeted
				val originalName = originalTag.name
				val originalTitle = originalTag.tagTitle
				val originalValue = originalTag.tagValue
				val originalAppearance = originalTag.tagAppearance

				if (arguments.newValue != null && arguments.newValue!!.length > 4096) {
					respond { content = Translations.Utility.Tags.bodyTooLong.translate() }
					return@action
				} else if (arguments.newTitle != null && arguments.newTitle!!.length > 256) {
					respond { content = Translations.Utility.Tags.tooLongTitle.translate() }
				}

				TagsCollection().removeTag(guild!!.id, arguments.tagName)

				TagsCollection().setTag(
					guild!!.id,
					arguments.newName ?: originalName,
					arguments.newTitle ?: originalTitle,
					arguments.newValue ?: originalValue,
					arguments.newAppearance ?: originalAppearance
				)

				respond {
					content = translations.response.translate()
				}

				val utilityLog =
					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!) ?: return@action
				utilityLog.createEmbed {
					title = translations.response.translate()
					description = translations.embedDesc.translate(arguments.tagName)
					field {
						name = translations.embedName.translate()
						value = if (arguments.newName.isNullOrEmpty()) {
							originalName
						} else {
							"$originalName -> ${arguments.newName!!}"
						}
					}
					field {
						name = translations.embedTitleField.translate()
						value = if (arguments.newTitle.isNullOrEmpty()) {
							originalTitle
						} else {
							"${arguments.newTitle} -> ${arguments.newTitle!!}"
						}
					}
					field {
						name = Translations.Utility.Tags.tagAppearance.translate()
						value = if (arguments.newAppearance.isNullOrEmpty()) {
							originalAppearance
						} else {
							"$originalAppearance -> ${arguments.newAppearance}"
						}
					}
					color = DISCORD_YELLOW
				}
				if (arguments.newValue.isNullOrEmpty()) {
					utilityLog.createEmbed {
						title = translations.embedValue.translate()
						description = originalValue
						footer {
							text = translations.editedBy.translate(user.asUserOrNull()?.username)
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				} else {
					utilityLog.createEmbed {
						title = translations.oldValue.translate()
						description = originalValue
						color = DISCORD_YELLOW
					}
					utilityLog.createEmbed {
						title = translations.newValue.translate()
						description = arguments.newValue
						footer {
							text = translations.editedBy.translate(user.asUserOrNull()?.username)
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				}
			}
		}

		ephemeralSlashCommand {
			name = Translations.Utility.Tags.List.name
			description = Translations.Utility.Tags.List.description

			check {
				anyGuild()
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}

			action {
				val pagesObj = Pages()
				val tags = TagsCollection().getAllTags(guild!!.id)

				val translations = Translations.Utility.Tags.List

				if (tags.isEmpty()) {
					pagesObj.addPage(
						Page {
							description = translations.noTags.translate()
						}
					)
				} else {
					tags.chunked(5).forEach { tag ->
						var response = ""
						tag.forEach {
							response += "• ${it.name} - ${
								if (it.tagTitle.length >= 175) {
									it.tagTitle.substring(
										0,
										175
									)
								} else {
									it.tagTitle
								}
							}\n"
						}
						pagesObj.addPage(
							Page {
								title = translations.pageTitle.translate()
								description = translations.pageDesc.translate()
								field {
									name = translations.pageValue.translate()
									value = response
								}
							}
						)
					}
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

	private suspend fun EmbedBuilder.appearanceFooter(tagAppearance: String, user: UserBehavior) {
		field {
			name = Translations.Utility.Tags.tagAppearance.translate()
			value = tagAppearance
		}
		footer {
			icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
			text = Translations.Basic.requestedBy.translate(user.asUserOrNull()?.username)
		}
		timestamp = Clock.System.now()
		color = DISCORD_GREEN
	}

	inner class CallTagArgs : Arguments() {
		/** The named identifier of the tag the user would like. */
		val tagName by string {
			name = Translations.Utility.Tags.Tag.Arguments.Name.name
			description = Translations.Utility.Tags.Tag.Arguments.Name.description

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
			name = Translations.Utility.Tags.Tag.Arguments.User.name
			description = Translations.Utility.Tags.Tag.Arguments.User.description
		}
	}

	inner class DeleteTagArgs : Arguments() {
		val tagName by string {
			name = Translations.Utility.Tags.Tag.Arguments.Name.name
			description = Translations.Utility.Tags.Tag.Arguments.Name.description

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
			name = Translations.Utility.Tags.Tag.Arguments.Name.name
			description = Translations.Utility.Tags.Tag.Arguments.Name.description
		}

		/** The title of the tag being created. */
		val tagTitle by string {
			name = Translations.Utility.Tags.Create.Arguments.Title.name
			description = Translations.Utility.Tags.Create.Arguments.Title.description
		}

		/** The value of the tag being created. */
		val tagValue by string {
			name = Translations.Utility.Tags.Create.Arguments.Content.name
			description = Translations.Utility.Tags.Create.Arguments.Content.description

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
					.replace("\n", "\n")
			}
		}

		val tagAppearance by stringChoice {
			name = Translations.Utility.Tags.Create.Arguments.Appearance.name
			description = Translations.Utility.Tags.Create.Arguments.Appearance.description
			choices = mutableMapOf(
				Translations.Utility.Tags.Create.Arguments.Appearance.Choice.embed to "embed",
				Translations.Utility.Tags.Create.Arguments.Appearance.Choice.message to "message"
			)
		}
	}

	inner class EditTagArgs : Arguments() {
		/** The named identifier of the tag being edited. */
		val tagName by string {
			name = Translations.Utility.Tags.Tag.Arguments.Name.name
			description = Translations.Utility.Tags.Tag.Arguments.Name.description

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
			name = Translations.Utility.Tags.Edit.Arguments.NewName.name
			description = Translations.Utility.Tags.Edit.Arguments.NewName.description
		}

		/** The new title for the tag being edited. */
		val newTitle by optionalString {
			name = Translations.Utility.Tags.Edit.Arguments.NewTitle.name
			description = Translations.Utility.Tags.Edit.Arguments.NewTitle.description
		}

		/** The new value for the tag being edited. */
		val newValue by optionalString {
			name = Translations.Utility.Tags.Edit.Arguments.NewValue.name
			description = Translations.Utility.Tags.Edit.Arguments.NewValue.description

			mutate {
				it?.replace("\\n", "\n")
					?.replace("\n ", "\n")
					?.replace("\n", "\n")
			}
		}

		/** The new appearance for the tag being edited. */
		val newAppearance by optionalStringChoice {
			name = Translations.Utility.Tags.Edit.Arguments.NewAppearance.name
			description = Translations.Utility.Tags.Edit.Arguments.NewAppearance.description
			choices = mutableMapOf(
				Translations.Utility.Tags.Create.Arguments.Appearance.Choice.embed to "embed",
				Translations.Utility.Tags.Create.Arguments.Appearance.Choice.message to "message"
			)
		}
	}
}
