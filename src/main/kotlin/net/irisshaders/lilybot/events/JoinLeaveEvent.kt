@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.cache.api.data.description
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import net.irisshaders.lilybot.utils.ACTION_LOG
import net.irisshaders.lilybot.utils.JOIN_CHANNEL
import kotlin.time.ExperimentalTime

class JoinLeaveEvent : Extension() {
    override val name = "joinleaveevent"

    override suspend fun setup() {
        event<MemberJoinEvent> {
            action {
                val member = event.member
                val memberId = event.member.id.asString
                val memberTag = event.member.tag
                val guildMemberCount = event.guild.fetchGuild().memberCount
                val joinChannel = event.guild.getChannel(JOIN_CHANNEL) as GuildMessageChannelBehavior

                joinChannel.createEmbed {
                    color = DISCORD_GREEN
                    title = "User joined the server!"
                    timestamp = Clock.System.now()

                    field {
                        value = "Everyone welcome $member"
                        value = memberTag
                        inline = false
                    }
                    field {
                        name = "**ID:**"
                        value = memberId
                        inline = false
                    }
                    footer {
                        this.text = "Member Count: $guildMemberCount"
                    }
                }
            }
        }
        event<MemberLeaveEvent> {
            action {
                val user = event.user.mention
                val userId = event.user.id.asString
                val userTag = event.user.tag
                val joinTime = event.guild.fetchGuild().joinedTime!!.toMessageFormat(DiscordTimestampStyle.LongTime)
                var guildMemberCount = event.guild.fetchGuild().memberCount
                guildMemberCount = guildMemberCount?.minus(1)
                val joinChannel = event.guild.getChannel(JOIN_CHANNEL) as GuildMessageChannelBehavior

                joinChannel.createEmbed {
                    color = DISCORD_RED
                    title = "User left the server!"
                    timestamp = Clock.System.now()

                    field {
                        value = "Goodbye $user!"
                        value = userTag
                        inline = false
                    }
                    field {
                        name = "**ID:**"
                        value = userId
                        inline = false
                    }
                    field {
                        name = "**Joined on:**"
                        value = joinTime
                        inline = true
                    }
                    footer {
                        this.text = "Member count: $guildMemberCount"
                    }
                }
            }
        }
    }
}