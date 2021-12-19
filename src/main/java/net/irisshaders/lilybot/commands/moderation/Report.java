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
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.ResponseHelper;

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

            String id = event.getComponentId().split(":")[1];

            if (id.equals("report")) {
                event.reply("We are currently waiting for threads to be implemented in JDA." +
                        " After that, we will finish this command.").mentionRepliedUser(false).setEphemeral(true).queue();
                // TODO THREADS
            }

        } else if (genericEvent instanceof MessageContextCommandEvent event) {

            String name = event.getName();

            if (name.equals("Report message")) {

                User user = event.getUser();
                String id = event.getChannel().getId();

                MessageEmbed reportEmbed = ResponseHelper.responseEmbed("Report Message", user, Color.RED)
                        .setDescription("Would you like to report this message? This will ping moderators, and false reporting will be treated as spam and punished accordingly.")
                        .build();

                event.replyEmbeds(reportEmbed).addActionRow(
                        Button.of(ButtonStyle.PRIMARY, "report:yes", "Yes", Emoji.fromUnicode("\u2705")),
                        Button.of(ButtonStyle.PRIMARY, "report:no", "No", Emoji.fromUnicode("\u274C"))
                ).mentionRepliedUser(false).setEphemeral(true).queue(interactionHook -> LilyBot.INSTANCE.waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
                    if (!buttonClickEvent.getUser().equals(user)) return false;
                    if (!equalsAny(buttonClickEvent.getComponentId())) return false;
                    return !buttonClickEvent.isAcknowledged();
                }, buttonClickEvent -> {
                    User buttonClickEventUser = buttonClickEvent.getUser();
                    String buttonClicked = buttonClickEvent.getComponentId().split(":")[1];

                    switch (buttonClicked) {
                        case "yes" -> {
                            Message message = event.getTargetMessage();
                            User author = message.getAuthor();
                            String contentDisplay = message.getContentDisplay();
                            String messageUrl = message.getJumpUrl();
                            Guild guild = event.getGuild();
                            TextChannel actionLog = guild.getTextChannelById(Constants.ACTION_LOG);
                            String channel = String.format("<#%s>", id);
                            String mention = String.format("<@&%s>", Constants.MODERATOR_ROLE);
                            String mention2 = String.format( "<@&%s>", Constants.TRIAL_MODERATOR_ROLE);

                            if (contentDisplay.length() > 100) {
                                contentDisplay = contentDisplay.substring(0, 99) + "...";
                            }

                            String finalContentDisplay = contentDisplay;

                            buttonClickEvent.editComponents().setEmbeds(reportMessage(user, author, "Report a message", contentDisplay, channel)).queue();
                            actionLog.sendMessage(mention).queue(message1 -> {
                                actionLog.sendMessage(mention2).queue();
                                actionLog.sendMessageEmbeds(reportMessage(user, author, "Reported message", finalContentDisplay, channel)).setActionRow(
                                        Button.link(messageUrl, "Message Link")
                                ).queue();
                            });

                        }
                        case "no" -> {
                            MessageEmbed noReportEmbed = new EmbedBuilder()
                                    .setTitle("Report canceled")
                                    .setColor(Color.GREEN)
                                    .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                                    .setTimestamp(Instant.now())
                                    .build();

                            buttonClickEvent.editComponents().setEmbeds(noReportEmbed).queue();
                        }
                    }
                }));
            }
        }
    }

    private MessageEmbed reportMessage(User user, User author, String title, String contentDisplay, String channel) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription("A message was reported in " + channel)
                .addField("Message Content:", contentDisplay, false)
                .addField("Message Author:", ResponseHelper.userField(author), false)
                .setColor(Color.CYAN)
                .setFooter("Message reported by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }

    private boolean equalsAny(String id) {
        return id.equals("report:yes") ||
                id.equals("report:no");
    }

}
