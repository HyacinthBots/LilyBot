package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class Indium extends SlashCommand {

    public Indium() {
        this.name = "indium";
        this.help = "Informs the user of the need to use Indium.";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed indiumEmbed = new EmbedBuilder()
                .setTitle("You may need Indium for some mods features to work.")
                .setDescription(
                        "Indium is a minecraft mod that adds Fabric Rendering API (FRAPI) support to Sodium. It is " +
                        "compatible with both mainstream Sodium and Iris' fork of Sodium. It can be required for " +
                        "many mods, such as Connected Texture mods and Campanion. It can be found at " +
                        "this link: (<https://modrinth.com/mod/Indium>)"
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(indiumEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

    }

}
