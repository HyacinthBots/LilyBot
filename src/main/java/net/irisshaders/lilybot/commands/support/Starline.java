package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;

public class Starline extends SlashCommand {

    public Starline() {
        this.name = "starline";
        this.help = "Informs the use of the lack of Starline support.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE, LilyBot.HELPER_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed starlineEmbed = new EmbedBuilder()
                .setTitle("Starline is not supported.")
                .setDescription(
                        "Starline is an unofficial fork and not supported by us, please move to" +
                        " #general if you need support with it."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(starlineEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
