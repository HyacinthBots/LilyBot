package org.hyacinthbots.lilybot.extensions.moderation.commands

import dev.kord.common.DiscordBitSet
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.ThreadParentChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.defaultingString
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.types.EphemeralInteractionContext
import kotlinx.datetime.Clock
import lilybot.i18n.Translations
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
			name = Translations.Moderation.LockingCommands.Lock.name
			description = Translations.Moderation.LockingCommands.Lock.description

			ephemeralSubCommand(::LockChannelArgs) {
				name = Translations.Moderation.LockingCommands.Channel.name
				description = Translations.Moderation.LockingCommands.Lock.Channel.description

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
							content = Translations.Moderation.LockingCommands.Lock.Channel.permsError.translate()
						}
						return@action
					}

					if (LockedChannelCollection().getLockedChannel(guild!!.id, targetChannel.id) != null) {
						respond { content = Translations.Moderation.LockingCommands.Lock.Channel.already.translate() }
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
						respond { content = Translations.Moderation.LockingCommands.unableToEveryone.translate() }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond {
							content = Translations.Moderation.LockingCommands.Lock.Channel.serverLocked.translate()
						}
						return@action
					}

					targetChannel.createEmbed {
						title = Translations.Moderation.LockingCommands.Lock.Channel.publicEmbedTitle.translate()
						description = Translations.Moderation.LockingCommands.Lock.Channel.publicEmbedDesc.translate()
						color = DISCORD_RED
					}

					targetChannel.editRolePermission(guild!!.id) {
						denied += Permission.SendMessages
						denied += Permission.SendMessagesInThreads
						denied += Permission.AddReactions
						denied += Permission.UseApplicationCommands
					}

					// Explicitly allow Lily send messages, so she can unlock the server again
					targetChannel.editMemberPermission(this@ephemeralSlashCommand.kord.selfId) {
						allowed += Permission.SendMessages
					}

					respond {
						content = Translations.Moderation.LockingCommands.Lock.Channel.lockConfirmation.translate(
							targetChannel.mention
						)
					}

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = Translations.Moderation.LockingCommands.Lock.Channel.publicEmbedTitle.translate()
						description = Translations.Moderation.LockingCommands.Lock.Channel.embedDesc.translateNamed(
							"channel" to targetChannel.mention,
							"reason" to arguments.reason
						)
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}
				}
			}

			ephemeralSubCommand(::LockServerArgs) {
				name = Translations.Moderation.LockingCommands.Server.name
				description = Translations.Moderation.LockingCommands.Lock.Server.description

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
						respond { content = Translations.Moderation.LockingCommands.unableToEveryone.translate() }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = Translations.Moderation.LockingCommands.Lock.Server.already.translate() }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.minus(Permission.SendMessages)
							.minus(Permission.SendMessagesInThreads)
							.minus(Permission.AddReactions)
							.minus(Permission.UseApplicationCommands)
					}

					respond {
						content = Translations.Moderation.LockingCommands.Lock.Server.lockConfirmation.translate()
					}

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = Translations.Moderation.LockingCommands.Lock.Server.lockConfirmation.translate()
						description = Translations.Moderation.LockingCommands.Lock.Server.embedDesc.translate()
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
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
			name = Translations.Moderation.LockingCommands.Unlock.name
			description = Translations.Moderation.LockingCommands.Unlock.description

			ephemeralSubCommand(::UnlockChannelArgs) {
				name = Translations.Moderation.LockingCommands.Channel.name
				description = Translations.Moderation.LockingCommands.Unlock.Channel.description

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
						respond { content = Translations.Moderation.LockingCommands.unableToEveryone.translate() }
						return@action
					} else if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond {
							content =
								Translations.Moderation.LockingCommands.Unlock.Channel.unlockServerToUnlock.translate()
						}
						return@action
					}

					val channelPerms = targetChannel?.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms == null) {
						respond {
							content = Translations.Moderation.LockingCommands.Unlock.Channel.notLocked.translate()
						}
						return@action
					}
					val lockedChannel = LockedChannelCollection().getLockedChannel(guild!!.id, targetChannel.id)
					if (lockedChannel == null) {
						respond {
							content = Translations.Moderation.LockingCommands.Unlock.Channel.notLocked.translate()
						}
						return@action
					}

					targetChannel.editRolePermission(guild!!.id) {
						denied = Permissions.Builder(DiscordBitSet(lockedChannel.denied)).build()
						allowed = Permissions.Builder(DiscordBitSet(lockedChannel.allowed)).build()
					}

					// Remove the explicit allow to avoid any issues with the servers permission system
					targetChannel.editMemberPermission(this@ephemeralSlashCommand.kord.selfId) {
						allowed -= Permission.SendMessages
					}

					targetChannel.createEmbed {
						title = Translations.Moderation.LockingCommands.Unlock.Channel.publicEmbedTitle.translate()
						description = Translations.Moderation.LockingCommands.Unlock.Channel.publicEmbedDesc.translate()
						color = DISCORD_GREEN
					}

					LockedChannelCollection().removeLockedChannel(guild!!.id, targetChannel.id)

					respond {
						content = Translations.Moderation.LockingCommands.Unlock.Channel.unlockConfirmation.translate(
							targetChannel.mention
						)
					}

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = Translations.Moderation.LockingCommands.Unlock.Channel.publicEmbedTitle.translate()
						description = Translations.Moderation.LockingCommands.Unlock.Channel.unlockConfirmation.translate(
							targetChannel.mention
						)
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
				}
			}

			ephemeralSubCommand {
				name = Translations.Moderation.LockingCommands.Server.name
				description = Translations.Moderation.LockingCommands.Unlock.Server.description

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
						respond { content = Translations.Moderation.LockingCommands.unableToEveryone.translate() }
						return@action
					} else if (everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = Translations.Moderation.LockingCommands.Unlock.Server.notLocked.translate() }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.plus(Permission.SendMessages)
							.plus(Permission.SendMessagesInThreads)
							.plus(Permission.AddReactions)
							.plus(Permission.UseApplicationCommands)
					}

					respond { content = Translations.Moderation.LockingCommands.Unlock.Server.unlockConfirmation.translate() }

					val actionLog =
						getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
					actionLog.createEmbed {
						title = Translations.Moderation.LockingCommands.Unlock.Server.unlockConfirmation.translate()
						footer {
							text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
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
				content = Translations.Moderation.LockingCommands.unableToChannel.translate()
			}
			return null
		}

		return targetChannel
	}

	inner class LockChannelArgs : Arguments() {
		/** The channel that the user wants to lock. */
		val channel by optionalChannel {
			name = Translations.Moderation.LockingCommands.Channel.name
			description = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Channel.description
		}

		/** The reason for the locking. */
		val reason by defaultingString {
			name = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Reason.name
			description = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Reason.description
			defaultValue = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Reason.default.translate()
		}
	}

	inner class LockServerArgs : Arguments() {
		/** The reason for the locking. */
		val reason by defaultingString {
			name = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Reason.name
			description = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Reason.description
			defaultValue = Translations.Moderation.LockingCommands.Lock.Channel.Arguments.Reason.default.translate()
		}
	}

	inner class UnlockChannelArgs : Arguments() {
		/** The channel to unlock. */
		val channel by optionalChannel {
			name = Translations.Moderation.LockingCommands.Channel.name
			description = Translations.Moderation.LockingCommands.Unlock.Channel.Arguments.Channel.description
		}
	}
}
