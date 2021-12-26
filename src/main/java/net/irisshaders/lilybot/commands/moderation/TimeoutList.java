package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.DateHelper;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;
import java.util.Collection;

public class TimeoutList extends SlashCommand {

    public TimeoutList() {
        this.name = "timeout-list";
        this.help = "Lists all timed out members.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE, Constants.TRIAL_MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
    	event.deferReply(true);
        Collection<Member> timedOutMembers = event.getGuild().getMembers().parallelStream().filter(Member::isTimedOut).toList();

        if (timedOutMembers.isEmpty()) {

            MessageEmbed noTimedOutMembers = ResponseHelper.responseEmbed(event.getUser(), Color.CYAN) 
                    .setDescription("There are no timed out members.")
                    .build();

            event.replyEmbeds(noTimedOutMembers).mentionRepliedUser(false).setEphemeral(true).queue();

        } else {

            EmbedBuilder timeOutListEmbedBuilder = ResponseHelper.responseEmbed("Timed out members", event.getUser(), Color.CYAN)
                    .setDescription("These are the timed out members:");
            for (Member member: timedOutMembers) {
                timeOutListEmbedBuilder.addField("Member:", ResponseHelper.userField(member.getUser()), false);
                timeOutListEmbedBuilder.addField("Expires:", DateHelper.formatDateAndTime(member.getTimeOutEnd().toInstant()), true);
            }

            event.replyEmbeds(timeOutListEmbedBuilder.build()).mentionRepliedUser(false).setEphemeral(true).queue();

        }

    }

}
