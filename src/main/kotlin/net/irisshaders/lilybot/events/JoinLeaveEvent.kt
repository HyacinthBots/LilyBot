@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.count
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
                val eventMember = event.member
                val guildMemberCount = event.getGuild().members.count()
                val joinChannel = event.getGuild().getChannel(JOIN_CHANNEL) as GuildMessageChannelBehavior

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
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "events.joinleaveevent.join"
                        message = "Member joined"
                        data["user"] = eventMember.tag
                    }
                }
            }
        }
        event<MemberLeaveEvent> {
            action {
                val eventUser = event.user
                val guildMemberCount = event.getGuild().members.count()
                val joinChannel = event.getGuild().getChannel(JOIN_CHANNEL) as GuildMessageChannelBehavior

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

                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "events.joinleaveevent.leave"
                        message = "Member Left"
                        data["user"] = eventUser.tag
                    }
                }
            }
        }
    }
}
