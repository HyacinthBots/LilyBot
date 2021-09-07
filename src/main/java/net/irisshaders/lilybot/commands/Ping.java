package net.irisshaders.lilybot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class Ping extends SlashCommand {

    public Ping() {
        this.name = "ping";
        this.help = "Ping the bot to see if it is alive.";
        this.defaultEnabled = true;
        this.guildOnly = false;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed pingEmbed = new EmbedBuilder()
                .setTitle("Ping!")
                .setDescription("`Pong!` Shows the bot's ping!")
                .setColor(Color.ORANGE)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(pingEmbed).mentionRepliedUser(false).setEphemeral(false).queue(interactionHook -> event.getJDA().getRestPing().queue(restPing -> {

            Long gatewayPing = event.getJDA().getGatewayPing();

            MessageEmbed finalPingEmbed = new EmbedBuilder()
                    .setTitle("Ping!")
                    .setDescription(String.format(
                            """
                            `Pong!` Showing the bot's ping!
                            Gateway Ping: `%s ms`
                            Rest Ping: `%s ms`
                            """, gatewayPing, restPing))
                    .setColor(Color.ORANGE)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            interactionHook.editOriginalEmbeds(finalPingEmbed).queue();

        }));

    }

}
