package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper

class JoinLeaveDetection : Extension() {
	override val name = "joinleavedetection"

	override suspend fun setup() {
		event<MemberLeaveEvent> {
			action {
				if (event.user.id != kord.selfId) return@action // We only care if Lily is leaving, so ignore others

				val guildLeaveData = DatabaseHelper.getLeaveTime(event.guildId) ?: return@action

				// This should never happen unless Lily is re-invited to a guild
				if (guildLeaveData.guildLeaveTime != null) return@action

				DatabaseHelper.setLeaveTime(event.guildId, Clock.System.now())
			}
		}

		event<MemberJoinEvent> {
			action {
				if (event.member.id != kord.selfId) return@action // We only care if Lily is joining, so ignore others

				val guildLeaveData = DatabaseHelper.getLeaveTime(event.guildId) ?: return@action

				// If this is the case, it is first time join. Return to avoid removing things that don't exist
				if (guildLeaveData.guildLeaveTime == null) return@action

				DatabaseHelper.setLeaveTime(event.guildId, null)
			}
		}
	}
}
