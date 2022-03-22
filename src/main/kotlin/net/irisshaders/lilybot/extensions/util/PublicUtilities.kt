@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ONLINE_STATUS_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import kotlin.time.ExperimentalTime

class PublicUtilities : Extension() {
	override val name = "public-utilities"

	override suspend fun setup() {
		val onlineLog = kord.getGuild(TEST_GUILD_ID)?.getChannel(ONLINE_STATUS_CHANNEL) as GuildMessageChannelBehavior

		/**
		 * Online notification
		 * @author IMS212
		 */
		responseEmbedInChannel(
			onlineLog, "Lily is now online!", null,
			DISCORD_GREEN, null
		)

		/**
		 * Ping Command
		 * @author IMS212
		 * @author NoComment1105
		 */
		publicSlashCommand {  // Public slash commands have public responses
			name = "ping"
			description = "Am I alive?"

			action {
				val averagePing = this@PublicUtilities.kord.gateway.averagePing

				respond {
					embed {
						color = DISCORD_YELLOW
						title = "Pong!"

						timestamp = Clock.System.now()

						field {
							name = "Your Ping with Lily is:"
							value = "**$averagePing**"
							inline = true
						}
					}
				}
			}
		}

	}
}
