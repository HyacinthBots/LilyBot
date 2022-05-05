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

				DatabaseHelper.setLeaveTime(event.guildId, Clock.System.now())
			}
		}

		event<MemberJoinEvent> {
			action {
				if (event.member.id != kord.selfId) return@action // We only care if Lily is joining, so ignore others

				DatabaseHelper.setLeaveTime(event.guildId, null)
			}
		}
	}
}
