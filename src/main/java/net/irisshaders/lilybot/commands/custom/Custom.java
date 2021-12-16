package net.irisshaders.lilybot.commands.custom;

import com.github.jezza.TomlArray;
import com.github.jezza.TomlTable;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.Color;
import java.util.Iterator;

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

    public static SlashCommand parse(TomlTable command) {
        var color = Color.RED;
        if (command.get("color") instanceof String colorString) { //null check and cast
            color = Color.decode(colorString);
        }

        var children = new SlashCommand[0];
        if (command.get("subcommand") instanceof TomlArray subcommands) {
            children = new SlashCommand[subcommands.size()];
            Iterator<Object> iterator = subcommands.iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                children[i] = parse((TomlTable)iterator.next());
            }
        }

        return new Custom(
                (String)command.get("name"),
                (String)command.getOrDefault("help", "A Lily bot command."),
                (String)command.getOrDefault("title", null),
                (String)command.getOrDefault("description", ""),
                children,
                color
        );
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
