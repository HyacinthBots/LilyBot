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

public class RuleEight extends SlashCommand {

    public RuleEight() {
        this.name = "rule-8";
        this.help = "Reminds the user of Rule 8";
        this.defaultEnabled = false;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleEightEmbed = new EmbedBuilder()
                .setTitle("**Rule 8**")
                .setDescription(
                        "Adhere to the Discord Terms of Service. I'd like to avoid getting" +
                        " this community banned. No piracy! Absolutely no support will be provided for people running cracked" +
                        " versions of the game. Providing support for these people counts as a rule violation!"
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleEightEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
