package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Clear extends SlashCommand {

    public Clear() {
        this.name = "clear";
        this.help = "Clears messages. If no input is given, it clears 100 by default.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_MANAGE};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE MANAGE` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.INTEGER, "messages", "The number of messages to clear.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        TextChannel action_log = event.getGuild().getTextChannelById(LilyBot.ACTION_LOG);
        int index = Integer.parseInt(event.getOption("messages") == null ? "100" : event.getOption("messages").getAsString());
        User user = event.getUser();
        MessageChannel channel = event.getChannel();

        try {

            List<Message> messageHistory = channel.getHistory().retrievePast(index).complete();
            channel.purgeMessages(messageHistory);

            MessageEmbed deletedMessagesEmbed = new EmbedBuilder()
                    .setTitle("Successfully deleted " + index + " messages.")
                    .setColor(Color.GREEN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(deletedMessagesEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
            action_log.sendMessageEmbeds(deletedMessagesEmbed).queue();

        } catch (Exception exception) {

            MessageEmbed clearFailureEmbed = new EmbedBuilder()
                    .setTitle("Error while clearing " + index + " messages.")
                    .setDescription("Invalid number of messages selected. Must be between 2 and 100.")
                    .setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(clearFailureEmbed).mentionRepliedUser(false).setEphemeral(true).queue();

        }

    }

}
