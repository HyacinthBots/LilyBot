package net.irisshaders.lilybot.commands.support.rules;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class RuleOne extends SlashCommand {

    public RuleOne() {
        this.name = "rule-1";
        this.help = "Reminds the user of Rule 1";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleOneEmbed = new EmbedBuilder()
                .setTitle("**Rule 1**")
                .setDescription(
                        "Be decent to one another. We're all human. Any and all forms of" +
                        " bigotry, harassment, doxxing, exclusionary, or otherwise abusive behavior will not be tolerated." +
                        " Excessive rudeness, impatience, and hostility are not welcome. Do not rage out or make personal attacks" +
                        " against other people. Do not encourage users to brigade/raid other communities."
                ).setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleOneEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
