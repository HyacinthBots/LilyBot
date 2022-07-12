package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.WarnCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.baseModerationEmbed
import net.irisshaders.lilybot.utils.botHasChannelPerms
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.dmNotificationStatusEmbedField
import net.irisshaders.lilybot.utils.isBotOrModerator
import java.lang.Integer.min
import kotlin.time.Duration

/**
 * The class for temporary moderation actions, such as timeouts and warnings.
 *
 * @since 3.0.0
 */
class TemporaryModeration : Extension() {
	override val name = "temporary-moderation"

	override suspend fun setup() {
		/**
		 * Clear Command
		 * @author IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::ClearArgs) {
			name = "clear"
			description = "Clears messages."

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ManageMessages)
				requireBotPermissions(Permission.ManageMessages)
				botHasChannelPerms(Permissions(Permission.ManageMessages))
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!

				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val messageAmount = arguments.messages
				val textChannel = channel.asChannelOf<GuildMessageChannel>()

				// Get the specified amount of messages into an array list of Snowflakes and delete them
				val messages = channel.withStrategy(EntitySupplyStrategy.rest).getMessagesBefore(
					Snowflake.max, min(messageAmount, 100)
				).map { it.id }.toList()

				textChannel.bulkDelete(messages)

				respond {
					content = "Messages cleared."
				}

				actionLog?.createEmbed {
					title = "$messageAmount messages have been cleared."
					description = "Action occurred in ${textChannel.mention}"
					footer {
						text = user.asUser().tag
						icon = user.asUser().avatar?.url
					}
					color = DISCORD_BLACK
				}
			}
		}

		/**
		 * Warn Command
		 * @author chalkyjeans, Miss-Corruption, NoComment1105
		 * @since 2.0.0
		 */
		ephemeralSlashCommand(::WarnArgs) {
			name = "warn"
			description = "Warn a member for any infractions."

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument

				isBotOrModerator(userArg, "warn") ?: return@action

				WarnCollection().setWarn(userArg.id, guild!!.id, false)
				val newStrikes = WarnCollection().getWarn(userArg.id, guild!!.id)?.strikes

				respond {
					content = "Warned user."
				}

				var dm: Message? = null
				// Check the amount of points before running sanctions and dming the user
				if (newStrikes == 1) {
					dm = userArg.dm {
						embed {
							title = "First warning in ${guild?.fetchGuild()?.name}"
							description = "**Reason:** ${arguments.reason}\n\n" +
									"No moderation action has been taken. Please consider your actions carefully.\n\n" +
									"For more information about the warn system, please see [this document]" +
									"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)"
							color = DISCORD_BLACK
						}
					}
				} else if (newStrikes == 2) {
					dm = userArg.dm {
						embed {
							title = "Second warning and timeout in ${guild?.fetchGuild()?.name}"
							description = "**Reason:** ${arguments.reason}\n\n" +
									"You have been timed out for 3 hours. Please consider your actions carefully.\n\n" +
									"For more information about the warn system, please see [this document]" +
									"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)"
							color = DISCORD_BLACK
						}
					}

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT3H"))
					}

					actionLog?.createEmbed {
						title = "Timeout"
						description = "${userArg.mention} has been timed-out for 3 hours due to $newStrikes warn " +
								"strikes\n${userArg.id} (${userArg.tag}) Reason: ${arguments.reason}"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						color = DISCORD_BLACK
					}
				} else if (newStrikes == 3) {
					userArg.dm {
						embed {
							title = "Third warning and timeout in ${guild!!.fetchGuild().name}"
							description =
								"You have been timed out for 12 hours. Please consider your actions carefully.\n\n" +
										"For more information about the warn system, please see [this document]" +
										"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)"
							color = DISCORD_RED
						}
					}

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT12H"))
					}

					actionLog?.createEmbed {
						title = "Timeout"
						description = "${userArg.mention} has been timed-out for 12 hours due to $newStrikes warn " +
								"strikes\n${userArg.id} (${userArg.tag}) Reason: ${arguments.reason}"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						color = DISCORD_BLACK
					}
				} else if (newStrikes != null) {
					if (newStrikes > 3) {
						dm = userArg.dm {
							embed {
								title = "Warning number $newStrikes and timeout in ${guild!!.fetchGuild().name}"
								description =
									"You have been timed out for 3 days. Please consider your actions carefully.\n\n" +
											"For more information about the warn system, please see [this document]" +
											"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)"
								color = DISCORD_RED
							}
						}

						guild?.getMember(userArg.id)?.edit {
							timeoutUntil = Clock.System.now().plus(Duration.parse("PT72H"))
						}

						actionLog?.createEmbed {
							title = "Timeout"
							description = "${userArg.mention} has been timed-out for 3 days due to $newStrikes warn " +
									"strike\n${userArg.id} (${userArg.tag})\nIt might be time to consider other " +
									"action. Reason: ${arguments.reason}"
							footer {
								text = user.asUser().tag
								icon = user.asUser().avatar?.url
							}
							color = DISCORD_BLACK
						}
					}
				}

				val embed = EmbedBuilder()
				embed.color = DISCORD_BLACK
				embed.title = "Warning"
				embed.image = arguments.image
				embed.baseModerationEmbed(arguments.reason, userArg, user)
				embed.dmNotificationStatusEmbedField(dm)
				embed.timestamp = Clock.System.now()
				embed.field {
					name = "Total Strikes:"
					value = newStrikes.toString()
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
		 * Remove warn command
		 *
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		ephemeralSlashCommand(::RemoveWarnArgs) {
			name = "remove-warn"
			description = "Remove a warning strike from a user"

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument

				val targetUser = guild?.getMember(userArg.id)
				val userStrikes = WarnCollection().getWarn(targetUser!!.id, guild!!.id)?.strikes
				if (userStrikes == 0 || userStrikes == null) {
					respond {
						content = "This user does not have any warning strikes!"
					}
					return@action
				}

				WarnCollection().setWarn(userArg.id, guild!!.id, true)
				val newStrikes = WarnCollection().getWarn(userArg.id, guild!!.id)

				respond {
					content = "Removed strike from user"
				}

				val dm = userArg.dm {
					embed {
						title = "Warn strike removal in ${guild?.fetchGuild()?.name}"
						description = "You have had a warn strike removed. You now have $newStrikes strikes."
						color = DISCORD_GREEN
					}
				}

				actionLog?.createEmbed {
					title = "Warning Removal"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()

					baseModerationEmbed(null, userArg, user)
					field {
						name = "Total Strikes:"
						value = newStrikes.toString()
						inline = false
					}
					dmNotificationStatusEmbedField(dm)
				}
			}
		}

		/**
		 * Timeout command
		 *
		 * @author NoComment1105, IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::TimeoutArgs) {
			name = "timeout"
			description = "Timeout a user"

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.UTC)

				// Clarify the user is not bot or a moderator
				isBotOrModerator(userArg, "timeout") ?: return@action

				try {
					// Run the timeout task
					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = duration
					}
				} catch (e: KtorRequestException) {
					respond {
						content = "Sorry, I can't timeout this person! Try doing the timeout manually instead!"
					}
				}

				// Send the DM after the timeout task, in case Lily doesn't have required permissions
				// DM the user about it
				val dm = userArg.dm {
					embed {
						title = "You have been timed out in ${guild?.fetchGuild()?.name}"
						description = "**Duration:**\n${
							duration.toDiscord(TimestampType.Default) + "(" + arguments.duration.toString()
								.replace("PT", "") + ")"
						}\n**Reason:**\n${arguments.reason}"
					}
				}

				respond {
					content = "Timed out ${userArg.id}"
				}

				val embed = EmbedBuilder()
				embed.color = DISCORD_BLACK
				embed.title = "Timeout"
				embed.image = arguments.image
				embed.baseModerationEmbed(arguments.reason, userArg, user)
				embed.dmNotificationStatusEmbedField(dm)
				embed.timestamp = Clock.System.now()
				embed.field {
					name = "Duration:"
					value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
						.replace("PT", "") + ")"
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
		 * Timeout removal command
		 *
		 * @author IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::RemoveTimeoutArgs) {
			name = "remove-timeout"
			description = "Remove timeout on a user"

			check {
				anyGuild()
				configPresent(ConfigType.MODERATION)
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
				val userArg = arguments.userArgument

				// Set timeout to null, or no timeout
				guild?.getMember(userArg.id)?.edit {
					timeoutUntil = null
				}

				respond {
					content = "Removed timeout on ${userArg.id}"
				}

				actionLog?.createEmbed {
					title = "Timeout Removed"
					field {
						name = "User:"
						value = "${userArg.tag} \n${userArg.id}"
						inline = false
					}
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
					timestamp = Clock.System.now()
					color = DISCORD_BLACK
				}
			}
		}

		/**
		 * Server and channel locking commands
		 *
		 * @author tempest15
		 * @since 3.1.0
		 */
		ephemeralSlashCommand {
			name = "lock"
			description = "The parent command for all locking commands"

			ephemeralSubCommand(::LockChannelArgs) {
				name = "channel"
				description = "Lock a channel so only mods can send messages"

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				@Suppress("DuplicatedCode")
				action {
					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)

					val channelArg = arguments.channel ?: event.interaction.getChannel()
					var channelParent: TextChannel? = null
					if (channelArg is TextChannelThread) {
						channelParent = channelArg.getParent()
					}
					val targetChannel = channelParent ?: channelArg.asChannelOf()

					val channelPerms = targetChannel.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms != null && channelPerms.denied.contains(Permission.SendMessages)) {
						respond { content = "This channel is already locked!" }
						return@action
					}

					targetChannel.createEmbed {
						title = "Channel Locked"
						description = "This channel has been locked by a moderator."
						color = DISCORD_RED
					}

					targetChannel.editRolePermission(guild!!.id) {
						denied += Permission.SendMessages
						denied += Permission.SendMessagesInThreads
						denied += Permission.AddReactions
						denied += Permission.UseApplicationCommands
					}

					actionLog?.createEmbed {
						title = "Channel Locked"
						description = "${targetChannel.mention} has been locked.\n\n**Reason:** ${arguments.reason}"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}

					respond { content = "${targetChannel.mention} has been locked." }
				}
			}

			ephemeralSubCommand(::LockServerArgs) {
				name = "server"
				description = "Lock the server so only mods can send messages"

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels)
				}

				action {
					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
					val everyoneRole = guild!!.getRole(guild!!.id)

					if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "The server is already locked!" }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.minus(Permission.SendMessages)
							.minus(Permission.SendMessagesInThreads)
							.minus(Permission.AddReactions)
							.minus(Permission.UseApplicationCommands)
					}

					actionLog?.createEmbed {
						title = "Server locked"
						description = "**Reason:** ${arguments.reason}"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}

					respond { content = "Server locked." }
				}
			}
		}

		/**
		 * Server and channel unlocking commands
		 *
		 * @author tempest15
		 * @since 3.1.0
		 */
		ephemeralSlashCommand {
			name = "unlock"
			description = "The parent command for all unlocking commands"

			ephemeralSubCommand(::UnlockChannelArgs) {
				name = "channel"
				description = "Unlock a channel so everyone can send messages"

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				@Suppress("DuplicatedCode")
				action {
					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)

					val channelArg = arguments.channel ?: event.interaction.getChannel()
					var channelParent: TextChannel? = null
					if (channelArg is TextChannelThread) {
						channelParent = channelArg.getParent()
					}
					val targetChannel = channelParent ?: channelArg.asChannelOf()

					val channelPerms = targetChannel.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms == null) {
						respond { content = "This channel is not locked!" }
						return@action
					}
					if (!channelPerms.denied.contains(Permission.SendMessages)) {
						respond { content = "This channel is not locked!" }
						return@action
					}

					targetChannel.editRolePermission(guild!!.id) {
						allowed += Permission.SendMessages
						allowed += Permission.SendMessagesInThreads
						allowed += Permission.AddReactions
						allowed += Permission.UseApplicationCommands
					}

					targetChannel.createEmbed {
						title = "Channel Unlocked"
						description = "This channel has been unlocked by a moderator.\n" +
								"Please be aware of the rules when continuing discussion."
						color = DISCORD_GREEN
					}

					actionLog?.createEmbed {
						title = "Channel Unlocked"
						description = "${targetChannel.mention} has been unlocked."
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}

					respond { content = "${targetChannel.mention} has been unlocked." }
				}
			}

			ephemeralSubCommand {
				name = "server"
				description = "Unlock the server so everyone can send messages"

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels)
				}

				action {
					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild?.getChannelOf<GuildMessageChannel>(config.channel)
					val everyoneRole = guild!!.getRole(guild!!.id)

					if (everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "The server isn't locked!" }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.plus(Permission.SendMessages)
							.plus(Permission.SendMessagesInThreads)
							.plus(Permission.AddReactions)
							.plus(Permission.UseApplicationCommands)
					}

					actionLog?.createEmbed {
						title = "Server unlocked"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}

					respond { content = "Server unlocked." }
				}
			}
		}
	}

	inner class ClearArgs : Arguments() {
		/** The number of messages the user wants to remove. */
		val messages by int {
			name = "messages"
			description = "Number of messages to delete"
		}
	}

	inner class TimeoutArgs : Arguments() {
		/** The requested user to timeout. */
		val userArgument by user {
			name = "user"
			description = "Person to timeout"
		}

		/** The time the timeout should last for. */
		val duration by coalescingDefaultingDuration {
			name = "duration"
			description = "Duration of timeout"
			defaultValue = DateTimePeriod(0, 0, 0, 6, 0, 0, 0)
		}

		/** The reason for the timeout. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for timeout"
			defaultValue = "No reason provided"
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalString {
			name = "image"
			description = "The URL to an image you'd like to provide as extra context for the action"
		}
	}

	inner class RemoveTimeoutArgs : Arguments() {
		/** The requested user to remove the timeout from. */
		val userArgument by user {
			name = "user"
			description = "Person to remove timeout from"
		}
	}

	inner class WarnArgs : Arguments() {
		/** The requested user to warn. */
		val userArgument by user {
			name = "user"
			description = "Person to warn"
		}

		/** The reason for the warning. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for warn"
			defaultValue = "No reason provided"
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalString {
			name = "image"
			description = "The URL to an image you'd like to provide as extra context for the action"
		}
	}

	inner class RemoveWarnArgs : Arguments() {
		/** The requested user to remove the warning from. */
		val userArgument by user {
			name = "user"
			description = "Person to remove warn from"
		}
	}

	inner class LockChannelArgs : Arguments() {
		/** The channel that the user wants to lock. */
		val channel by optionalChannel {
			name = "channel"
			description = "Channel to lock. Defaults to current channel"
		}

		/** The reason for the locking. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for locking the channel"
			defaultValue = "No reason provided"
		}
	}

	inner class LockServerArgs : Arguments() {
		/** The reason for the locking. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for locking the server"
			defaultValue = "No reason provided"
		}
	}

	inner class UnlockChannelArgs : Arguments() {
		/** The channel to unlock. */
		val channel by optionalChannel {
			name = "channel"
			description = "Channel to unlock. Defaults to current channel"
		}
	}
}
