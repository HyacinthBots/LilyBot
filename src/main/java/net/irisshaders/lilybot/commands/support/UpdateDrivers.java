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
        this.name = "update-drivers"; // The name that will appear in the suggestion list. Must to be lower case

        this.help = "Informs the user on how to update drivers."; // The description of your command

        this.defaultEnabled = true; // Sets weather this command is enabled or disabled by default. If disabled you must specify a permission

//        this.enabledRoles = new String[]{MainClass.ROLE};   Sets the roles that allow you to use the command. Ideally you will have a specified role
//                                                              in you main class that is set with an id in a .env file. Not needed if this.defaultEnabled is true

        this.guildOnly = false; // Sets weather the command can be used in dms too. Not Threads count as DMs because Discord is concern

        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE}; // Set's the required permission for the command to work

        this.botMissingPermMessage = "The bot can't write messages!"; // The error message shown to the user if permission(s) above aren't full filled
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser(); // Get's the user that ran the command

        MessageEmbed embed = new EmbedBuilder() // Creates a builder for the embed, name "builder" in this case
                .setTitle("Update your Drivers.")
                .setDescription(
                        "This can be done in multiple ways, depending on your graphics card:\n\n" +
                                "AMD: You can use AMD's Radeon Software to easily update your drivers, If you don't have it, download it here: https://www.amd.com/en/support\n\n" +
                                "NVIDIA: You can use GeForce Experience to easily update drivers, If you dont have it, download it here: https://www.nvidia.com/Download/index.aspx?lang=us"
                )
                .setColor(Color.RED) // Java AWT Color is used here. It may not have to be, unknown
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl()) // Get's the user's tag and their Profile picture and adds them to the footer
                .setTimestamp(Instant.now()) // Get's the timer the command was sent. This will be lined up with any user's System time
                .build(); // Builds the embed

        /*
        Replies to the original message, without mentioning (pinging) the user. It is also not Ephemeral so anyone can see it
        .queue() Is required to tell JDA to queue up the reply
         */
        event.replyEmbeds(embed).mentionRepliedUser(false).setEphemeral(false).queue();
    }
}
