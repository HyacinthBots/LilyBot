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
import net.irisshaders.lilybot.utils.ResponseHelper
import net.irisshaders.lilybot.utils.TEST_GUILD_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import kotlin.time.ExperimentalTime

@Suppress("PrivatePropertyName")
class Ping : Extension() {
	override val name = "ping"

	override suspend fun setup() {
		val actionLog = kord.getGuild(TEST_GUILD_ID)?.getChannel(TEST_GUILD_CHANNEL) as GuildMessageChannelBehavior

		/**
		 * Online notification
		 * @author IMS212
		 */
		ResponseHelper.responseEmbedInChannel(actionLog, "Lily is now online!", null, DISCORD_GREEN, null)

		/**
		 * Ping Command
		 * @author IMS212
		 * @author NoComment1105
		 */
		publicSlashCommand {  // Public slash commands have public responses
			name = "ping"
			description = "Am I alive?"

			action {
				val averagePing = this@Ping.kord.gateway.averagePing

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
