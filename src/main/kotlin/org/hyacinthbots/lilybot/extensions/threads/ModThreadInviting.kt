package org.hyacinthbots.lilybot.extensions.threads

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
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

				AutoThreadingCollection().getAllAutoThreads(channel.guildId).forEach {
					if (it.channelId == channel.parentId) return@action
				}

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
