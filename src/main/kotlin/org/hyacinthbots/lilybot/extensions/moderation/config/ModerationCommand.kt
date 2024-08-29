package org.hyacinthbots.lilybot.extensions.moderation.config

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.utils.botHasPermissions
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.utils.moderationEmbed
import org.hyacinthbots.lilybot.utils.canPingRole
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

suspend fun SlashCommand<*, *, *>.moderationCommand() =
		ephemeralSubCommand(::ModerationArgs) {
			name = "moderation"
			description = "Configure Lily's moderation system"

			requirePermission(Permission.ManageGuild)

			check {
				anyGuild()
				hasPermission(Permission.ManageGuild)
			}

			action {
				val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)
				if (moderationConfig != null) {
					respond {
						content = "You already have a moderation configuration set. " +
							"Please clear it before attempting to set a new one."
					}
					return@action
				}

				if (!arguments.enabled) {
					ModerationConfigCollection().setConfig(
						ModerationConfigData(
							guild!!.id,
							false,
							null,
							null,
							null,
							null,
							null,
							null,
							null
						)
					)
					respond {
						content = "Moderation system disabled."
					}
					return@action
				}

				if (
					arguments.moderatorRole != null && arguments.modActionLog == null ||
					arguments.moderatorRole == null && arguments.modActionLog != null
				) {
					respond {
						content =
							"You must set both the moderator role and the action log channel to use the moderation configuration."
					}
					return@action
				}

				if (!canPingRole(arguments.moderatorRole, guild!!.id, this@moderationCommand.kord)) {
					respond {
						content =
							"I cannot use the role: ${arguments.moderatorRole!!.mention}, because it is not mentionable by " +
								"regular users. Please enable this in the role settings, or use a different role."
					}
					return@action
				}

				val modActionLog: TextChannel?
				if (arguments.enabled && arguments.modActionLog != null) {
					modActionLog = guild!!.getChannelOfOrNull(arguments.modActionLog!!.id)
					if (modActionLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
						respond {
							content = "The mod action log you've selected is invalid, or I can't view it. " +
								"Please attempt to resolve this and try again."
						}
						return@action
					}
				}

				respond {
					embed {
						moderationEmbed(arguments, user)
					}
				}

				ModerationConfigCollection().setConfig(
					ModerationConfigData(
						guild!!.id,
						arguments.enabled,
						arguments.modActionLog?.id,
						arguments.moderatorRole?.id,
						arguments.quickTimeoutLength,
						arguments.warnAutoPunishments,
						arguments.logPublicly,
						arguments.banDmMessage,
						arguments.autoInviteModeratorRole
					)
				)

				val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

				if (utilityLog == null) {
					respond {
						content = "Consider setting a utility config to log changes to configurations."
					}
					return@action
				}

				utilityLog.createMessage {
					embed {
						moderationEmbed(arguments, user)
					}
				}
			}
		}
