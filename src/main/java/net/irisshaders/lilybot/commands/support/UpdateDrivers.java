package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class UpdateDrivers extends SlashCommand {
    public UpdateDrivers() {
        this.name = "update-drivers";

        this.help = "Informs the user on how to update drivers.";

        this.defaultEnabled = true;

        this.guildOnly = false;

        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};

        this.botMissingPermMessage = "The bot can't write messages!";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Update your Drivers.")
                .setDescription(
                        """
                                This can be done in multiple ways, depending on your graphics card:
                                
                                AMD: You can use AMD's Radeon Software to easily update your drivers, If you don't have it, download it here: https://www.amd.com/en/support
                                
                                NVIDIA: You can use GeForce Experience to easily update drivers, If you don't have it, download it here: https://www.nvidia.com/Download/index.aspx?lang=us
                                
                                Intel: You can use Intel DSA to easily update your drivers, If you don't have it, download it here: https://www.intel.com/content/www/us/en/support/detect.html"""
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
        event.replyEmbeds(embed).mentionRepliedUser(false).setEphemeral(false).queue();
    }
}
