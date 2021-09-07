package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.irisshaders.lilybot.LilyBot;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;

@SuppressWarnings("ConstantConditions")
public class ReadyHandler extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {

        JDA jda = event.getJDA();
        String tag = jda.getSelfUser().getAsTag();
        String channelId = LilyBot.ACTION_LOG;

        LoggerFactory.getLogger(ReadyHandler.class).info(String.format("Logged in as \"%s\"", tag));

        MessageEmbed onlineEmbed = new EmbedBuilder()
                .setTitle("LilyBot is now online!")
                .setColor(Color.GREEN) // change these pls im too lazy
                .setTimestamp(Instant.now())
                .build();

        jda.getTextChannelById(channelId).sendMessageEmbeds(onlineEmbed).queue();

    }

}
