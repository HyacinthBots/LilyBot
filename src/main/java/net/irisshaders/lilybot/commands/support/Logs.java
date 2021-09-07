package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;

public class Logs extends SlashCommand {

    public Logs() {
        this.name = "logs";
        this.help = "Informs the user of how to get a log file.";
        this.defaultEnabled = false;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed logsEmbed = new EmbedBuilder()
                .setTitle("How to get a log file:")
                .setDescription(
                        "Game logs can be found in the `logs` sub-directory in .minecraft on clients, or the" +
                        " `logs` sub-folder of the server directory for servers. We're probably interested in the file named" +
                        " `latest.log`, which you can drag and drop into Discord."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(logsEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
