package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class ETA extends SlashCommand {

    public ETA() {
        this.name = "eta";
        this.help = "We dont know";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE_WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed etaEmbed = new EmbedBuilder()
                .setTitle("What's the ETA!?")
                .setDescription(
                        "We don't know."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(etaEmbed).mentionRepliedUser(false).setEphemeral(false).queue();
    }
}
