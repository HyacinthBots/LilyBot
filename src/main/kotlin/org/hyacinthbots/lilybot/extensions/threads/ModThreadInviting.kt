package org.hyacinthbots.lilybot.extensions.threads

import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.hyacinthbots.lilybot.utils.canPingRole

class ModThreadInviting : Extension() {
	override val name: String = "mod-thread-inviting"

	override suspend fun setup() {
		event<ThreadChannelCreateEvent> {
			check {
				anyGuild()
				failIf {
					event.channel.ownerId == kord.selfId ||
						event.channel.member != null
				}
			}

			action {
				if (event.channel.getLastMessage()?.withStrategy(EntitySupplyStrategy.rest) != null) return@action

				val channel = event.channel
				val thread = channel.asChannelOfOrNull<TextChannelThread>() ?: return@action
				val autoThreadOptions = AutoThreadingCollection().getSingleAutoThread(thread.parentId)

				if (autoThreadOptions != null) {
					handleThreadCreation(
						autoThreadOptions, thread, channel.owner.asUser()
					)
				} else {
					val config = ModerationConfigCollection().getConfig(channel.guildId) ?: return@action

					if (!config.enabled || config.role == null || config.autoInviteModeratorRole != true) return@action

					val moderatorRole = channel.guild.getRoleOrNull(config.role) ?: return@action

					if (!canPingRole(moderatorRole, event.channel.guildId, kord)) return@action

					val message = channel.createMessage {
						content = "Placeholder message"
					}

					message.edit {
						content = moderatorRole.mention
					}

					message.delete()
				}
			}
		}
	}

	suspend inline fun handleThreadCreation(
		inputOptions: AutoThreadingData,
		inputThread: TextChannelThread,
		inputUser: User,
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
