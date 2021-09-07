package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Kick extends SlashCommand {

    public Kick() {
        this.name = "kick";
        this.help = "Kicks a member! Goodbye!";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.KICK_MEMBERS};
        this.botMissingPermMessage = "The bot does not have the `KICK MEMBERS` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to kick.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the kick.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        TextChannel action_log = event.getGuild().getTextChannelById(LilyBot.ACTION_LOG);
        Member member = event.getOption("member").getAsMember();
        String reason = event.getOption("reason") == null ? "No reason provided." : event.getOption("reason").getAsString();
        User user = event.getUser();

        member.kick(reason).queue(unused -> {

            MessageEmbed kickEmbed = new EmbedBuilder()
                    .setTitle("Kicked a member")
                    .addField("Kicked:", member.getUser().getAsTag(), false)
                    .addField("Reason:", reason, false)
                    .setColor(Color.GREEN)
                    .setFooter("Requested by " + user.getAsTag(), user.getAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(kickEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
            action_log.sendMessageEmbeds(kickEmbed).queue();


        });

    }

}
