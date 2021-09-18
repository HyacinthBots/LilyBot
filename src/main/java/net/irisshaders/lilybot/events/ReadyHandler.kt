package net.irisshaders.lilybot.events

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.irisshaders.lilybot.database.SQLiteDataSource
import net.irisshaders.lilybot.utils.Constants
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.awt.Color
import java.sql.SQLException
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

    /**
     * What Lily does when she loads Iriscord on startup.
     * @param event Iriscord's GuildReadyEvent.
     */
    override fun onGuildReady(event: GuildReadyEvent) {
        val guild = event.guild
        val members = guild.members
        @Language("SQL") val insertString = "INSERT OR IGNORE INTO [warn](id, points) VALUES (?, ?)"
        try {
            val statement = SQLiteDataSource.getConnection().prepareStatement(insertString)
            LoggerFactory.getLogger("SQLite").info("Writing all guild members to database!")
            for (member in members) {
                val memberId = member.id
                statement.setString(1, memberId)
                statement.setInt(2, 0)
                statement.execute()
                statement.closeOnCompletion()
            }
            statement.closeOnCompletion()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}