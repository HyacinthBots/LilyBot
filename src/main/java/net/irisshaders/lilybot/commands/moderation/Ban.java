package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Ban extends SlashCommand {

    public Ban() {
        this.name = "ban";
        this.help = "Bans a member! Goodbye!";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.botMissingPermMessage = "The bot does not have the `BAN MEMBERS` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "target", "The user to ban.").setRequired(true));
        optionData.add(new OptionData(OptionType.INTEGER, "days", "Days of messages to delete.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the ban.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        TextChannel actionLog = event.getGuild().getTextChannelById(Constants.ACTION_LOG);
        User target = event.getOption("target").getAsUser();
        int days = Integer.parseInt(event.getOption("days").getAsString());
        String reason = event.getOption("reason") == null ? "No reason provided." : event.getOption("reason").getAsString();
        User requester = event.getUser();
        JDA jda = event.getJDA();

        if (target.equals(requester)) {
            event.replyEmbeds(ResponseHelper.genFailureEmbed(requester, "You can't ban yourself!",
                    "What were you thinking!")).mentionRepliedUser(false).setEphemeral(true).queue();
            return;
        }

        // Runs when user attempts to ban the bot.
        if (target.equals(jda.getSelfUser())) {
            event.reply("bruh").mentionRepliedUser(false).setEphemeral(false).queue();
            return;
        }

        event.getGuild().ban(target, days, reason).queue(unused -> {

            MessageEmbed banEmbed = ResponseHelper.responseEmbed("Banned a member.", requester, Color.CYAN)
                    .addField("Banned:", ResponseHelper.userField(target), false)
                    .addField("Days of messages deleted:", String.valueOf(days), false)
                    .addField("Reason:", reason, false)
                    .build();

            event.replyEmbeds(banEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
            actionLog.sendMessageEmbeds(banEmbed).queue();

        }, throwable -> event.replyEmbeds(ResponseHelper.genFailureEmbed(requester, "Failed to ban " + target.getAsTag(), null))
                            .mentionRepliedUser(false).setEphemeral(true).queue()
        );

    }

}
