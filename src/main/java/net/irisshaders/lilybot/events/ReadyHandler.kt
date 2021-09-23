package net.irisshaders.lilybot.events

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.irisshaders.lilybot.utils.Constants
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.Instant

/**
 * What Lily does on startup.
 */
class ReadyHandler : ListenerAdapter() {

    /**
     * What Lily does when she starts.
     * @param event Lily's ReadyEvent.
     */
    override fun onReady(event: ReadyEvent) {
        val jda = event.jda
        val actionLog = Constants.ACTION_LOG
        LoggerFactory.getLogger(ReadyHandler::class.java).info("Logged in as ${jda.selfUser.asTag}")
        val onlineEmbed = EmbedBuilder()
            .setTitle("LilyBot is now online!")
            .setColor(Color.GREEN)
            .setTimestamp(Instant.now())
            .build()
        jda.getTextChannelById(actionLog)!!.sendMessageEmbeds(onlineEmbed).queue()
    }

}