package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PKMessage
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.PKMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.hyacinthbots.docgenerator.subCommandAdditionalDocumentation
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.canPingRole
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

class AutoThreading : Extension() {
	override val name = "auto-threading"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "auto-threading"
			description = "The parent command for auto-threading management."

			@OptIn(UnsafeAPI::class)
			unsafeSubCommand(::AutoThreadingArgs) {
				name = "enable"
				description = "Automatically create a thread for each message sent in this channel."

				initialResponse = InitialSlashCommandResponse.None

				requirePermission(Permission.ManageChannels)

				check {
					anyGuild()
					hasPermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					// Check if the auto-threading is disabled
					if (AutoThreadingCollection().getSingleAutoThread(channel.id) != null) {
						ackEphemeral()
						respondEphemeral {
							content = "Auto-threading is already enabled for this channel."
						}
						return@action
					}

					// Check if the role can be pinged
					if (!canPingRole(arguments.role, guild!!.id, this@unsafeSubCommand.kord)) {
						ackEphemeral()
						respondEphemeral {
							content = "Lily cannot mention this role. Please fix the role's permissions and try again."
						}
						return@action
					}

					var message: String? = null

					if (arguments.message) {
						val modalObj = MessageModal()

						this@unsafeSubCommand.componentRegistry.register(modalObj)

						event.interaction.modal(
							modalObj.title,
							modalObj.id
						) {
							modalObj.applyToBuilder(this, getLocale(), null)
						}

						modalObj.awaitCompletion { modalSubmitInteraction ->
							interactionResponse = modalSubmitInteraction?.deferEphemeralMessageUpdate()
						}

						message = modalObj.msgInput.value!!
					} else {
						ackEphemeral()
					}

					respondEphemeral {
						content = "Auto-threading has been **enabled** in this channel."
					}

					// Add the channel to the database as auto-threaded
					AutoThreadingCollection().setAutoThread(
						AutoThreadingData(
							guildId = guild!!.id,
							channelId = channel.id,
							roleId = arguments.role?.id,
							preventDuplicates = arguments.preventDuplicates,
							archive = arguments.archive,
							contentAwareNaming = arguments.contentAwareNaming,
							mention = arguments.mention,
							creationMessage = message,
							addModsAndRole = arguments.addModsAndRole,
							extraRoleIds = mutableListOf()
						)
					)

					// Log the change
					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!) ?: return@action

					utilityLog.createEmbed {
						title = "Auto-threading Enabled"
						description = null
						field {
							name = "Channel:"
							value = channel.mention
							inline = true
						}
						field {
							name = "Role:"
							value = arguments.role?.mention ?: "null"
							inline = true
						}
						field {
							name = "Prevent Duplicates:"
							value = arguments.preventDuplicates.toString()
							inline = true
						}
						field {
							name = "Begin Archived:"
							value = arguments.archive.toString()
							inline = true
						}
						field {
							name = "Smart Naming Enabled:"
							value = arguments.contentAwareNaming.toString()
							inline = true
						}
						field {
							name = "Mention:"
							value = arguments.mention.toString()
							inline = true
						}
						field {
							name = "Initial Message:"
							value = if (message != null) "```$message```" else "null"
							inline = message == null
						}
						footer {
							text = user.asUser().username
							icon = user.asUser().avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_BLACK
					}
				}
			}

			ephemeralSubCommand {
				name = "disable"
				description = "Stop automatically creating threads in this channel."

				requirePermission(Permission.ManageChannels)

				check {
					anyGuild()
					hasPermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					// Check if auto-threading is enabled
					if (AutoThreadingCollection().getSingleAutoThread(channel.id) == null) {
						respond {
							content = "Auto-threading is not enabled for this channel."
						}
						return@action
					}

					// Remove the channel from the database as auto-threaded
					AutoThreadingCollection().deleteAutoThread(channel.id)

					// Respond to the user
					respond {
						content = "Auto-threading has been **disabled** in this channel."
					}

					// Log the change
					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!) ?: return@action

					utilityLog.createEmbed {
						title = "Auto-threading Disabled"
						description = null

						field {
							name = "Channel:"
							value = channel.mention
							inline = true
						}
						footer {
							text = user.asUser().username
							icon = user.asUser().avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_BLACK
					}
				}
			}

			ephemeralSubCommand {
				name = "list"
				description = "List all the auto-threaded channels in this server, if any."

				check {
					anyGuild()
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					val autoThreads = AutoThreadingCollection().getAllAutoThreads(guild!!.id)
					var responseContent: String? = null
					autoThreads.forEach {
						responseContent += "\n<#${it.channelId}>"
						if (responseContent!!.length > 4080) {
							responseContent += "(List trimmed.)"
							return@forEach
						}
					}

					respond {
						embed {
							if (responseContent == null) {
								title = "There are no auto-threaded channels in this guild."
								description = "Add new ones by using `/auto-threading enable`"
							} else {
								title = "Auto-threaded channels in this guild:"
								description = responseContent.replace("null", "")
							}
						}
					}
				}
			}

			ephemeralSubCommand(::AutoThreadingViewArgs) {
				name = "view"
				description = "View the settings of an auto-threaded channel"

				requirePermission(Permission.ManageChannels)

				check {
					anyGuild()
					requirePermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					val autoThread = AutoThreadingCollection().getSingleAutoThread(arguments.channel.id)
					if (autoThread == null) {
						respond {
							content = "**Error:** This is not an auto-threaded channel!"
						}
						return@action
					}

					respond {
						embed {
							title = "Auto-Threaded channel settings"
							description = "These are the settings for channel: ${arguments.channel.mention}"
							field {
								name = "Ping role"
								value = if (autoThread.roleId != null) {
									guild!!.getRoleOrNull(autoThread.roleId)?.mention ?: "Unable to get role!"
								} else {
									"None"
								}
							}
							if (autoThread.extraRoleIds.isNotEmpty()) {
								var mentions = ""
								autoThread.extraRoleIds.forEach {
									mentions += "${guild!!.getRoleOrNull(it)?.mention} "
								}
								field {
									name = "Extra Ping Roles"
									value = mentions
								}
							}
							field {
								name = "Prevent duplicates"
								value = autoThread.preventDuplicates.toString()
							}
							field {
								name = "Archive on creation"
								value = autoThread.archive.toString()
							}
							field {
								name = "Content aware naming"
								value = autoThread.contentAwareNaming.toString()
							}
							field {
								name = "Mention creator on creation"
								value = autoThread.mention.toString()
							}
							field {
								name = "Creation message"
								value = autoThread.creationMessage ?: "Default"
							}
							field {
								name = "Add mods and ping role"
								value = autoThread.addModsAndRole.toString()
							}
						}
					}
				}
			}

			ephemeralSubCommand(::ExtraRolesArgs) {
				name = "add-roles"
				description = "Add extra to threads in auto-threaded channels"

				subCommandAdditionalDocumentation {
					extraInformation = "This command will add roles to be pinged alongside the default ping role " +
						"for this auto-threaded channel"
				}

				requirePermission(Permission.ManageChannels)

				check {
					anyGuild()
					hasPermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					val autoThread = AutoThreadingCollection().getSingleAutoThread(event.interaction.channelId)
					if (autoThread == null) {
						respond {
							content = "**Error:** This is not an auto-threaded channel!"
						}
						return@action
					}

					if (!canPingRole(arguments.role, guild!!.id, this@ephemeralSubCommand.kord)) {
						respond {
							content = "Lily cannot mention this role. Please fix the role's permissions and try again."
						}
						return@action
					}

					if (autoThread.extraRoleIds.contains(arguments.role!!.id)) {
						respond {
							content = "This role has already been added."
						}
						return@action
					}

					val updatedRoles = autoThread.extraRoleIds
					updatedRoles.add(arguments.role!!.id)

					AutoThreadingCollection().updateExtraRoles(event.interaction.channelId, updatedRoles)

					respond {
						content = "Role (${arguments.role!!.mention}) added"
					}
				}
			}

			ephemeralSubCommand(::ExtraRolesArgs) {
				name = "remove-roles"
				description = "Remove extra from threads in auto-threaded channels"

				subCommandAdditionalDocumentation {
					extraInformation = "This command will remove roles that have been added to be pinged alongside " +
						"the default ping role for this auto-threaded channel"
				}

				requirePermission(Permission.ManageChannels)

				check {
					anyGuild()
					hasPermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					val autoThread = AutoThreadingCollection().getSingleAutoThread(event.interaction.channelId)
					if (autoThread == null) {
						respond {
							content = "**Error:** This is not an auto-threaded channel!"
						}
						return@action
					}

					if (!autoThread.extraRoleIds.contains(arguments.role!!.id)) {
						respond {
							content = "This role has not been added."
						}
						return@action
					}

					val updatedRoles = autoThread.extraRoleIds
					updatedRoles.remove(arguments.role!!.id)

					AutoThreadingCollection().updateExtraRoles(event.interaction.channelId, updatedRoles)

					respond {
						content = "Role (${arguments.role!!.mention}) removed"
					}
				}
			}
		}

		event<ProxiedMessageCreateEvent> {
			check {
				anyGuild()
				failIf {
					event.pkMessage.sender == kord.selfId ||
							listOf(
								MessageType.ChatInputCommand,
								MessageType.ThreadCreated,
								MessageType.ThreadStarterMessage
							).contains(event.message.type) ||
							listOf(
								ChannelType.GuildNews,
								ChannelType.GuildVoice,
								ChannelType.PublicGuildThread,
								ChannelType.PublicNewsThread
							).contains(event.message.getChannelOrNull()?.type)
				}
			}

			action {
				onMessageSend(event, event.getMessageOrNull(), event.pkMessage)
			}
		}

		event<UnProxiedMessageCreateEvent> {
			check {
				anyGuild()
				failIf {
					event.message.author?.id == kord.selfId ||
							listOf(
								MessageType.ChatInputCommand,
								MessageType.ThreadCreated,
								MessageType.ThreadStarterMessage
							).contains(event.message.type) ||
							listOf(
								ChannelType.GuildNews,
								ChannelType.GuildVoice,
								ChannelType.PublicGuildThread,
								ChannelType.PublicNewsThread
							).contains(event.message.getChannelOrNull()?.type)
				}
			}

			action {
				onMessageSend(event, event.getMessageOrNull())
			}
		}

		event<ThreadChannelCreateEvent> {
			check {
				anyGuild()
				failIf {
					event.channel.ownerId == kord.selfId ||
							event.channel.member != null
				}
			}

			action {
				// fixme this event fires twice for some unknown reason so this is a workaround
				delay(1000)
				if (event.channel.getLastMessage()?.withStrategy(EntitySupplyStrategy.rest) != null) return@action

				val thread = event.channel.asChannelOfOrNull<TextChannelThread>() ?: return@action
				val options = AutoThreadingCollection().getSingleAutoThread(thread.parentId) ?: return@action

				handleThreadCreation(
					options,
					thread,
					event.channel.owner.asUser()
				)
			}
		}
	}

	inner class AutoThreadingArgs : Arguments() {
		val role by optionalRole {
			name = "role"
			description = "The role, if any, to invite to threads created in this channel."
		}

		val addModsAndRole by defaultingBoolean {
			name = "add-mods-and-role"
			description = "Whether to add moderators to the thread alongside the role"
			defaultValue = false
		}

		val preventDuplicates by defaultingBoolean {
			name = "prevent-duplicates"
			description = "If users should be stopped from having multiple open threads in this channel. Default false."
			defaultValue = false
		}

		val archive by defaultingBoolean {
			name = "archive"
			description = "If threads should be archived on creation to avoid filling the sidebar. Default false."
			defaultValue = false
		}

		val contentAwareNaming by defaultingBoolean {
			name = "content-aware-naming"
			description = "If Lily should use content-aware thread titles. Default false"
			defaultValue = false
		}

		val mention by defaultingBoolean {
			name = "mention"
			description = "If the creator should be mentioned in new threads in this channel. Default false."
			defaultValue = false
		}

		val message by defaultingBoolean {
			name = "message"
			description = "Whether to send a custom message on thread creation or not. Default false."
			defaultValue = false
		}
	}

	inner class ExtraRolesArgs : Arguments() {
		val role by optionalRole {
			name = "role"
			description = "A role to invite to threads in this channel"
		}
	}

	inner class AutoThreadingViewArgs : Arguments() {
		val channel by channel {
			name = "channel"
			description = "The channel to view the auto-threading settings for."
		}
	}

	inner class MessageModal : ModalForm() {
		override var title = "Thread Creation Message Configuration"

		val msgInput = paragraphText {
			label = "Thread Creation Message"
			placeholder = "Input the content of the message to send when a thread is created in this channel"
			required = true
		}
	}

	/**
	 * A single function for both Proxied and Non-Proxied message to be turned into threads.
	 *
	 * @param event The event for the message creation
	 * @param message The original message that wasn't proxied
	 * @param proxiedMessage The proxied message, if the message was proxied
	 * @since 4.6.0
	 * @author NoComment1105
	 */
	private suspend fun <T : PKMessageCreateEvent> onMessageSend(
		event: T,
		message: Message?,
		proxiedMessage: PKMessage? = null
	) {
		val memberFromPk = if (proxiedMessage != null) event.getGuild().getMemberOrNull(proxiedMessage.sender) else null

		val channel: TextChannel = if (proxiedMessage == null) {
			message?.channel?.asChannelOfOrNull() ?: return
		} else {
			// Check the real message member too, despite the pk message not being null, we may still be able to use the original
			message?.channel?.asChannelOfOrNull()
				?: try {
					event.getGuild().getChannelOfOrNull(proxiedMessage.channel)
				} catch (_: IllegalArgumentException) {
					null
				} ?: return
		}

		val authorId: Snowflake = if (proxiedMessage == null) {
			message?.author?.id ?: return
		} else {
			message?.author?.id ?: proxiedMessage.sender
		}

		val options = AutoThreadingCollection().getSingleAutoThread(channel.id) ?: return

		var threadName: String? = event.message.content.trim().split("\n").firstOrNull()?.take(75)

		if (!options.contentAwareNaming || threadName == null) {
			threadName = "Thread for ${
				message?.author?.asUser()?.username ?: proxiedMessage?.member?.name
			}".take(75)
		}

		if (options.preventDuplicates) {
			var previousThreadExists = false
			var previousUserThread: ThreadChannel? = null
			val ownerThreads = ThreadsCollection().getOwnerThreads(authorId)

			ownerThreads.forEach {
				val thread = try {
					event.guild?.getChannelOfOrNull<ThreadChannel>(it.threadId)
				} catch (_: IllegalArgumentException) {
					null
				}
				if (thread == null) {
					ThreadsCollection().removeThread(it.threadId)
				} else if (thread.parentId == channel.id && !thread.isArchived) {
					previousThreadExists = true
					previousUserThread = thread
				}
			}

			if (previousThreadExists) {
				val response = event.message.respond {
					// There is a not-null call because the compiler knows it's not null if the boolean is true.
					content = "Please use your existing thread in this channel ${previousUserThread!!.mention}"
				}
				event.message.delete("User already has a thread")
				response.delete(10000L, false)
				return
			}
		}

		val thread = channel.startPublicThreadWithMessage(
			message?.id ?: proxiedMessage!!.channel,
			threadName
		) {
			autoArchiveDuration = channel.data.defaultAutoArchiveDuration.value ?: ArchiveDuration.Day
		}

		ThreadsCollection().setThreadOwner(event.getGuild().id, thread.id, event.member!!.id, channel.id)

		handleThreadCreation(
			options,
			thread,
			message?.author ?: memberFromPk!!.asUser()
		)
	}

	private suspend inline fun handleThreadCreation(
		inputOptions: AutoThreadingData,
		inputThread: TextChannelThread,
		inputUser: User
	) {
		val threadMessage = inputThread.createMessage(if (inputOptions.mention) inputUser.mention else "Placeholder")

		if (inputOptions.roleId != null) {
			val role = inputThread.guild.getRole(inputOptions.roleId)
			val moderatorRoleId = ModerationConfigCollection().getConfig(inputThread.guildId)?.role
			var moderatorRole: Role? = null
			if (moderatorRoleId != null) {
				moderatorRole = inputThread.guild.getRole(moderatorRoleId)
			}
			var mentions = ""
			inputOptions.extraRoleIds.forEach {
				mentions += inputThread.guild.getRole(it).mention
			}

			if (moderatorRole != null && moderatorRole.mentionable && inputOptions.addModsAndRole) {
				threadMessage.edit {
					content = role.mention + mentions + moderatorRole.mention
				}
			} else {
				threadMessage.edit {
					content = role.mention + mentions
				}
			}
		}

		if (inputOptions.creationMessage != null) {
			threadMessage.edit {
				content =
					if (inputOptions.mention) {
						inputUser.mention + " " + inputOptions.creationMessage
					} else {
						inputOptions.creationMessage
					}
			}
		} else {
			threadMessage.delete()
		}

		if (inputOptions.archive) {
			inputThread.edit {
				archived = true
				reason = "Initial thread creation"
			}
		}
	}
}
