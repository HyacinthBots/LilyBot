package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Say extends SlashCommand {

    public Say() {
        this.name = "say";
        this.help = "Sends a message in a specified channel. If none is given, it sends in the current one.";
        this.defaultEnabled = true;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.STRING, "message", "What you want to send in the channel.").setRequired(true));
        optionData.add(new OptionData(OptionType.CHANNEL, "channel", "The channel to send the message in.").setRequired(false));
        optionData.add(new OptionData(OptionType.BOOLEAN, "embed", "Whether the response should be in the form of an embed or not.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        String message = event.getOption("message").getAsString();
        MessageChannel channel = event.getOption("channel") == null ?
                event.getChannel() : event.getOption("channel").getAsMessageChannel();
        boolean isEmbed = event.getOption("embed") != null &&
                event.getOption("embed").getAsBoolean();
        boolean audio = channel.getType().isAudio();
        User user = event.getUser();
        JDA jda = event.getJDA();
        TextChannel actionLog = jda.getTextChannelById(Constants.ACTION_LOG);
        String channelString = String.format("<#%s>", channel.getId());

        if (audio) {
            event.replyEmbeds(ResponseHelper.genFailureEmbed(user, "You cannot send messages in a voice channel!", null))
                    .mentionRepliedUser(false).setEphemeral(true).queue();
            return;
        }

        // left for later
        MessageEmbed sayEmbed = new EmbedBuilder()
                .setDescription(message)
                .setColor(0x9992ff)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        Message say = new MessageBuilder().setContent(message).build();

        MessageEmbed sayFailEmbed = ResponseHelper.genFailureEmbed(user, "Failed to send message in specified channel",
                "This could be for a variety of reasons, such as not having the required permissions," +
                        " or the channel being deleted.");

        MessageEmbed saySuccessEmbed = new EmbedBuilder()
                .setTitle("Sent message in specified channel")
                .setColor(0x9992ff)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        MessageEmbed actionLogEmbed = new EmbedBuilder()
                .setTitle(user.getAsTag() + " used the Say Command")
                .addField("Message sent:", message, false)
                .addField("Channel:", channelString, false)
                .setColor(0x9992ff)
                .setTimestamp(Instant.now())
                .build();

        try {
            if (isEmbed) {
                channel.sendMessageEmbeds(sayEmbed).queue(message1 -> reply(event, actionLog, saySuccessEmbed, actionLogEmbed, message1));
            } else {
                channel.sendMessage(say).queue(message1 -> reply(event, actionLog, saySuccessEmbed, actionLogEmbed, message1));
            }
        } catch (Exception e) {
            event.replyEmbeds(sayFailEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
        }

    }

    private void reply(SlashCommandEvent event, TextChannel actionLog, MessageEmbed saySuccessEmbed, MessageEmbed actionLogEmbed, Message message1) {
        event.replyEmbeds(saySuccessEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
        actionLog.sendMessageEmbeds(actionLogEmbed).setActionRow(
                Button.link(message1.getJumpUrl(), "Click here to see the message")
        ).queue();
    }

}
