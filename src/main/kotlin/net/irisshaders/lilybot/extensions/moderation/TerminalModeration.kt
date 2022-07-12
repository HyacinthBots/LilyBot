package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import mu.KotlinLogging
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.baseModerationEmbed
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.dmNotificationStatusEmbedField
import net.irisshaders.lilybot.utils.isBotOrModerator

/**
 * The class for permanent moderation actions, such as ban and kick.
 *
 * @since 3.0.0
 */
class TerminalModeration : Extension() {
	override val name = "terminal-moderation"

	override suspend fun setup() {
		val logger = KotlinLogging.logger("Terminal Moderation Logger")

		/**
		 * Ban command
		 * @author IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::BanArgs) {
			name = "ban"
			description = "Bans a user."

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers, Permission.ManageMessages)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument

				// Clarify the user is not a bot or moderator
				isBotOrModerator(userArg, "ban") ?: return@action

				// DM the user before the ban task is run, to avoid error, null if fails
				val dm = userArg.dm {
					embed {
						title = "You have been banned from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}"
					}
				}

				try {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // remove timeout if they had a timeout when banned
				} catch (e: EntityNotFoundException) {
					logger.info("Unable to find user! Skipping timeout removal")
				}

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				// Run the ban task
				guild?.ban(userArg.id, builder = {
					this.reason = arguments.reason
					this.deleteMessagesDays = arguments.messages
				})

				respond {
					content = "Banned a user"
				}

				val embed = EmbedBuilder()
				embed.color = DISCORD_BLACK
				embed.title = "Banned a user"
				embed.description = "${userArg.mention} has been banned!"
				embed.image = arguments.image
				embed.baseModerationEmbed(arguments.reason, userArg, user)
				embed.dmNotificationStatusEmbedField(dm)
				embed.timestamp = Clock.System.now()
				embed.field {
					name = "Days of messages deleted:"
					value = arguments.messages.toString()
					inline = false
				}

				try {
					actionLog?.createMessage { embeds.add(embed) }
				} catch (e: KtorRequestException) {
					embed.image = null
					actionLog?.createMessage { embeds.add(embed) }
				}
			}
		}

		/**
		 *  Unban command
		 *  @author NoComment1105
		 *  @since 2.0
		 */
		ephemeralSlashCommand(::UnbanArgs) {
			name = "unban"
			description = "Unbans a user"

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument
				// Get all the bans into a list
				val bans = guild!!.bans.toList().map { it.userId }

				// Search the list for the banned user
				if (userArg.id in bans) {
					// Unban the user if they're banned
					guild?.unban(userArg.id)
				} else {
					// Respond with an error if they aren't
					respond { content = "**Error:** User is not banned" }
					return@action
				}
				respond {
					content = "Unbanned user"
				}

				actionLog?.createEmbed {
					title = "Unbanned a user"
					description = "${userArg.mention} has been unbanned!\n${userArg.id} (${userArg.tag})"
					field {
						name = "Reason:"
						value = arguments.reason
					}
					footer {
						text = user.asUser().tag
						icon = user.asUser().avatar?.url
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}

		/**
		 * Soft ban command
		 * @author NoComment1105
		 * @since 2.0
		 */
		ephemeralSlashCommand(::SoftBanArgs) {
			name = "soft-ban"
			description = "Soft-bans a user"

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers, Permission.ManageMessages)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument

				isBotOrModerator(userArg, "soft-ban") ?: return@action

				// DM the user before the ban task is run
				val dm = userArg.dm {
					embed {
						title = "You have been soft-banned from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}\n\n" +
								"You are free to rejoin without the need to be unbanned"
					}
				}

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				try {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // Remove timeout if they had a timeout when banned
				} catch (e: EntityNotFoundException) {
					logger.info("Unable to find user! Skipping timeout removal")
				}

				// Ban the user, mark it as a soft-ban clearly
				guild?.ban(userArg.id, builder = {
					this.reason = "${arguments.reason} + **SOFT-BAN**"
					this.deleteMessagesDays = arguments.messages
				})

				respond {
					content = "Soft-Banned User"
				}

				val embed = EmbedBuilder()
				embed.color = DISCORD_BLACK
				embed.title = "Soft-Banned a user"
				embed.description = "${userArg.mention} has been soft-banned!"
				embed.image = arguments.image
				embed.baseModerationEmbed(arguments.reason, userArg, user)
				embed.dmNotificationStatusEmbedField(dm)
				embed.timestamp = Clock.System.now()
				embed.field {
					name = "Days of messages deleted"
					value = arguments.messages.toString()
					inline = false
				}

				try {
					actionLog?.createMessage { embeds.add(embed) }
				} catch (e: KtorRequestException) {
					embed.image = null
					actionLog?.createMessage { embeds.add(embed) }
				}

				// Unban the user, as you're supposed to in soft-ban
				guild?.unban(userArg.id)
			}
		}

		/**
		 * Kick command
		 * @author IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::KickArgs) {
			name = "kick"
			description = "Kicks a user."

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.KickMembers)
				requireBotPermissions(Permission.KickMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument

				// Clarify the user isn't a bot or a moderator
				isBotOrModerator(userArg, "kick") ?: return@action
				// DM the user about it before the kick
				val dm = userArg.dm {
					embed {
						title = "You have been kicked from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}"
					}
				}

				try {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // Remove timeout if they had a timeout when kicked
				} catch (e: EntityNotFoundException) {
					logger.info("Unable to find user! Skipping timeout removal")
				}

				// Run the kick task
				guild?.kick(userArg.id, arguments.reason)

				respond {
					content = "Kicked User"
				}

				val embed = EmbedBuilder()
				embed.color = DISCORD_BLACK
				embed.title = "Kicked a user"
				embed.description = "${userArg.mention} has been kicked!"
				embed.image = arguments.image
				embed.baseModerationEmbed(arguments.reason, userArg, user)
				embed.dmNotificationStatusEmbedField(dm)
				embed.timestamp = Clock.System.now()

				try {
					actionLog?.createMessage { embeds.add(embed) }
				} catch (e: KtorRequestException) {
					embed.image = null
					actionLog?.createMessage { embeds.add(embed) }
				}
			}
		}
	}

	inner class KickArgs : Arguments() {
		/** The user to kick. */
		val userArgument by user {
			name = "kickUser"
			description = "Person to kick"
		}

		/** The reason for the kick. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the Kick"
			defaultValue = "No reason provided"
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalString {
			name = "image"
			description = "The URL to an image you'd like to provide as extra context for the action"
		}
	}

	inner class BanArgs : Arguments() {
		/** The user to ban. */
		val userArgument by user {
			name = "banUser"
			description = "Person to ban"
		}

		/** The number of days worth of messages to delete. */
		val messages by int {
			name = "messages"
			description = "Messages"
		}

		/** The reason for the ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No reason provided"
		}

		/** An image that the user wishes to provide for context to the ban. */
		val image by optionalString {
			name = "image"
			description = "The URL to an image you'd like to provide as extra context for the action"
		}
	}

	inner class UnbanArgs : Arguments() {
		/** The ID of the user to unban. */
		val userArgument by user {
			name = "unbanUserId"
			description = "Person to un-ban"
		}

		/** The reason for the un-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the un-ban"
			defaultValue = "No reason provided"
		}
	}

	inner class SoftBanArgs : Arguments() {
		/** The user to soft-ban. */
		val userArgument by user {
			name = "softBanUser"
			description = "Person to Soft ban"
		}

		/** The number of days worth of messages to delete, defaults to 3 days. */
		val messages by defaultingInt {
			name = "messages"
			description = "Messages"
			defaultValue = 3
		}

		/** The reason for the soft-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No reason provided"
		}

		/** An image that the user wishes to provide for context to the soft-ban. */
		val image by optionalString {
			name = "image"
			description = "The URL to an image you'd like to provide as extra context for the action"
		}
	}
}
