package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.DateHelper;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Timeout extends SlashCommand {

    public Timeout() {
        this.name = "timeout";
        this.help = "Times out the specified member for the given reason and duration. Defaults to 6h and no reason.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE, Constants.TRIAL_MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.botMissingPermMessage = "The bot does not have the `MANAGE ROLES` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to timeout.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "duration", "The duration of the timeout.").setRequired(false));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the timeout.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        Member target = event.getOption("member").getAsMember();
        User user = event.getUser();
        String reason = event.getOption("reason") == null ? "No reason provided" : event.getOption("reason").getAsString();
        String duration = event.getOption("duration") == null ? "6h" : event.getOption("duration").getAsString();

        if (!target.isTimedOut()) {

            Instant expiry = Instant.now().plusSeconds(parseDuration(duration));
            TimeOutEntry timeout = new TimeOutEntry(target, user, expiry, reason);
            timeout(timeout, event);

        } else { // Ask for removing timeout
            MessageEmbed alreadyTimedOutEmbed = ResponseHelper.responseEmbed("Already timed out", user, Color.CYAN)
                    .setDescription("Do you want to remove timeout?")
                    .addField("The following member is already timed out:", target.getUser().getAsTag(), false)
                    .addField("Expires at", DateHelper.formatDateAndTime(target.getTimeOutEnd().toInstant()), false)
                    .build();
            String targetId = target.getId();

            event.replyEmbeds(alreadyTimedOutEmbed).addActionRow(
                    Button.of(ButtonStyle.PRIMARY, "timeout" + targetId + ":yes", "Yes", Emoji.fromUnicode("\u2705")),
                    Button.of(ButtonStyle.PRIMARY, "timeout" + targetId + ":no", "No", Emoji.fromUnicode("\u274C"))
            ).mentionRepliedUser(false).setEphemeral(true).queue(interactionHook -> LilyBot.INSTANCE.waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
                if (!buttonClickEvent.getUser().equals(user)) return false;
                if (!equalsAny(buttonClickEvent.getComponentId(), targetId)) return false;
                return !buttonClickEvent.isAcknowledged();
            }, buttonClickEvent -> {

                User buttonClickEventUser = buttonClickEvent.getUser();
                String id = buttonClickEvent.getComponentId().split(":")[1];

                switch (id) {

                    case "yes" -> removeTimeout(target, "Timeout removed by " + buttonClickEventUser.getAsTag(), buttonClickEvent);
                    case "no" -> {

                        MessageEmbed stillTimedOutEmbed = new EmbedBuilder()
                                .setTitle(String.format("%s is still timed out", target.getUser().getAsTag()))
                                .setColor(Color.CYAN)
                                .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .build();

                        buttonClickEvent.editComponents().setEmbeds(stillTimedOutEmbed).submit();

                    }

                }

            }));

        }

    }

    /**
     * Times out a member, messages them about it, prints to the action log and replies to the given interaction<p>
     * This method is also called from {@link Warn}.<p>
     * @param timeoutEntry The {@link TimeOutEntry} to get all the data from
     * @param interaction The interaction to reply to, or {@code null} to not reply to any event
     */
    public static void timeout(TimeOutEntry timeoutEntry, Interaction interaction) {
        Guild guild = LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID);
        TextChannel actionLog = LilyBot.INSTANCE.jda.getTextChannelById(Constants.ACTION_LOG);

        MessageEmbed timeOutEmbed = ResponseHelper.responseEmbed("Time out", timeoutEntry.requester(), Color.CYAN)
                .addField("Timed out:", ResponseHelper.userField(timeoutEntry.target().getUser()), false)
                .addField("Timed out until:", DateHelper.formatRelative(timeoutEntry.expiry()), false)
                .addField("Reason:", timeoutEntry.reason(), false)
                .build();
        MessageEmbed userEmbed = new EmbedBuilder()
                .setTitle("You were timed out")
                .addField(String.format("You are timed out from %s until:", guild.getName()), DateHelper.formatRelative(timeoutEntry.expiry()), false)
                .addField("Reason:", timeoutEntry.reason(), false)
                .setColor(Color.CYAN)
                .setFooter("Timed out by " + timeoutEntry.requester().getAsTag(), timeoutEntry.requester().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        // Send the embed as Interaction reply, in action log and to the user
        if (interaction != null) { // There may not be an event if this is a consequence of warn points
            interaction.replyEmbeds(timeOutEmbed).mentionRepliedUser(false).setEphemeral(true).submit();
        }
        actionLog.sendMessageEmbeds(timeOutEmbed).queue();

        timeoutEntry.target().getUser().openPrivateChannel()
            .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(userEmbed))
            .queue(null, throwable -> {
                MessageEmbed failedToDMEmbed = ResponseHelper.responseEmbed(timeoutEntry.requester(), Color.CYAN)
                        .setTitle("Failed to DM " + timeoutEntry.target().getUser().getAsTag() + " for time out.")
                        .build();
            actionLog.sendMessageEmbeds(failedToDMEmbed).queue();
        });
        timeoutEntry.target().timeoutUntil(timeoutEntry.expiry()).reason(timeoutEntry.reason()).queue();
    }

    /**
     * Converts a duration string to a duration in seconds.
     * Also used in {@link Warn}
     */
    public static int parseDuration(String time) {
        int duration = Integer.parseInt(time.replaceAll("[^0-9]", ""));
        String unit = time.replaceAll("[^A-Za-z]+", "").trim();
        switch (unit) {
            // I know this is cursed, but I do not care, it works
            case "s" -> duration *= 1;
            case "m", "min" -> duration *= 60;
            case "h", "hour" -> duration *= 3600;
            case "d", "day" -> duration *= 86400;
        }
        return duration;
    }

    /**
     * Removes timeout from the user from the {@link TimeOutEntry} and logs it to action log, and as an interaction response if interaction isn't {@code null}
     * @param target The {@link Member} to remove timeout from
     * @param reason The reason of the timeout removal
     * @param interaction The interaction to notify about the timeout removal, or {@code null} if there's none
     */
    private static void removeTimeout(Member target, String reason, ComponentInteraction interaction) {
        TextChannel actionLog = LilyBot.INSTANCE.jda.getTextChannelById(Constants.ACTION_LOG);

        MessageEmbed timeoutRemovalEmbed = new EmbedBuilder()
                .setTitle("Removed timeout")
                .addField("From:", target.getAsMention(), false)
                .addField("Reason:", reason, false)
                .setColor(Color.CYAN)
                .setTimestamp(Instant.now())
                .build();

        target.removeTimeout().queue(
                success -> {
                    if (interaction != null) {
                        interaction.editComponents().setEmbeds(timeoutRemovalEmbed).submit();
                    }
                    actionLog.sendMessageEmbeds(timeoutRemovalEmbed).queue();
                },
                error -> {
                    MessageEmbed errorEmbed = new EmbedBuilder()
                            .setTitle(String.format("Unable to remove timeout from %s:", target.getUser().getName()))
                            .appendDescription(error.toString())
                            .setColor(Color.RED)
                            .build();
                    actionLog.sendMessageEmbeds(errorEmbed).submit();
                    if (interaction != null) {
                        interaction.replyEmbeds(timeoutRemovalEmbed).mentionRepliedUser(false).setEphemeral(true).submit();
                    }
                    error.printStackTrace();
                }
        );
    }

    private boolean equalsAny(String id, String memberId) {
        return id.equals("timeout" + memberId + ":yes") ||
                id.equals("timeout" + memberId + ":no");
    }

    /**
     *
     * @param target The target that was timed out
     * @param requester The requester of the timeout, that may come from a warn
     *
     */
    public static record TimeOutEntry(Member target, User requester, Instant expiry, String reason) {}

}
