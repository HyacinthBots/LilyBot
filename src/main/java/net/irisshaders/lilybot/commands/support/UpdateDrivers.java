package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class UpdateDrivers extends SlashCommand {

    public UpdateDrivers() {
        this.name = "drivers";
        this.help = "Informs the user on how to update drivers.";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.children = new SlashCommand[]{
                new AMDDrivers(),
                new NvidiaDrivers(),
                new IntelDrivers()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // Ignored because all commands are sub-commands
    }

    public static class AMDDrivers extends SlashCommand {

        public AMDDrivers() {
            this.name = "radeon";
            this.help = "Informs the user that they should try to update their AMD Radeon drivers";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            User user = event.getUser();

            event.replyEmbeds(driverUtils(user, "Update your AMD Radeon Drivers", DriverDescriptions.AMDDrivers))
                    .mentionRepliedUser(false).setEphemeral(false).queue();
        }
    }

    public static class NvidiaDrivers extends SlashCommand {

        public NvidiaDrivers() {
            this.name = "nvidia";
            this.help = "Informs the user that they should try to update their Nvidia drivers";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            User user = event.getUser();

            event.replyEmbeds(driverUtils(user, "Update your Nvidia Drivers", DriverDescriptions.NvidiaDrivers))
                    .mentionRepliedUser(false).setEphemeral(false).queue();
        }
    }

    public static class IntelDrivers extends SlashCommand {

        public IntelDrivers() {
            this.name = "intel";
            this.help = "Informs the user that they should try to update their Intel drivers";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            User user = event.getUser();

            event.replyEmbeds(driverUtils(user, "Update your Intel Drivers", DriverDescriptions.IntelDrivers))
                    .mentionRepliedUser(false).setEphemeral(false).queue();
        }
    }

    private static MessageEmbed driverUtils(User user, String title, String description) {

        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }

    private interface DriverDescriptions {

        String AMDDrivers = "You can use AMD's Radeon Software to easily update your drivers, If you don't have it, " +
                "download it here: https://www.amd.com/en/support";
        String NvidiaDrivers = "You can use GeForce Experience to easily update drivers, If you don't have it, " +
                "download it here: https://www.nvidia.com/Download/index.aspx?lang=us";
        String IntelDrivers = "You can use Intel DSA to easily update your drivers, If you don't have it, download it " +
                "here: https://www.intel.com/content/www/us/en/support/detect.html";
    }
}
