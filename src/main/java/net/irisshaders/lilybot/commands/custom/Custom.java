package net.irisshaders.lilybot.commands.custom;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;
import java.util.Properties;

public class Custom extends SlashCommand {
    private final String title;
    private final String desc;
    private final Color color;

    public Custom(String name, String help, String title, String desc, SlashCommand[] children, Color color) {
        this.title = title;
        this.desc = desc;
        this.color = color;
        this.name = name;
        this.help = help;
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.botPermissions = new Permission[]{ Permission.MESSAGE_SEND };
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
        this.children = children;
    }

    private static SlashCommand parse(String key, String name, Properties properties) {
        var color = Color.RED;
        if (properties.getProperty(key + ".color") != null) {
            color = Color.decode(properties.getProperty(key + ".color"));
        }

        var children = new SlashCommand[0];
        if (properties.getProperty(key + ".children") != null) {
            var childNames = properties.getProperty(key + ".children").split(" ");
            children = new SlashCommand[childNames.length];

            String child;
            for (int i = 0; i < childNames.length; i++) {
                child = childNames[i];
                children[i] = parse(key + ".child." + child, child, properties);
            }
        }

        return new Custom(
                name,
                properties.getProperty(key + ".help", "A Lily bot command."),
                properties.getProperty(key + ".title", name),
                properties.getProperty(key + ".desc", ""),
                children,
                color
        );
    }

    public static SlashCommand parse(String name, Properties properties) {
        return parse(name, name, properties);
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        var user = event.getUser();

        var embed = ResponseHelper.responseEmbed(this.title, user, this.color)
                .setDescription(this.desc)
                .build();

        event.replyEmbeds(embed).mentionRepliedUser(false).setEphemeral(false).queue();
    }
}
