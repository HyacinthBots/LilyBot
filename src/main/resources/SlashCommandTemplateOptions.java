package net.irisshaders.lilybot.commands;
// package <package>;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/*
This Template class makes use of of JDA Chewtils to create the slash command. It is reccomended that you also
use Chewtils for you bot's slash commands
 */

public class Command extends SlashCommand {

    /*
    A method for defining all the settings and parameters of you slash command
     */
    public Command() {
        this.name = "command"; // The name that will appear in the suggestion list. Must to be lower case

        this.help = "Command help message."; // The description of your command

        this.defaultEnabled = true; // Sets weather this command is enabled or disabled by default. If disabled you must specify a permission

//        this.enabledRoles = new String[]{MainClass.ROLE};   Sets the roles that allow you to use the command. Ideally you will have a specified role
//                                                              in you main class that is set with an id in a .env file. Not needed if this.defaultEnabled is true

        this.guildOnly = false; // Sets weather the command can be used in dms too. Not Threads count as DMs because Discord is concern

        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE}; // Set's the required permission for the command to work

        this.botMissingPermMessage = "The bot can't write messages!"; // The error message shown to the user if permission(s) above aren't full filled

        List<OptionData> optionData = new ArrayList<>(); // Creates an array list for the options

        // Adds an option to the array list of type Boolean. Named hi, and has a description
        optionData.add(new OptionData(OptionType.BOOLEAN, "hi", "Say hello").setRequired(true));

        this.options = optionData; // Sets the options to be the optionData array
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser(); // Get's the user that ran the command
        boolean option = event.getOption("hi").getAsBoolean();

        MessageEmbed embed = new EmbedBuilder() // Creates a builder for the embed, name "builder" in this case
                .setTitle("Command Title!")
                .setDescription(
                        "Command Description!"
                )
                .setColor(Color.RED) // Java AWT Color is used here. It may not have to be, unknown
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl()) // Get's the user's tag and their Profile picture and adds them to the footer
                .setTimestamp(Instant.now()) // Get's the timer the command was sent. This will be lined up with any user's System time
                .build(); // Builds the embed

        if (option) { // Use that option to decide what to reply to the message with
            event.reply("Hi").queue();

        } else {
            /*
            Replies to the original message, without mentioning (pinging) the user. It is also not Ephemeral so anyone can see it
            .queue() Is required to tell JDA to queue up the reply
            */
            event.replyEmbeds(embed).mentionRepliedUser(false).setEphemeral(false).queue();
        }
    }
}