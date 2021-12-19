package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.irisshaders.lilybot.commands.moderation.Mute.MuteEntry;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.DateHelper;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;
import java.util.Collection;

public class MuteList extends SlashCommand {

    public MuteList() {
        this.name = "mute-list";
        this.help = "Lists all muted members.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE, Constants.TRIAL_MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.botMissingPermMessage = "The bot does not have the `MANAGE_ROLES` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        User user = event.getUser();
        JDA jda = event.getJDA();
        Collection<MuteEntry> mutedMembers = Mute.getCurrentMutes(jda).values();

        if (mutedMembers.isEmpty()) {

            MessageEmbed noMutedMembers = ResponseHelper.responseEmbed(user, Color.CYAN) 
                    .setDescription("There are no muted members.")
                    .build();

            event.replyEmbeds(noMutedMembers).mentionRepliedUser(false).setEphemeral(true).queue();

        } else {

            EmbedBuilder muteListEmbedBuilder = ResponseHelper.responseEmbed("Muted Users", user, Color.CYAN)
                    .setDescription("These are the muted members:");
            for (MuteEntry mute: mutedMembers) {
                muteListEmbedBuilder.addField("User:", ResponseHelper.userField(mute.target().getUser()), false);
                muteListEmbedBuilder.addField("Muted by:", mute.requester().getAsTag(), true);
                muteListEmbedBuilder.addField("Expires:", DateHelper.formatDateAndTime(mute.expiry()), true);
                muteListEmbedBuilder.addField("Reason:", mute.reason(), true);
            }

            event.replyEmbeds(muteListEmbedBuilder.build()).mentionRepliedUser(false).setEphemeral(true).queue();

        }

    }

}
