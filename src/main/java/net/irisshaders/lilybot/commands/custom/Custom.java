package net.irisshaders.lilybot.commands.custom;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;
import java.util.Properties;

public class Custom extends SlashCommand {
    private final String title;
    private final String desc;
    private final Color color;

    public Custom(String name, String help, String title, String desc, Color color) {
        this.title = title;
        this.desc = desc;
        this.color = color;
        this.name = name;
        this.help = help;
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{ Permission.MESSAGE_WRITE };
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
    }

    public static Custom parse(String name, Properties properties) {
        var color = Color.RED;
        if (properties.getProperty(name + ".color") != null) {
            color = Color.decode(properties.getProperty(name + ".color"));
        }
        return new Custom(
                name,
                properties.getProperty(name + ".help"),
                properties.getProperty(name + ".title"),
                properties.getProperty(name + ".desc"),
                color
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        var user = event.getUser();

        var embed = new EmbedBuilder()
                .setTitle(this.title)
                .setDescription(this.desc)
                .setColor(this.color)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(embed).mentionRepliedUser(false).setEphemeral(false).queue();
    }
}
