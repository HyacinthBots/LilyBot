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

public class RuleFive extends SlashCommand {

    public RuleFive() {
        this.name = "rule-5";
        this.help = "Reminds the user of Rule 5";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE, LilyBot.HELPER_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed ruleFiveEmbed = new EmbedBuilder()
                .setTitle("**Rule 5**")
                .setDescription(
                        "Do not ask for support on compiling Iris, and refrain from providing this support." +
                        " There is sufficient information for people who know what they're doing to compile the mod themselves" +
                        " without help. The fact that compiled builds are only distributed to Patrons is intentional, and is" +
                        " intended to keep the support burden manageable."
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(ruleFiveEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
