@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.DatabaseManager
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.ExperimentalTime

/**
 * The join and leave logging for Members in the guild. More accurate join and leave times for users
 * @author NoComment1105
 */
class JoinLeaveEvent : Extension() {
	override val name = "joinleaveevent"

	override suspend fun setup() {
		event<MemberJoinEvent> {
			@Suppress("DuplicatedCode")
			action {
				var joinChannelId: String? = null
				var error = false

				newSuspendedTransaction {
					try {
						joinChannelId = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq event.guild.id.toString()
						}.single()[DatabaseManager.Config.joinChannel]
					} catch (e: NoSuchElementException) {
						error = true
					}
				}

				val eventMember = event.member
				val guildMemberCount = event.getGuild().members.count()

				if (!error) {
					val joinChannel = event.getGuild().getChannel(Snowflake(joinChannelId!!)) as GuildMessageChannelBehavior

					joinChannel.createEmbed {
						color = DISCORD_GREEN
						title = "User joined the server!"
						timestamp = Clock.System.now()

						field {
							name = "Welcome:"
							value = eventMember.tag
							inline = true
						}
						field {
							name = "ID:"
							value = eventMember.id.toString()
							inline = false
						}
						footer {
							text = "Member Count: $guildMemberCount"
						}
					}
				}
			}
		}
		event<MemberLeaveEvent> {
			@Suppress("DuplicatedCode")
			action {
				var joinChannelId: String? = null
				var error = false

				newSuspendedTransaction {
					try {
						joinChannelId = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq event.guild.id.toString()
						}.single()[DatabaseManager.Config.joinChannel]

					} catch (e: NoSuchElementException) {
						error = true
					}
				}

				val eventUser = event.user
				val guildMemberCount = event.getGuild().members.count()

				if (!error) {
					val joinChannel = event.getGuild().getChannel(Snowflake(joinChannelId!!)) as GuildMessageChannelBehavior

					joinChannel.createEmbed {
						color = DISCORD_RED
						title = "User left the server!"
						timestamp = Clock.System.now()

						field {
							name = "Goodbye:"
							value = eventUser.tag
							inline = true
						}
						field {
							name = "ID:"
							value = eventUser.id.toString()
							inline = false
						}
						footer {
							text = "Member count: $guildMemberCount"
						}
					}
				}
			}
		}
	}
}
