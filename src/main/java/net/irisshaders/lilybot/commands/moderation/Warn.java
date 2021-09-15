package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.utils.Constants;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Warn extends SlashCommand {

    public Warn() {
        this.name = "warn";
        this.help = "Warns a member for any infractions.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
        this.guildOnly = true;
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to warn.").setRequired(true));
        optionData.add(new OptionData(OptionType.INTEGER, "points", "The number of points the user should be given.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the warn").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User target = event.getOption("member").getAsUser();
        int points = Integer.parseInt(event.getOption("points").getAsString());
        String reason = event.getOption("reason").getAsString();

        // TODO Finish warn command - some sort of way to write data to a file to read later

    }

}
