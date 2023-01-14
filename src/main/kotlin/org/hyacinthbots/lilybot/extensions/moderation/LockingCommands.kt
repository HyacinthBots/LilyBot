package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs

class LockingCommands : Extension() {
	override val name = "locking-commands"

	override suspend fun setup() {
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
				description = "Lock a channel so those with default permissions cannot send messages"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					requiredConfigs(
						ConfigOptions.MODERATION_ENABLED
					)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(
						Permission.ManageChannels,
						Permission.ManageRoles,
						Permission.SendMessages
					)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				action {
					val channelArg = arguments.channel ?: event.interaction.getChannelOrNull()
					var channelParent: TextChannel? = null
					if (channelArg is TextChannelThread) {
						channelParent = channelArg.getParent()
					}
					val targetChannel = channelParent ?: channelArg?.asChannelOfOrNull()
					if (targetChannel == null) {
						respond {
							content = "I can't fetch the targeted channel properly."
							return@action
						}
					}

					val channelPerms = targetChannel!!.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms != null && channelPerms.denied.contains(Permission.SendMessages)) {
						respond { content = "This channel is already locked!" }
						return@action
					}

					val everyoneRole = guild!!.getRoleOrNull(guild!!.id)
					if (everyoneRole == null) {
						respond { content = "I was unable to get the `@everyone` role. Please try again." }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "The server is locked, so I cannot lock this channel." }
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

					respond { content = "${targetChannel.mention} has been locked." }

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = "Channel Locked"
						description = "${targetChannel.mention} has been locked.\n\n**Reason:** ${arguments.reason}"
						footer {
							text = user.asUserOrNull()?.tag ?: "Unable to get tag"
							icon = user.asUserOrNull()?.avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}
				}
			}

			ephemeralSubCommand(::LockServerArgs) {
				name = "server"
				description = "Lock the server so those with default permissions cannot send messages"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					requiredConfigs(
						ConfigOptions.MODERATION_ENABLED
					)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(
						Permission.ManageChannels,
						Permission.ManageRoles,
						Permission.SendMessages
					)
				}

				action {
					val everyoneRole = guild!!.getRoleOrNull(guild!!.id)

					if (everyoneRole == null) {
						respond { content = "I was unable to get the `@everyone` role. Please try again." }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "The server is already locked." }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.minus(Permission.SendMessages)
							.minus(Permission.SendMessagesInThreads)
							.minus(Permission.AddReactions)
							.minus(Permission.UseApplicationCommands)
					}

					respond { content = "Server locked." }

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = "Server locked"
						description = "**Reason:** ${arguments.reason}"
						footer {
							text = user.asUserOrNull()?.tag ?: "Unable to get user tag"
							icon = user.asUserOrNull()?.avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}
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
				description = "Unlock a channel so everyone can send messages again"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					requiredConfigs(ConfigOptions.MODERATION_ENABLED)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(
						Permission.ManageChannels,
						Permission.ManageRoles,
						Permission.SendMessages
					)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				action {
					val channelArg = arguments.channel ?: event.interaction.getChannelOrNull()
					var channelParent: TextChannel? = null
					if (channelArg is TextChannelThread) {
						channelParent = channelArg.getParent()
					}
					val targetChannel = channelParent ?: channelArg?.asChannelOfOrNull()
					if (targetChannel == null) {
						respond {
							content = "I can't fetch the targeted channel properly."
							return@action
						}
					}

					val everyoneRole = guild!!.getRoleOrNull(guild!!.id)
					if (everyoneRole == null) {
						respond { content = "Unable to get `@everyone` role. Please try again" }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "Please unlock the server to unlock this channel." }
						return@action
					}

					val channelPerms = targetChannel!!.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms == null) {
						respond { content = "This channel is not locked!" }
						return@action
					}
					if (!channelPerms.denied.contains(Permission.SendMessages)) {
						respond { content = "This channel is not locked!" }
						return@action
					}

					targetChannel.editRolePermission(guild!!.id) {
						denied -= Permission.SendMessages
						denied -= Permission.SendMessagesInThreads
						denied -= Permission.AddReactions
						denied -= Permission.UseApplicationCommands
					}

					targetChannel.createEmbed {
						title = "Channel Unlocked"
						description = "This channel has been unlocked by a moderator.\n" +
								"Please be aware of the rules when continuing discussion."
						color = DISCORD_GREEN
					}

					respond { content = "${targetChannel.mention} has been unlocked." }

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = "Channel Unlocked"
						description = "${targetChannel.mention} has been unlocked."
						footer {
							text = user.asUserOrNull()?.tag ?: "Unable to get user tag"
							icon = user.asUserOrNull()?.avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
				}
			}

			ephemeralSubCommand {
				name = "server"
				description = "Unlock the server so everyone can send messages again"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					requiredConfigs(
						ConfigOptions.MODERATION_ENABLED
					)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(
						Permission.ManageChannels,
						Permission.ManageRoles,
						Permission.SendMessages
					)
				}

				action {
					val everyoneRole = guild!!.getRoleOrNull(guild!!.id)
					if (everyoneRole == null) {
						respond { content = "Unable to get `@everyone` role. Please try again" }
						return@action
					} else if (everyoneRole.permissions.contains(Permission.SendMessages)) {
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

					respond { content = "Server unlocked." }

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = "Server unlocked"
						footer {
							text = user.asUserOrNull()?.tag ?: "Unable to get user tag"
							icon = user.asUserOrNull()?.avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
				}
			}
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
