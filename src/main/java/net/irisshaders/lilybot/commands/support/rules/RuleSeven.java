package net.irisshaders.lilybot.commands.support.rules;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class RuleSeven extends SlashCommand {

    public RuleSeven() {
        this.name = "rule-7";
        this.help = "Reminds the user of Rule 7";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleSevenEmbed = new EmbedBuilder()
                .setTitle("**Rule 7**")
                .setDescription(
                        "Refrain from sending unsolicited pings and direct messages. Pings" +
                        " and DMs can be annoying to deal with, so please avoid using them unless they are necessary. Use pings in" +
                        " replies with discretion as well. Unsolicited support requests made with pings and DMs are a big no."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleSevenEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
