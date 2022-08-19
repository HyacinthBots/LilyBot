package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingEnum
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.botHasChannelPerms
import net.irisshaders.lilybot.utils.configPresent

// This code solves the following issues:
// Customize support message - https://github.com/IrisShaders/LilyBot/issues/138
// Multiple support threads - https://github.com/IrisShaders/LilyBot/issues/178
// Better support thread naming system - https://github.com/IrisShaders/LilyBot/issues/182
// Multiple support channels - https://github.com/IrisShaders/LilyBot/issues/194

// Yes this is no longer MPL, I rewrote it from scratch

class AutoThreading : Extension() {
	override val name = "auto-threading"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "auto-threading"
			description = "The parent command for auto-threading management."

			ephemeralSubCommand(::AutoThreadingArgs) {
				name = "enable"
				description = "Automatically create a thread for each message sent in this channel."

				check {
					anyGuild()
					configPresent()
					hasPermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}
				action {
					// Check if the auto-threading is disabled
					if (getAutoThread(channel.id) != null) {
						respond {
							content = "Auto-threading is already enabled for this channel."
						}
						return@action
					}

					// Add the channel to the database as auto-threaded
					setAutoThread(
						AutoThreadingData(
							channelId = channel.id,
							roleId = arguments.role?.id,
							allowDuplicates = arguments.allowDuplicates,
							archive = arguments.archive,
							namingScheme = arguments.namingScheme,
							mention = arguments.mention,
							creationMessage = arguments.message
						)
					)

					// Respond to the user
					respond {
						content = "Auto-threading has been **enabled** in this channel."
					}

					// Log the change
					val config = DatabaseHelper.getConfig(guild!!.id) ?: return@action
					val modActionLog = guild!!.getChannelOfOrNull<TextChannel>(config.modActionLog)

					modActionLog?.createEmbed {
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
							name = "Allow Duplicates:"
							value = arguments.allowDuplicates.toString()
							inline = true
						}
						field {
							name = "Begin Archived:"
							value = arguments.archive.toString()
							inline = true
						}
						field {
							name = "Naming Scheme:"
							value = arguments.namingScheme.toString()
							inline = true
						}
						field {
							name = "Mention:"
							value = arguments.mention.toString()
							inline = true
						}
						field {
							name = "Initial Message:"
							value = if (arguments.message != null) "```${arguments.message}```" else "null"
							inline = arguments.message == null
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_BLACK
					}
				}
			}

			ephemeralSubCommand {
				name = "disable"
				description = "Stop automatically creating threads in this channel."

				check {
					anyGuild()
					configPresent()
					hasPermission(Permission.ManageChannels)
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}
				action {
					// Check if auto-threading is enabled
					if (getAutoThread(channel.id) == null) {
						respond {
							content = "Auto-threading is not enabled for this channel."
						}
						return@action
					}

					// Remove the channel from the database as auto-threaded
					deleteAutoThread(channel.id)

					// Respond to the user
					respond {
						content = "Auto-threading has been **disabled** in this channel."
					}

					// Log the change
					val config = DatabaseHelper.getConfig(guild!!.id) ?: return@action
					val modActionLog = guild!!.getChannelOfOrNull<TextChannel>(config.modActionLog)

					modActionLog?.createEmbed {
						title = "Auto-threading Disabled"
						description = null

						field {
							name = "Channel:"
							value = channel.mention
							inline = true
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_BLACK
					}
				}
			}

			// todo Command to list all auto-threaded channels in a guild
		}

		// todo Use PluralKit module
		event<MessageCreateEvent> {
			check {
				anyGuild()
				configPresent()
				failIf {
					// todo This can probably be tidied further
					event.message.author?.id == kord.selfId ||
							event.message.type in listOf(
						MessageType.ChatInputCommand,
						MessageType.ThreadCreated,
						MessageType.ThreadStarterMessage
					) ||
							event.message.getChannelOrNull()?.type in listOf(
						ChannelType.GuildNews,
						ChannelType.GuildVoice,
						ChannelType.PublicGuildThread,
						ChannelType.PublicNewsThread
					)
				}
			}
			action {
				val eventMessage = event.message
				val channel = eventMessage.channel.asChannelOf<TextChannel>()
				val authorId = eventMessage.author?.id ?: return@action

				val options = getAutoThread(channel.id) ?: return@action

				// todo Implement naming schemes properly
				val threadName = when (options.namingScheme) {
					ThreadNamingSchemes.SUPPORT -> "support"
					ThreadNamingSchemes.CONTENT -> "content"
					ThreadNamingSchemes.USERNAME -> eventMessage.author?.username ?: "username"
				}

				if (!options.allowDuplicates) {
					var previousUserThread: TextChannelThread? = null
					val ownerThreads = DatabaseHelper.getOwnerThreads(authorId)

					// todo If we include channel ID in ThreadData we can improve this code by using .find { }
					ownerThreads.forEach {
						val ownedThread = event.getGuild()?.getChannelOfOrNull<TextChannelThread>(it.threadId)
						if (ownedThread?.parentId == channel.id) {
							previousUserThread = ownedThread
							return@forEach
						}
					}

					if (previousUserThread != null) {
						val response = event.message.respond {
							content = "Please use your existing thread in this channel ${previousUserThread!!.mention}"
						}
						event.message.delete("User already has a thread")
						response.delete(10000L, false)
						return@action
					}
				}

				val thread = channel.startPublicThreadWithMessage(
					eventMessage.id,
					threadName,
					channel.data.defaultAutoArchiveDuration.value ?: ArchiveDuration.Day
				)

				DatabaseHelper.setThreadOwner(thread.id, authorId)

				val threadMessage = thread.createMessage(
					if (options.mention) {
						eventMessage.author!!.mention
					} else "message"
				)

				if (options.roleId != null) {
					val role = event.getGuild()?.getRole(options.roleId)
					threadMessage.edit {
						this.content += role?.mention
					}
				}

				messageAndArchive(
					options,
					thread,
					threadMessage,
					eventMessage.author!!
				)
			}
		}

		event<ThreadChannelCreateEvent> {
			check {
				anyGuild()
				configPresent()
				failIf {
					event.channel.ownerId == kord.selfId
				}
			}
			action {
				val thread = event.channel.asChannelOfOrNull<TextChannelThread>() ?: return@action
				val options = getAutoThread(thread.parentId) ?: return@action

				// fixme this is being done twice for some reason
				val threadMessage = thread.createMessage(
					if (options.mention) {
						event.channel.owner.mention
					} else "message"
				)

				if (options.roleId != null) {
					val role = event.channel.guild.getRole(options.roleId)
					threadMessage.edit {
						this.content = role.mention
					}
				}

				messageAndArchive(
					options,
					thread,
					threadMessage,
					event.channel.owner.asUser()
				)
			}
		}
	}

	inner class AutoThreadingArgs : Arguments() {
		// todo Check that the role can be pinged
		val role by optionalRole {
			name = "role"
			description = "The role, if any, to invite to threads created in this channel."
		}

		val allowDuplicates by defaultingBoolean {
			name = "allow-duplicates"
			description = "If users should be prevented from having multiple threads open in this channel. " +
					"Default true."
			defaultValue = true
		}

		val archive by defaultingBoolean {
			name = "archive"
			description = "If threads should be archived on creation to avoid filling the sidebar. Default false."
			defaultValue = false
		}

		// todo make this auto-complete options
		val namingScheme by defaultingEnum<ThreadNamingSchemes> {
			name = "naming-scheme"
			description = "The method for naming threads in this channel."
			defaultValue = ThreadNamingSchemes.USERNAME
			typeName = "foo" // todo what does this do?
		}

		val mention by defaultingBoolean {
			name = "mention"
			description = "If the user should be mentioned at the beginning of new threads in this channel."
			defaultValue = false
		}

		val message by optionalString {
			name = "message"
			description = "The message, if any, to send at the beginning of new threads in this channel."
		} // todo trim this so as not to go over message size limits
	}

	enum class ThreadNamingSchemes {
		SUPPORT, USERNAME, CONTENT
	}

	private suspend inline fun messageAndArchive(
		inputOptions: AutoThreadingData,
		inputThread: TextChannelThread,
		inputThreadMessage: Message,
		inputUser: User
	) {
		if (inputOptions.creationMessage != null) {
			inputThreadMessage.edit {
				content =
					if (inputOptions.mention) {
						inputUser.mention + " " + inputOptions.creationMessage
					} else {
						inputOptions.creationMessage
					}
			}
		} else {
			inputThreadMessage.delete("Initial thread creation")
		}

		if (inputOptions.archive) {
			inputThread.edit {
				archived = true
				reason = "Initial thread creation"
			}
		}
	}
}
