package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.commands.MessageContextCommandEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.irisshaders.lilybot.utils.Constants;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;

@SuppressWarnings("ConstantConditions")
public class Report extends Command implements EventListener {

    public Report() {
        this.name = "report";
        this.help = "Triggers the bot to send the report embed and button.";
        this.hidden = true;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {

        MessageChannel channel = event.getChannel();
        MessageEmbed reportEmbed = new EmbedBuilder()
                .setTitle("Report a problem to a mod.")
                .setDescription(
                        "This should only be used when a case of severe-rule breaking has occurred and " +
                        "there are no mods available to take control of the situation."
                ).setColor(Color.RED)
                .build();
        channel.sendMessageEmbeds(reportEmbed).setActionRow(
                Button.of(ButtonStyle.PRIMARY, "report:report", "Report", Emoji.fromUnicode("\u26A0"))
        ).queue();

    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {

        if (genericEvent instanceof ButtonClickEvent event) {

            String id = event.getButton().getId().split(":")[1];

            if (id.equals("report")) {
                event.reply("We are currently waiting for threads to be implemented in JDA." +
                        " After that, we will finish this command.").mentionRepliedUser(false).setEphemeral(true).queue();
                // TODO THREADS
            }

        } else if (genericEvent instanceof MessageContextCommandEvent event) {

            String name = event.getName();

            if (name.equals("Report message")) {

                User user = event.getUser();
                Message message = event.getTargetMessage();
                String id = message.getChannel().getId();
                User author = message.getAuthor();
                String contentDisplay = message.getContentDisplay();
                String messageUrl = message.getJumpUrl();
                Guild guild = event.getGuild();
                TextChannel actionLog = guild.getTextChannelById(Constants.ACTION_LOG);
                String channel = String.format("<#%s>", id);
                String mention = String.format("<@&%s>", Constants.MODERATOR_ROLE);

                if (contentDisplay.length() > 100) {
                    contentDisplay = contentDisplay.substring(0, 99) + "...";
                }

                event.replyEmbeds(reportMessage(user, author, "Report a message", contentDisplay, messageUrl, channel)).setEphemeral(true).queue();
                actionLog.sendMessage(mention).queue();
                actionLog.sendMessageEmbeds(reportMessage(user, author, "Reported message", contentDisplay, messageUrl, channel)).queue();

            }

        }

    }

    private MessageEmbed reportMessage(User user, User author, String title, String contentDisplay, String messageUrl, String channel) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription("A message was reported in " + channel)
                .addField("Message Content:", contentDisplay, false)
                .addField("Message Author:", author.getAsTag(), false)
                .addField("Message Link:", messageUrl, false)
                .setColor(Color.CYAN)
                .setFooter("Message reported by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }

}
