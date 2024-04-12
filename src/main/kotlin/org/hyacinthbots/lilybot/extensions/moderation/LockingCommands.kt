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
import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import dev.kord.common.DiscordBitSet
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.ThreadParentChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LockedChannelCollection
import org.hyacinthbots.lilybot.database.entities.LockedChannelData
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
					requiredConfigs(ConfigOptions.MODERATION_ENABLED)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels, Permission.ManageRoles)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				action {
					val channelArg = arguments.channel ?: event.interaction.getChannelOrNull()
					val channelParent = getChannelParent(channelArg)
					val targetChannel = getTargetChannel(channelParent, channelArg)

					val currentChannelPerms = targetChannel?.getPermissionOverwritesForRole(guild!!.id)
					if (currentChannelPerms == null) {
						respond {
							content = "There was an error getting the permissions for this channel. Please try again."
						}
						return@action
					}

					if (LockedChannelCollection().getLockedChannel(guild!!.id, targetChannel.id) != null) {
						respond { content = "This channel is already locked" }
						return@action
					}

					LockedChannelCollection().addLockedChannel(
						LockedChannelData(
							guildId = guild!!.id,
							channelId = targetChannel.id,
							allowed = currentChannelPerms.data.allowed.code.value,
							denied = currentChannelPerms.data.denied.code.value
						)
					)

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
							text = user.asUserOrNull()?.username ?: "Unable to get username"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
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
					requiredConfigs(ConfigOptions.MODERATION_ENABLED)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels, Permission.ManageRoles, Permission.SendMessages)
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
							text = user.asUserOrNull()?.username ?: "Unable to get user username"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
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
					requireBotPermissions(Permission.ManageChannels, Permission.ManageRoles)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				action {
					val channelArg = arguments.channel ?: event.interaction.getChannelOrNull()
					val channelParent = getChannelParent(channelArg)
					val targetChannel = getTargetChannel(channelParent, channelArg)

					val everyoneRole = guild!!.getRoleOrNull(guild!!.id)
					if (everyoneRole == null) {
						respond { content = "Unable to get `@everyone` role. Please try again" }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "Please unlock the server to unlock this channel." }
						return@action
					}

					val channelPerms = targetChannel?.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms == null) {
						respond { content = "This channel is not locked!" }
						return@action
					}
					val lockedChannel = LockedChannelCollection().getLockedChannel(guild!!.id, targetChannel.id)
					if (lockedChannel == null) {
						respond { content = "This channel is not locked!" }
						return@action
					}

					targetChannel.editRolePermission(guild!!.id) {
						denied = Permissions.Builder(DiscordBitSet(lockedChannel.denied)).build()
						allowed = Permissions.Builder(DiscordBitSet(lockedChannel.allowed)).build()
					}

					targetChannel.createEmbed {
						title = "Channel Unlocked"
						description = "This channel has been unlocked by a moderator.\n" +
							"Please be aware of the rules when continuing discussion."
						color = DISCORD_GREEN
					}

					LockedChannelCollection().removeLockedChannel(guild!!.id, targetChannel.id)

					respond { content = "${targetChannel.mention} has been unlocked." }

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = "Channel Unlocked"
						description = "${targetChannel.mention} has been unlocked."
						footer {
							text = user.asUserOrNull()?.username ?: "Unable to get user username"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
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
					requiredConfigs(ConfigOptions.MODERATION_ENABLED)
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ManageChannels, Permission.ManageRoles, Permission.SendMessages)
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
							text = user.asUserOrNull()?.username ?: "Unable to get user username"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
				}
			}
		}
	}

	/**
	 * Gets the parent of the channel argument.
	 *
	 * @param channelArg The channel to get the parent of
	 * @return The channel parent as a [TextChannel]
	 *
	 * @author NoComment1105
	 * @since 4.8.0
	 */
	private suspend inline fun getChannelParent(channelArg: Channel?): TextChannel? {
		var channelParent: ThreadParentChannel? = null
		if (channelArg is TextChannelThread) {
			channelParent = channelArg.getParent()
		}

		return channelParent?.asChannelOfOrNull()
	}

	/**
	 * Gets the target channel and responds appropriately if unable to get it.
	 *
	 * @param channelParent The parent channel if that is the target
	 * @param channelArg The channel argument, if that is the target
	 * @return The channel as a [TextChannel]
	 *
	 * @author NoComment1105
	 * @since 4.8.0
	 */
	private suspend inline fun EphemeralInteractionContext.getTargetChannel(
		channelParent: TextChannel?,
		channelArg: Channel?
	): TextChannel? {
		val targetChannel = channelParent ?: channelArg?.asChannelOfOrNull()
		if (targetChannel == null) {
			respond {
				content = "I can't fetch the targeted channel properly."
			}
			return null
		}

		return targetChannel
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
