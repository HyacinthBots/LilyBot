@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.JOIN_CHANNEL
import kotlin.time.ExperimentalTime


/**
 * The join and leave logging for Members in the guild. More accurate join and leave times for users
 * @author NoComment1105
 */
class JoinLeaveEvent : Extension() {
    override val name = "joinleaveevent"

    override suspend fun setup() {
        event<MemberJoinEvent> {
            action {
                val memberMention = event.member.mention
                val memberId = event.member.id.asString
                val memberTag = event.member.tag
                val guildMemberCount = event.guild.fetchGuild().memberCount
                val joinChannel = event.guild.getChannel(JOIN_CHANNEL) as GuildMessageChannelBehavior

                joinChannel.createEmbed {
                    color = DISCORD_GREEN
                    title = "User joined the server!"
                    timestamp = Clock.System.now()

                    field {
                        name = "Welcome:"
                        value = "$memberMention! ($memberTag)"
                        inline = true
                    }
                    field {
                        name = "ID:"
                        value = memberId
                        inline = false
                    }
                    footer {
                        text = "Member Count: $guildMemberCount"
                    }
                }
            }
        }
        event<MemberLeaveEvent> {
            action {
                val userMention = event.user.mention
                val userId = event.user.id.asString
                val userTag = event.user.tag
                val joinTime = event.guild.fetchGuild().joinedTime!!.toMessageFormat(DiscordTimestampStyle.LongDateTime)
                var guildMemberCount = event.guild.fetchGuild().memberCount
                guildMemberCount = guildMemberCount?.minus(1)
                val joinChannel = event.guild.getChannel(JOIN_CHANNEL) as GuildMessageChannelBehavior

                joinChannel.createEmbed {
                    color = DISCORD_RED
                    title = "User left the server!"
                    timestamp = Clock.System.now()

                    field {
                        name = "Goodbye:"
                        value = "$userMention! ($userTag)"
                        inline = true
                    }
                    field {
                        name = "ID:"
                        value = userId
                        inline = false
                    }
                    field {
                        name = "**Joined on:**"
                        value = joinTime
                        inline = true
                    }
                    footer {
                        text = "Member count: $guildMemberCount"
                    }
                }
            }
        }
    }
}