package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalColour
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
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

				try {
					if (arguments.embed) {
						targetChannel.createEmbed {
							color = arguments.color
							description = arguments.message
							if (arguments.timestamp) {
								timestamp = Clock.System.now()
							}
						}
					} else {
						targetChannel.createMessage {
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
				}
			}
		}

		/**
		 * Message editing command
		 *
		 * @since 3.3.0
		 */
		ephemeralSlashCommand(::SayEditMessageArgs) {
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

				val message = channelOfMessage.getMessage(arguments.messageToEdit)

				// If the message is not by LilyBot, return
				if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
					respond { content = "This message is not by me, I cannot edit this!" }
					return@action
				}

				message.edit { content = arguments.newContent }

				respond { content = "Message edited" }
			}
		}

		/**
		 * Embed editing command
		 *
		 * @since 3.3.0
		 */
		ephemeralSlashCommand(::SayEditEmbedArgs) {
			name = "edit-say-embed"
			description = "Edit an embed created by /say"

			check {
				anyGuild()
				configPresent()
				hasPermission(Permission.ModerateMembers)
			}

			action {
				// The channel the message was sent in. Either the channel provided, or if null, the channel the
				// command was executed in.
				val channelOfMessage = if (arguments.channelOfEmbed != null) {
					guild!!.getChannel(arguments.channelOfEmbed!!.id) as MessageChannelBehavior
				} else {
					channel
				}

				// The messages that contains the embed that is going to be edited. If the message has no embed, or
				// it's not by LilyBot, it returns
				val messageContainingEmbed = channelOfMessage.getMessage(arguments.embedToEdit)
				if (messageContainingEmbed.embeds.isEmpty()) {
					respond { content = "This message does not contain an embed" }
					return@action
				} else if (messageContainingEmbed.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
					respond { content = "This message is not by me! I cannot edit it" }
					return@action
				}

				// The old description and color to the embed. We get it here before we start changing it.
				val oldDescription = messageContainingEmbed.embeds[0].description
				val oldColor = messageContainingEmbed.embeds[0].color

				messageContainingEmbed.edit {
					embed {
						description = if (arguments.newContent != null) arguments.newContent else oldDescription
						color = if (arguments.newColor != null) arguments.newColor else oldColor
						timestamp = if (arguments.timestamp) messageContainingEmbed.timestamp else null
					}
				}

				respond { content = "Embed updated" }
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

	inner class SayEditMessageArgs : Arguments() {
		/** The ID of the message to edit. */
		val messageToEdit by snowflake {
			name = "messageToEdit"
			description = "The ID of the message to edit"
		}

		/** The new content of the message. */
		val newContent by string {
			name = "newContent"
			description = "The new contents of the message"

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n", "\n")
			}
		}

		/** The channel the message was originally sent in. */
		val channelOfMessage by optionalChannel {
			name = "channelOfMessage"
			description = "The channel the message was originally sent in"
		}
	}

	inner class SayEditEmbedArgs : Arguments() {
		/** The ID of the embed to edit. */
		val embedToEdit by snowflake {
			name = "embedToEdit"
			description = "The ID of the embed you'd like to edit"
		}

		/** Whether to add the timestamp of when the message was originally sent or not. */
		val timestamp by boolean {
			name = "timestamp"
			description = "Whether to add the timestamp of when the message was originally sent or not"
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
		val channelOfEmbed by optionalChannel {
			name = "channelOfEmbed"
			description = "The channel of the embed"
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
