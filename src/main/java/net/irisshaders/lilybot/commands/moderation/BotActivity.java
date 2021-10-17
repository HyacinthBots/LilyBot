package net.irisshaders.lilybot.commands.moderation;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.Presence;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.Constants;

public class BotActivity extends SlashCommand{

    public BotActivity() {

        this.name = "set-status";
        this.help = "Sets the bot's custom status.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
        this.guildOnly = false;
        this.botPermissions = new Permission[]{};

        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.STRING, "status", "The Custom Status").setRequired(true));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();
        String option = event.getOption("status").getAsString();

        event.getJDA().getPresence().setActivity(Activity.playing(option));
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Changed Custom Status!")
                .setDescription(
                        "Changed Custom Status to " + option
                )
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
        event.getGuild().getTextChannelById(Constants.ACTION_LOG).sendMessageEmbeds(embed).queue();
        event.replyEmbeds(embed).setEphemeral(true).queue();


    }
}
