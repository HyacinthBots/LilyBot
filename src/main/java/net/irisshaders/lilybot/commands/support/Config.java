package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;

public class Config extends SlashCommand {

    public Config() {
        this.name = "config";
        this.help = "Informs the user of Shader Config issues.";
        this.defaultEnabled = false;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed configEmbed = new EmbedBuilder()
                .setTitle("You can't currently edit shader settings.")
                .setDescription(
                        "A configuration menu will be coming in a future version. " +
                        "(<https://github.com/IrisShaders/Iris/issues/663>)"
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(configEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
