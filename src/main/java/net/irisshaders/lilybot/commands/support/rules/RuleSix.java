package net.irisshaders.lilybot.commands.support.rules;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;

public class RuleSix extends SlashCommand {

    public RuleSix() {
        this.name = "rule-6";
        this.help = "Reminds the user of Rule 6";
        this.defaultEnabled = false;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleSixEmbed = new EmbedBuilder()
                .setTitle("**Rule 6**")
                .setDescription(
                        "No links to executable files or JAR files. Uploading or directly" +
                        " linking to executable files is not allowed without prior approval."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleSixEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
