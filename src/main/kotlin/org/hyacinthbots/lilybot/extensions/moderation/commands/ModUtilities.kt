package org.hyacinthbots.lilybot.extensions.moderation.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.DISCORD_BLACK
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.DISCORD_WHITE
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.defaultingBoolean
import dev.kordex.core.commands.converters.impl.defaultingColor
import dev.kordex.core.commands.converters.impl.optionalBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.commands.converters.impl.optionalColour
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.snowflake
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.components.linkButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.utils.getJumpUrl
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.NewsChannelPublishingCollection
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.RoleSubscriptionCollection
import org.hyacinthbots.lilybot.database.collections.StatusCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs
import org.hyacinthbots.lilybot.utils.trimmedContents
import org.hyacinthbots.lilybot.utils.updateDefaultPresence
import kotlin.time.Duration.Companion.minutes

/**
 * This class contains a few utility commands that can be used by moderators. They all require a guild to be run.
 *
 * @since 3.1.0
 */
class ModUtilities : Extension() {
	override val name = "mod-utilities"

	private val presenceScheduler = Scheduler()
	private lateinit var presenceTask: Task

	override suspend fun setup() {
		presenceTask = presenceScheduler.schedule(
			15.minutes, repeat = true, callback = ::updateDefaultPresence, name = "Presence task"
		)

		/**
		 * Say Command
		 * @author NoComment1105, tempest15
		 * @since 2.0
		 */
		ephemeralSlashCommand(::SayArgs) {
			name = Translations.Moderation.ModUtilities.Say.name
			description = Translations.Moderation.ModUtilities.Say.description

			requirePermission(Permission.ModerateMembers)

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}
			action {
				val targetChannel: GuildMessageChannel = if (arguments.channel != null) {
					guild!!.getChannelOfOrNull(arguments.channel!!.id) ?: return@action
				} else {
					channel.asChannelOfOrNull() ?: return@action
				}
				val createdMessage: Message
				val translations = Translations.Moderation.ModUtilities.Say

				try {
					if (arguments.embed) {
						createdMessage = targetChannel.createEmbed {
							color = arguments.color
							description = arguments.message
							if (arguments.timestamp) {
								timestamp = Clock.System.now()
							}
						}
					} else {
						createdMessage = targetChannel.createMessage {
							content = arguments.message
						}
					}
				} catch (_: KtorRequestException) {
					respond { content = translations.noSendPerms.translate() }
					return@action
				}

				respond { content = translations.response.translate() }

				val utilityLog =
					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!) ?: return@action
				utilityLog.createMessage {
					embed {
						title = translations.embedTitle.translate()
						description = "```${arguments.message}```"
						field {
							name = translations.channelField.translate()
							value = targetChannel.mention
							inline = true
						}
						field {
							name = translations.typeField.translate()
							value = if (arguments.embed) {
								translations.embedType
							} else {
								translations.messageType
							}.translate()
							inline = true
						}
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						if (arguments.embed) {
							color = arguments.color
							field {
								name = translations.colorField.translate()
								value = arguments.color.toString()
								inline = true
							}
						} else {
							color = DISCORD_BLACK
						}
					}
					components {
						linkButton {
							label = translations.jumpButton
							url = createdMessage.getJumpUrl()
						}
					}
				}
			}
		}

		/**
		 * Message editing command
		 *
		 * @since 3.3.0
		 */
		ephemeralSlashCommand(::SayEditArgs) {
			name = Translations.Moderation.ModUtilities.EditSay.name
			description = Translations.Moderation.ModUtilities.EditSay.description

			requirePermission(Permission.ModerateMembers)

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
			}

			action {
				// The channel the message was sent in. Either the channel provided, or if null, the channel the
				// command was executed in.
				val channelOfMessage = if (arguments.channelOfMessage != null) {
					guild!!.getChannelOfOrNull<GuildMessageChannel>(arguments.channelOfMessage!!.id)
				} else {
					channel
				}
				val message = channelOfMessage?.getMessageOrNull(arguments.messageToEdit)

				val translations = Translations.Moderation.ModUtilities.EditSay

				if (message == null) {
					respond { content = translations.unableTo.translate() }
					return@action
				}

				val originalContent = message.content
				// The messages that contains the embed that is going to be edited. If the message has no embed, or
				// it's not by LilyBot, it returns
				if (message.embeds.isEmpty()) {
					if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
						respond { content = translations.notAuthor.translate() }
						return@action
					} else if (arguments.newContent == null) {
						respond { content = translations.missingContent.translate() }
						return@action
					} else if (arguments.newContent != null && arguments.newContent!!.length > 1024) {
						respond { content = translations.maxLength.translate() }
						return@action
					}

					message.edit { content = arguments.newContent }

					respond { content = translations.response.translate() }

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
						?: return@action
					utilityLog.createMessage {
						embed {
							title = translations.embedTitle.translate()
							field {
								name = translations.embedOriginal.translate()
								value = "```${originalContent.trimmedContents(500)}```"
							}
							field {
								name = translations.embedNew.translate()
								value = "```${arguments.newContent.trimmedContents(500)}```"
							}
							footer {
								text = translations.editedBy.translate(user.asUserOrNull()?.username)
								icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
							}
							color = DISCORD_WHITE
							timestamp = Clock.System.now()
						}
						components {
							linkButton {
								label = Translations.Moderation.ModUtilities.Say.jumpButton
								url = message.getJumpUrl()
							}
						}
					}
				} else {
					if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
						respond { content = translations.notAuthor.translate() }
						return@action
					}

					// The old description and color to the embed. We get it here before we start changing it.
					val oldContent = message.embeds[0].description
					val oldColor = message.embeds[0].color
					val oldTimestamp = message.embeds[0].timestamp

					message.edit {
						embed {
							description = arguments.newContent ?: oldContent
							color = arguments.newColor ?: oldColor
							timestamp = when (arguments.timestamp) {
								true -> message.timestamp
								false -> null
								null -> oldTimestamp
							}
						}
					}

					respond { content = translations.response.translate() }

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
						?: return@action
					utilityLog.createMessage {
						embed {
							title = translations.embedTitle.translate()
							field {
								name = translations.embedOriginal.translate()
								// The old content, if null none
								value = "```${oldContent ?: Translations.Basic.none.translate()}```"
							}
							field {
								name = translations.embedNew.translate()
								// The new content, if null the old content, if null none
								value =
									"```${arguments.newContent ?: oldContent ?: Translations.Basic.none.translate()}```"
							}
							field {
								name = translations.embedOldColor.translate()
								value = oldColor.toString()
							}
							field {
								name = translations.embedNewColor.translate()
								value =
									if (arguments.newColor != null) arguments.newColor.toString() else oldColor.toString()
							}
							field {
								name = translations.embedHasTime.translate()
								value = when (arguments.timestamp) {
									true -> Translations.Basic.`true`
									false -> Translations.Basic.`false`
									else -> Translations.Basic.original
								}.translate()
							}
							footer {
								text = "Edited by ${user.asUserOrNull()?.username}"
								icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
							}
							timestamp = Clock.System.now()
							color = DISCORD_WHITE
						}
						components {
							linkButton {
								label = Translations.Moderation.ModUtilities.Say.jumpButton
								url = message.getJumpUrl()
							}
						}
					}
				}
			}
		}

		/**
		 * Presence Command
		 * @author IMS
		 * @since 2.0
		 */
		ephemeralSlashCommand(::PresenceArgs) {
			name = Translations.Moderation.ModUtilities.Status.name
			description = Translations.Moderation.ModUtilities.Status.description

			guild(TEST_GUILD_ID)

			ephemeralSubCommand(::PresenceArgs) {
				name = Translations.Moderation.ModUtilities.Status.Set.name
				description = Translations.Moderation.ModUtilities.Status.Set.description

				guild(TEST_GUILD_ID)
				requirePermission(Permission.Administrator)

				check {
					hasPermission(Permission.Administrator)
					requiredConfigs(ConfigOptions.MODERATION_ENABLED, ConfigOptions.ACTION_LOG)
				}

				action {
					val config = ModerationConfigCollection().getConfig(guildFor(event)!!.id)!!
					val actionLog = guild!!.getChannelOfOrNull<GuildMessageChannel>(config.channel!!)

					val translations = Translations.Moderation.ModUtilities.Status.Set

					// Update the presence in the action
					this@ephemeralSlashCommand.kord.editPresence {
						status = PresenceStatus.Online
						playing(arguments.presenceArgument)
					}

					// Store the new presence in the database for if there is a restart
					StatusCollection().setStatus(arguments.presenceArgument)

					respond { content = translations.response.translate(arguments.presenceArgument) }

					actionLog?.createEmbed {
						title = translations.embedTitle.translate()
						description = translations.embedDesc.translate(arguments.presenceArgument)
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						color = DISCORD_BLACK
					}
				}
			}

			ephemeralSubCommand {
				name = Translations.Moderation.ModUtilities.Status.Reset.name
				description = Translations.Moderation.ModUtilities.Status.Reset.description

				guild(TEST_GUILD_ID)
				requirePermission(Permission.Administrator)

				check {
					hasPermission(Permission.Administrator)
					requiredConfigs(ConfigOptions.MODERATION_ENABLED, ConfigOptions.ACTION_LOG)
				}

				action {
					val translations = Translations.Moderation.ModUtilities.Status.Reset

					// Store the new presence in the database for if there is a restart
					StatusCollection().setStatus(null)

					updateDefaultPresence()
					val guilds = this@ephemeralSlashCommand.kord.guilds.toList().size

					respond { content = translations.response.translate() }

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
						?: return@action
					utilityLog.createEmbed {
						title = Translations.Moderation.ModUtilities.Status.Set.embedTitle.translate()
						description = translations.embedDesc.translate()
						field {
							value = translations.embedValue.translate(guilds)
						}
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						color = DISCORD_BLACK
					}
				}
			}
		}

		ephemeralSlashCommand(::ResetModal) {
			name = Translations.Moderation.ModUtilities.Reset.name
			description = Translations.Moderation.ModUtilities.Reset.description

			requirePermission(Permission.Administrator) // Hide this command from non-administrators

			check {
				anyGuild()
				hasPermission(Permission.Administrator)
			}

			action { modal ->
				val translations = Translations.Moderation.ModUtilities.Reset
				if (modal?.confirmation?.value?.lowercase() != "yes") {
					respond { content = translations.failResponse.translate() }
					return@action
				}

				var response: EphemeralFollowupMessage? = null

				response = respond {
					content = translations.tripleCheck.translate()

					components {
						ephemeralButton(0) {
							label = translations.yesButton
							style = ButtonStyle.Danger

							action {
								response?.edit {
									content = translations.resetResponse.translate()
									components { removeAll() }
								}

								guild!!.getChannelOfOrNull<GuildMessageChannel>(
									ModerationConfigCollection().getConfig(guild!!.id)?.channel
										?: guild!!.asGuildOrNull()
											?.getSystemChannel()!!.id
								)?.createMessage {
									embed {
										title = translations.resetResponse.translate()
										description = translations.embedDesc.translate()
										timestamp = Clock.System.now()
										color = DISCORD_BLACK
									}
								}

								// Reset
								AutoThreadingCollection().deleteGuildAutoThreads(guild!!.id)
								GalleryChannelCollection().removeAll(guild!!.id)
								GithubCollection().removeDefaultRepo(guild!!.id)
								LoggingConfigCollection().clearConfig(guild!!.id)
								ModerationConfigCollection().clearConfig(guild!!.id)
								NewsChannelPublishingCollection().clearAutoPublishingForGuild(guild!!.id)
								ReminderCollection().removeGuildReminders(guild!!.id)
								RoleMenuCollection().removeAllRoleMenus(guild!!.id)
								RoleSubscriptionCollection().removeAllSubscribableRoles(guild!!.id)
								TagsCollection().clearTags(guild!!.id)
								ThreadsCollection().removeGuildThreads(guild!!.id)
								UtilityConfigCollection().clearConfig(guild!!.id)
								WarnCollection().clearWarns(guild!!.id)
								WelcomeChannelCollection().removeWelcomeChannelsForGuild(guild!!.id, kord)
							}
						}

						ephemeralButton(0) {
							label = translations.noButton
							style = ButtonStyle.Secondary

							action {
								response?.edit {
									content = translations.cancelResponse.translate()
									components { removeAll() }
								}
							}
						}
					}
				}
			}
		}
	}

	inner class SayArgs : Arguments() {
		/** The message the user wishes to send. */
		val message by string {
			name = Translations.Moderation.ModUtilities.Say.Arguments.Message.name
			description = Translations.Moderation.ModUtilities.Say.Arguments.Message.description

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
					.replace("\n", "\n")
			}
		}

		/** The channel to aim the message at. */
		val channel by optionalChannel {
			name = Translations.Moderation.ModUtilities.Say.Arguments.Channel.name
			description = Translations.Moderation.ModUtilities.Say.Arguments.Channel.description
		}

		/** Whether to embed the message or not. */
		val embed by defaultingBoolean {
			name = Translations.Moderation.ModUtilities.Say.Arguments.Embed.name
			description = Translations.Moderation.ModUtilities.Say.Arguments.Embed.description
			defaultValue = false
		}

		/** If the embed should have a timestamp. */
		val timestamp by defaultingBoolean {
			name = Translations.Moderation.ModUtilities.Say.Arguments.Timestamp.name
			description = Translations.Moderation.ModUtilities.Say.Arguments.Timestamp.description
			defaultValue = true
		}

		/** What color the embed should be. */
		val color by defaultingColor {
			name = Translations.Moderation.ModUtilities.Say.Arguments.Color.name
			description = Translations.Moderation.ModUtilities.Say.Arguments.Color.description
			defaultValue = DISCORD_BLURPLE
		}
	}

	inner class SayEditArgs : Arguments() {
		/** The ID of the embed to edit. */
		val messageToEdit by snowflake {
			name = Translations.Moderation.ModUtilities.EditSay.Arguments.MessageToEdit.name
			description = Translations.Moderation.ModUtilities.EditSay.Arguments.MessageToEdit.description
		}

		/** The new content of the embed. */
		val newContent by optionalString {
			name = Translations.Moderation.ModUtilities.EditSay.Arguments.NewContent.name
			description = Translations.Moderation.ModUtilities.EditSay.Arguments.NewContent.description

			mutate {
				it?.replace("\\n", "\n")
					?.replace("\n ", "\n")
					?.replace("\n", "\n")
			}
		}

		/** The new color for the embed. */
		val newColor by optionalColour {
			name = Translations.Moderation.ModUtilities.EditSay.Arguments.NewColor.name
			description = Translations.Moderation.ModUtilities.EditSay.Arguments.NewColor.description
		}

		/** The channel the embed was originally sent in. */
		val channelOfMessage by optionalChannel {
			name = Translations.Moderation.ModUtilities.EditSay.Arguments.ChannelOf.name
			description = Translations.Moderation.ModUtilities.EditSay.Arguments.ChannelOf.description
		}

		/** Whether to add the timestamp of when the message was originally sent or not. */
		val timestamp by optionalBoolean {
			name = Translations.Moderation.ModUtilities.EditSay.Arguments.Timestamp.name
			description = Translations.Moderation.ModUtilities.EditSay.Arguments.Timestamp.description
		}
	}

	inner class PresenceArgs : Arguments() {
		/** The new presence set by the command user. */
		val presenceArgument by string {
			name = Translations.Moderation.ModUtilities.Status.Arguments.Presence.name
			description = Translations.Moderation.ModUtilities.Status.Arguments.Presence.description
		}
	}

	inner class ResetModal : ModalForm() {
		override var title = Translations.Moderation.ModUtilities.Reset.Modal.title

		val confirmation = lineText {
			label = Translations.Moderation.ModUtilities.Reset.Modal.Confirmation.label
			placeholder = Translations.Moderation.ModUtilities.Reset.Modal.Confirmation.placeholder
			required = true
		}
	}
}
