package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.ONLINE_STATUS_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.responseEmbedInChannel

/**
 * This class serves as a place for all functions that get run on bot start and bot start alone. This *hypothetically*
 * fixes a peculiar bug with [PublicUtilities], where if these functions we're present within, all other feature from
 * the class don't get added to a server when the bot joins the server, and instead only present themselves after a
 * bot instance restart.
 *
 * @since 3.3.0
 */
class StartupHooks : Extension() {
	override val name = "startuphooks"

	override suspend fun setup() {
		/**
		 * Online notification, that is printed to the official [TEST_GUILD_ID]'s [ONLINE_STATUS_CHANNEL].
		 * @author IMS212
		 * @since v2.0
		 */
		// The channel specifically for sending online notifications to
		val onlineLog = kord.getGuild(TEST_GUILD_ID)?.getChannel(ONLINE_STATUS_CHANNEL) as GuildMessageChannelBehavior
		responseEmbedInChannel(
			onlineLog, "Lily is now online!", null, DISCORD_GREEN, null
		)

		/**
		 * This function is called to remove any threads in the database that haven't had a message sent in the last
		 * week. It only runs on startup.
		 * @author tempest15
		 * @since 3.2.0
		 */
		DatabaseHelper.cleanupThreadData(kord)

		/**
		 * This function is called to remove any guilds in the database that haven't had Lily in them for more than
		 * a month. It only runs on startup
		 *
		 * @author NoComment1105
		 * @since 3.2.0
		 */
		DatabaseHelper.cleanupGuildData()
	}
}
