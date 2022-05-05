package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper

class JoinLeaveDetection : Extension() {
	override val name = "join-leave-detection"

	override suspend fun setup() {
		event<GuildDeleteEvent> {
			action {
				DatabaseHelper.setLeaveTime(event.guildId, Clock.System.now())
			}
		}

		event<GuildCreateEvent> {
			action {
				DatabaseHelper.deleteLeaveTime(event.guild.id)
			}
		}
	}
}
