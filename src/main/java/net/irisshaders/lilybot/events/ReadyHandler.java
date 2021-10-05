package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.CommandType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.irisshaders.lilybot.utils.Constants;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;

/**
 * What Lily does on startup.
 */
@SuppressWarnings("ConstantConditions")
public class ReadyHandler extends ListenerAdapter {

    /**
     * What Lily does when she starts.
     * @param event Lily's ReadyEvent.
     */
    @Override
    public void onReady(ReadyEvent event) {

        JDA jda = event.getJDA();
        String tag = jda.getSelfUser().getAsTag();
        TextChannel actionLog = jda.getTextChannelById(Constants.ACTION_LOG);

        jda.getGuildById(Constants.GUILD_ID).upsertCommand(new CommandData(CommandType.MESSAGE_CONTEXT, "Report message")).queue();

        LoggerFactory.getLogger(ReadyHandler.class).info(String.format("Logged in as %s", tag));

        MessageEmbed onlineEmbed = new EmbedBuilder()
                .setTitle("Lily is now online!")
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .build();

        actionLog.sendMessageEmbeds(onlineEmbed).queue();

    }

}
