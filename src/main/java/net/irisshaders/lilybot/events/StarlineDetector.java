package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.time.Instant;


public class StarlineDetector extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        Message msg = event.getMessage();
        String[] msgSplit = msg.getContentRaw().split(" ");
        for(String i : msgSplit ){
            String lowerMsg = i.toLowerCase();
            if (lowerMsg.contains("starline"))  {
                MessageEmbed starlineEmbed = new EmbedBuilder()
                        .setTitle("Starline is not supported.")
                        .setDescription(
                                "Starline is an unofficial fork and not supported by us, please move to" +
                                        " #general if you need support with it."
                        )
                        .setColor(Color.RED)
                        .setTimestamp(Instant.now())
                        .build();

                msg.getChannel().sendMessageEmbeds(starlineEmbed).mentionRepliedUser(false).queue();
            }
        }
    }
}
