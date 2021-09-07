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

public class RuleThree extends SlashCommand {

    public RuleThree() {
        this.name = "rule-3";
        this.help = "Reminds the user of Rule 3";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE, LilyBot.HELPER_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleThreeEmbed = new EmbedBuilder()
                .setTitle("**Rule 3**")
                .setDescription(
                        "Keep discussion on-topic. This server is focused around Iris." +
                        " General chatter unrelated to Iris is best kept to other Discord communities or to DMs."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleThreeEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
