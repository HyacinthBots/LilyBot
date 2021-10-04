package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS};
        this.botMissingPermMessage = "The bot does not have the `MESSAGE WRITE` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.STRING, "message", "What do you want to send in the channel.").setRequired(true));
        optionData.add(new OptionData(OptionType.CHANNEL, "channel", "The channel to send the message in.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        String message = event.getOption("message").getAsString();
        MessageChannel channel = event.getOption("channel") == null ?
                event.getChannel() : event.getOption("channel").getAsMessageChannel();
        boolean audio = channel.getType().isAudio();
        User user = event.getUser();

        if (audio) {
            event.replyEmbeds(ResponseHelper.genFailureEmbed(user, "You cannot send messages in a VC", null))
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

        try {
            channel.sendMessage(say).queue(success -> event.replyEmbeds(saySuccessEmbed).mentionRepliedUser(false)
                    .setEphemeral(true).queue());
        } catch (Exception e) {
            event.replyEmbeds(sayFailEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
        }

    }

}
