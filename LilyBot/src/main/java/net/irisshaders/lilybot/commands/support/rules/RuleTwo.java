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

public class RuleTwo extends SlashCommand {

    public RuleTwo() {
        this.name = "rule-2";
        this.help = "Reminds the user of Rule 2";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE, LilyBot.HELPER_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleTwoEmbed = new EmbedBuilder()
                .setTitle("**Rule 2**")
                .setDescription(
                        "Keep chat clean. Do not spam text, images, user mentions, emojis," +
                        " reactions, or anything else. Do not post offensive, obscene, politically charged, or sexually explicit" +
                        " content. Avoid using vulgar language and excessive profanity. Use English at all times in public channels."
                ).setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleTwoEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
