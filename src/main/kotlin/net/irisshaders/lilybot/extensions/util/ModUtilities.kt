package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_WHITE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalColour
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Permission
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.modify.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.responseEmbedInChannel

/**
 * This class contains a few utility commands that can be used by moderators. They all require a guild to be run.
 *
 * @since 3.1.0
 */
class ModUtilities : Extension() {
	override val name = "mod-utilities"

	override suspend fun setup() {
		/**
		 * Say Command
		 * @author NoComment1105, tempest15
		 * @since 2.0
		 */
		ephemeralSlashCommand(::SayArgs) {
			name = "say"
			description = "Say something through Lily."

			check {
				anyGuild()
				configPresent()
				hasPermission(Permission.ModerateMembers)
			}
			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannel(config.modActionLog) as GuildMessageChannelBehavior
				val targetChannel =
					if (arguments.channel != null) {
						// This odd syntax is necessary for casting to MessageChannelBehavior
						guild!!.getChannel(arguments.channel!!.id) as MessageChannelBehavior
					} else {
						channel
					}
				val createdMessage: Message

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
				} catch (e: KtorRequestException) {
					respond { content = "Lily does not have permission to send messages in this channel." }
					return@action
				}

				respond { content = "Message sent." }

				actionLog.createEmbed {
					title = "Say command used"
					description = "```${arguments.message}```"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()
					field {
						name = "Channel:"
						value = targetChannel.mention
						inline = true
					}
					field {
						name = "Type:"
						value = if (arguments.embed) "Embed" else "Message"
						inline = true
					}
					if (arguments.embed) {
						field {
							name = "Color:"
							value = arguments.color.toString()
							inline = true
						}
					}
					footer {
						text = user.asUser().tag
						icon = user.asUser().avatar?.url
					}
				}.edit {
					components {
						linkButton {
							label = "Jump to message"
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
			name = "edit-say-message"
			description = "Edit a message created by /say"

			check {
				anyGuild()
				configPresent()
				hasPermission(Permission.ModerateMembers)
			}

			action {
				// The channel the message was sent in. Either the channel provided, or if null, the channel the
				// command was executed in.
				val channelOfMessage = if (arguments.channelOfMessage != null) {
					guild!!.getChannel(arguments.channelOfMessage!!.id) as MessageChannelBehavior
				} else {
					channel
				}

				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannel(config.modActionLog) as GuildMessageChannelBehavior

				// The messages that contains the embed that is going to be edited. If the message has no embed, or
				// it's not by LilyBot, it returns
				val message = channelOfMessage.getMessage(arguments.messageToEdit)
				val originalContent = message.content
				if (message.embeds.isEmpty()) {
					if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
						respond { content = "I did not send this message, I cannot edit this!" }
						return@action
					} else if (arguments.newContent == null) {
						respond { content = "Please specify a new message content" }
						return@action
					}

					message.edit { content = arguments.newContent }

					respond { content = "Message edited" }

					actionLog.createEmbed {
						title = "Say message edited"
						color = DISCORD_WHITE
						timestamp = Clock.System.now()
						field {
							name = "Original Content"
							value = "```$originalContent```"
						}
						field {
							name = "New Content"
							value = "```${arguments.newContent}```"
						}
						footer {
							text = "Edited by ${user.asUser().tag}"
							icon = user.asUser().avatar?.url
						}
					}.edit {
						components {
							linkButton {
								label = "Jump to message"
								url = message.getJumpUrl()
							}
						}
					}
				} else {
					if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
						respond { content = "I did not send this message, I cannot edit this!" }
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
							timestamp = if (arguments.timestamp) message.timestamp else oldTimestamp
						}
					}

					respond { content = "Embed updated" }

					actionLog.createEmbed {
						title = "Say message edited"
						color = DISCORD_WHITE
						timestamp = Clock.System.now()
						field {
							name = "Original content"
							value = "```${oldContent ?: "none"}```"
						}
						field {
							name = "New content"
							value = "```${arguments.newContent ?: oldContent ?: "none"}```"
						}
						field {
							name = "Old color"
							value = oldColor.toString()
						}
						field {
							name = "New color"
							value =
								if (arguments.newColor != null) arguments.newColor.toString() else oldColor.toString()
						}
						field {
							name = "Has Timestamp"
							value = arguments.timestamp.toString()
						}
						footer {
							text = "Edited by ${user.asUser().tag}"
							icon = user.asUser().avatar?.url
						}
					}.edit {
						components {
							linkButton {
								label = "Jump to message"
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
			name = "set-status"
			description = "Set Lily's current presence/status."

			check {
				hasPermission(Permission.Administrator)
				configPresent()
			}

			action {
				// Lock this command to the testing guild
				if (guild?.id != TEST_GUILD_ID) {
					respond { content = "**Error:** This command can only be run in Lily's testing guild." }
					return@action
				}

				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior

				// Update the presence in the action
				this@ephemeralSlashCommand.kord.editPresence {
					status = PresenceStatus.Online
					playing(arguments.presenceArgument)
				}

				// Store the new presence in the database for if there is a restart
				DatabaseHelper.setStatus(arguments.presenceArgument)

				respond { content = "Presence set to `${arguments.presenceArgument}`" }

				responseEmbedInChannel(
					actionLog,
					"Presence Changed",
					"Lily's presence has been set to `${arguments.presenceArgument}`",
					DISCORD_BLACK,
					user.asUser()
				)
			}
		}
	}

	inner class SayArgs : Arguments() {
		/** The message the user wishes to send. */
		val message by string {
			name = "message"
			description = "The text of the message to be sent."

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
			}
		}

		/** The channel to aim the message at. */
		val channel by optionalChannel {
			name = "channel"
			description = "The channel the message should be sent in."
		}

		/** Whether to embed the message or not. */
		val embed by defaultingBoolean {
			name = "embed"
			description = "If the message should be sent as an embed."
			defaultValue = false
		}

		/** If the embed should have a timestamp. */
		val timestamp by defaultingBoolean {
			name = "timestamp"
			description = "If the message should be sent with a timestamp. Only works with embeds."
			defaultValue = true
		}

		/** What color the embed should be. */
		val color by defaultingColor {
			name = "color"
			description = "The color of the embed. Can be either a hex code or one of Discord's supported colors. " +
					"Embeds only"
			defaultValue = DISCORD_BLURPLE
		}
	}

	inner class SayEditArgs : Arguments() {
		/** The ID of the embed to edit. */
		val messageToEdit by snowflake {
			name = "messageToEdit"
			description = "The ID of the embed you'd like to edit"
		}

		/** The new content of the embed. */
		val newContent by optionalString {
			name = "newContent"
			description = "The new content of the embed"

			mutate {
				it?.replace("\\n", "\n")
					?.replace("\n", "\n")
			}
		}

		/** The new color for the embed. */
		val newColor by optionalColour {
			name = "newColor"
			description = "The new color of the embed"
		}

		/** The channel the embed was originally sent in. */
		val channelOfMessage by optionalChannel {
			name = "channelOfEmbed"
			description = "The channel of the embed"
		}

		/** Whether to add the timestamp of when the message was originally sent or not. */
		val timestamp by defaultingBoolean {
			name = "timestamp"
			description = "Whether to add the timestamp of when the message was originally sent or not"
			defaultValue = false
		}
	}

	inner class PresenceArgs : Arguments() {
		/** The new presence set by the command user. */
		val presenceArgument by string {
			name = "presence"
			description = "The new value Lily's presence should be set to"
		}
	}
}
