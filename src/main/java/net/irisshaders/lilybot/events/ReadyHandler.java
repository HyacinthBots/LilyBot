package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class ReadyHandler extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {

        JDA jda = event.getJDA();
        String tag = jda.getSelfUser().getAsTag();
        String actionLog = Constants.ACTION_LOG;

        LoggerFactory.getLogger(ReadyHandler.class).info(String.format("Logged in as \"%s\"", tag));

        MessageEmbed onlineEmbed = new EmbedBuilder()
                .setTitle("LilyBot is now online!")
                .setColor(Color.GREEN) // change these pls im too lazy
                .setTimestamp(Instant.now())
                .build();

        jda.getTextChannelById(actionLog).sendMessageEmbeds(onlineEmbed).queue();

    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {

        Guild guild = event.getGuild();
        List<Member> members = guild.getMembers();
        String insertString = "INSERT OR IGNORE INTO [warn](id, points) VALUES (?, ?)";

        try {
            PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(insertString);
            LoggerFactory.getLogger("SQLite").info("Writing all guild members to database!");
            for (Member member : members) {
                String memberId = member.getId();
                statement.setString(1, memberId);
                statement.setInt(2, 0);
                statement.execute();
                statement.closeOnCompletion();
            }
            statement.closeOnCompletion();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
