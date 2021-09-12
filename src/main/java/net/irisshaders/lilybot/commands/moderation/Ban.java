package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Ban extends SlashCommand {

    public Ban() {
        this.name = "ban";
        this.help = "Bans a member! Goodbye!";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.botMissingPermMessage = "The bot does not have the `BAN MEMBERS` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to ban.").setRequired(true));
        optionData.add(new OptionData(OptionType.INTEGER, "days", "Days of messages to delete.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the ban.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        TextChannel action_log = event.getGuild().getTextChannelById(LilyBot.ACTION_LOG);
        Member member = event.getOption("member").getAsMember();
        int days = Integer.parseInt(event.getOption("days").getAsString());
        String reason = event.getOption("reason") == null ? "No reason provided." : event.getOption("reason").getAsString();
        User user = event.getUser();

        member.ban(days, reason).queue(unused -> {

            MessageEmbed banEmbed = new EmbedBuilder()
                    .setTitle("Banned a member.")
                    .setColor(Color.CYAN)
                    .addField("Banned:", member.getUser().getAsTag(), false)
                    .addField("Days of messages deleted:", String.valueOf(days), false)
                    .addField("Reason:", reason, false)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(banEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
            action_log.sendMessageEmbeds(banEmbed).queue();

        }, throwable -> event.replyEmbeds(ResponseHelper.genFailureEmbed(user, "Failed to ban.", null))
                            .mentionRepliedUser(false).setEphemeral(true).queue()
        );

    }

}
