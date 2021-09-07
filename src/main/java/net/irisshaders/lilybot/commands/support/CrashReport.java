package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class CrashReport extends SlashCommand {

    public CrashReport() {
        this.name = "crash-report";
        this.help = "Informs the user of how to get a crash report.";
        this.defaultEnabled = true;
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed crashReportEmbed = new EmbedBuilder()
                .setTitle("How to get crash reports:")
                .setDescription(
                        "Crash reports can be found in the `crash-reports` sub-directory in" +
                        " `.minecraft` on clients, or the crash-reports sub-directory of the server directory for servers. We're probably" +
                        " interested in the most recent file, which you can drag and drop into Discord."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(crashReportEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
