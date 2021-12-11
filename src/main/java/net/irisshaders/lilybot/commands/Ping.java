package net.irisshaders.lilybot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;

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

        EmbedBuilder pingEmbed = ResponseHelper.responseEmbed("Ping!", user, Color.ORANGE)
                .setDescription("`Pong!` Shows the bot's ping!");

        event.replyEmbeds(pingEmbed.build()).mentionRepliedUser(false).setEphemeral(false).queue(interactionHook -> event.getJDA().getRestPing().queue(restPing -> {

            Long gatewayPing = event.getJDA().getGatewayPing();

            MessageEmbed finalPingEmbed = pingEmbed
                    .setDescription(String.format(
                            """
                            `Pong!` Showing the bot's ping!
                            Gateway Ping: `%s ms`
                            Rest Ping: `%s ms`
                            """, gatewayPing, restPing))
                    .build();

            interactionHook.editOriginalEmbeds(finalPingEmbed).queue();

        }));

    }

}
