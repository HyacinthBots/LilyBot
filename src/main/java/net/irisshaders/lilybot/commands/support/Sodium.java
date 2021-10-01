package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class Sodium extends SlashCommand {

    public Sodium() {
        this.name = "sodium";
        this.help = "Informs the user of conflicts between mainstream Sodium and Iris.";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

         MessageEmbed sodiumEmbed = new EmbedBuilder()
                .setTitle("Remove Sodium from your mods folder.")
                .setDescription(
                        "If you're getting an error similar to `Could not execute entrypoint stage 'client'" +
                        " due to errors, provided by 'iris'!`, please remove Sodium from your mods folder. Iris bundles a version" +
                        " of Sodium that is not compatible with the official version."
                )
                 .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

         event.replyEmbeds(sodiumEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
