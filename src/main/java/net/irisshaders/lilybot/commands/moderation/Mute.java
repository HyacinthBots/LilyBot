package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
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
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.DateHelper;

import java.awt.*;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ConstantConditions")
public class Mute extends SlashCommand {
    private static final Timer TIMER = new Timer();
    private static final Map<Member, TimerTask> SCHEDULED_UNMUTES = new HashMap<>();

    public Mute() {
        this.name = "mute";
        this.help = "Mutes a specified member for the given reason and duration. Defaults to 6h and no reason.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE, Constants.TRIAL_MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.botMissingPermMessage = "The bot does not have the `MANAGE ROLES` permission.";
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to mute.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "duration", "The duration of the mute.").setRequired(false));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the mute.").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        Member target = event.getOption("member").getAsMember();
        User user = event.getUser();
        String reason = event.getOption("reason") == null ? "No reason provided" : event.getOption("reason").getAsString();
        String duration = event.getOption("duration") == null ? "6h" : event.getOption("duration").getAsString();
        JDA jda = event.getJDA();
        MuteEntry currentMute = getCurrentMutes(jda).get(target);

        if (currentMute == null) { // User is not muted

            Timestamp expiry = Timestamp.from(Instant.now().plusSeconds(parseDuration(duration)));
            MuteEntry mute = new MuteEntry(target, user, expiry, reason);
            mute(mute, event);

        } else { // User is muted, ask for unmuting
            MessageEmbed alreadyMutedEmbed = new EmbedBuilder()
                    .setTitle("Already muted")
                    .setDescription("Do you want to unmute? Respond with the buttons below.")
                    .addField("The following member is already muted:", target.getUser().getAsTag(), false)
                    .addField("Mute reason:", currentMute.reason(), false)
                    .addField("Muted by:", currentMute.requester().getAsTag(), false)
                    .addField("Expires at", DateHelper.formatDateAndTime(currentMute.expiry()), false)
                    .setColor(Color.CYAN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();
            String targetId = target.getId();

            event.replyEmbeds(alreadyMutedEmbed).addActionRow(
                    Button.of(ButtonStyle.PRIMARY, "mute" + targetId + ":yes", "Yes", Emoji.fromUnicode("\u2705")),
                    Button.of(ButtonStyle.PRIMARY, "mute" + targetId + ":no", "No", Emoji.fromUnicode("\u274C"))
            ).mentionRepliedUser(false).setEphemeral(true).queue(interactionHook -> LilyBot.INSTANCE.waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
                if (!buttonClickEvent.getUser().equals(user)) return false;
                if (!equalsAny(buttonClickEvent.getComponentId(), targetId)) return false;
                return !buttonClickEvent.isAcknowledged();
            }, buttonClickEvent -> {

                User buttonClickEventUser = buttonClickEvent.getUser();
                String id = buttonClickEvent.getComponentId().split(":")[1];

                switch (id) {

                    case "yes" -> unmute(currentMute, "Manually unmuted by " + buttonClickEventUser.getAsTag(), buttonClickEvent);
                    case "no" -> {

                        MessageEmbed stillMutedEmbed = new EmbedBuilder()
                                .setTitle(String.format("%s is still muted", target.getUser().getAsTag()))
                                .setColor(Color.CYAN)
                                .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .build();

                        buttonClickEvent.editComponents().setEmbeds(stillMutedEmbed).submit();

                    }

                }

            }));

        }

    }

    /**
     * Mutes a member, messages them about it, prints to the action log and replies to the given interaction<p>
     * This method is also called from {@link Warn}.<p>
     * If the {@link Member} is already muted, the database will throw an exception
     * @param mute The {@link MuteEntry} to get all the data from
     * @param interaction The interaction to reply to, or {@code null} to not reply to any event
     */
    public static void mute(MuteEntry mute, Interaction interaction) {
        Guild guild = LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID);
        TextChannel actionLog = LilyBot.INSTANCE.jda.getTextChannelById(Constants.ACTION_LOG);
        Role mutedRole = LilyBot.INSTANCE.jda.getRoleById(Constants.MUTED_ROLE);

        MessageEmbed muteEmbed = new EmbedBuilder()
                .setTitle("Mute")
                .addField("Muted:", mute.target().getUser().getAsMention(), false)
                .addField("Muted until:", DateHelper.formatRelative(mute.expiry()), false)
                .addField("Reason:", mute.reason(), false)
                .setColor(Color.CYAN)
                .setFooter("Requested by " + mute.requester().getAsTag(), mute.requester().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
        MessageEmbed userEmbed = new EmbedBuilder()
                .setTitle("You were muted")
                .addField(String.format("You are muted from %s until:", guild.getName()), DateHelper.formatRelative(mute.expiry()), false)
                .addField("Reason:", mute.reason(), false)
                .setColor(Color.CYAN)
                .setFooter("Muted by " + mute.requester().getAsTag(), mute.requester().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        // Send the embed as Interaction reply, in action log and to the user
        if (interaction != null) { // There may not be an event if this is a consequence of warn points
            interaction.replyEmbeds(muteEmbed).mentionRepliedUser(false).setEphemeral(true).submit();
        }
        actionLog.sendMessageEmbeds(muteEmbed).queue();

        mute.target().getUser().openPrivateChannel()
            .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(userEmbed))
            .queue(null, throwable -> {
                MessageEmbed failedToDMEmbed = new EmbedBuilder()
                        .setTitle("Failed to DM " + mute.target().getUser().getAsTag() + " for mute.")
                        .setColor(Color.CYAN)
                        .setFooter("Mute was requested by " + mute.requester().getAsTag(), mute.requester().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build();
            actionLog.sendMessageEmbeds(failedToDMEmbed).queue();
        });

        guild.addRoleToMember(mute.target(), mutedRole).queue();

        insertMuteToDb(mute);
        scheduleUnmute(mute);
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
     * Unmutes the user from the {@link MuteEntry} and logs it to the action log, and as an interaction response if interaction isn't {@code null}
     * @param mute The {@link MuteEntry} to get the data from. Ignores everything but target and requester
     * @param reason The reason of the unmute
     * @param interaction The interaction to notify about the unmute, or {@code null} if there's none
     */
    private static void unmute(MuteEntry mute, String reason, ComponentInteraction interaction) {
        Guild guild = LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID);
        TextChannel actionLog = LilyBot.INSTANCE.jda.getTextChannelById(Constants.ACTION_LOG);

        MessageEmbed unmuteEmbed = new EmbedBuilder()
                .setTitle("Unmute")
                .addField("Unmuted:", mute.target().getUser().getAsMention(), false)
                .addField("Reason:", reason, false)
                .setColor(Color.CYAN)
                .setFooter("Mute was originally requested by " + mute.requester().getAsTag(), mute.requester().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        guild.removeRoleFromMember(mute.target(), LilyBot.INSTANCE.jda.getRoleById(Constants.MUTED_ROLE)).queue(
                success -> {
                    if (interaction != null) {
                        interaction.editComponents().setEmbeds(unmuteEmbed).submit();
                    }
                    actionLog.sendMessageEmbeds(unmuteEmbed).queue();
                },
                error -> {
                    MessageEmbed errorEmbed = new EmbedBuilder()
                            .setTitle(String.format("Unable to unmute %s:", mute.target().getUser().getName()))
                            .appendDescription(error.toString())
                            .setColor(Color.RED)
                            .build();
                    actionLog.sendMessageEmbeds(errorEmbed).submit();
                    if (interaction != null) {
                        interaction.replyEmbeds(unmuteEmbed).mentionRepliedUser(false).setEphemeral(true).submit();
                    }
                    error.printStackTrace();
                }
        );
        SCHEDULED_UNMUTES.computeIfPresent(mute.target(), (member, task) -> {
            task.cancel();
            return null;
        });
        removeUserFromDb(mute.target().getId());
    }

    private static void insertMuteToDb(MuteEntry mute) {
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO mute(id, expiry, requester, reason) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, mute.target().getId());
            ps.setTimestamp(2, mute.expiry());
            ps.setString(3, mute.requester().getId());
            ps.setString(4, mute.reason());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void removeUserFromDb(String targetId) {
        try (Connection connection = SQLiteDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("DELETE FROM mute WHERE id = ?")) {
               ps.setString(1, targetId);
               ps.executeUpdate();
           } catch (SQLException e) {
               e.printStackTrace();
           }
    }

    private boolean equalsAny(String id, String memberId) {
        return id.equals("mute" + memberId + ":yes") ||
                id.equals("mute" + memberId + ":no");
    }

    /**
     * Re-schedules all saved mutes, and unmutes all expired mutes
     */
    public static void rescheduleMutes(JDA jda) {
        for (MuteEntry mute : getCurrentMutes(jda).values()) {
            if (mute.expiry().before(Date.from(Instant.now()))) {
                unmute(mute, "The duration of the mute was over while the bot wasn't running", null); // Already expired
            } else {
                scheduleUnmute(mute);
            }
        }
    }
    
    private static void scheduleUnmute(MuteEntry mute) {
        TimerTask unmuteTask = new TimerTask() {
            @Override
            public void run() {
                unmute(mute, "The duration of the mute is over", null);
            }
        };
        TIMER.schedule(unmuteTask, mute.expiry());
        SCHEDULED_UNMUTES.put(mute.target(), unmuteTask);
    }

    /**
     * Gets a {@link Map} mapping {@link Member} to their {@link MuteEntry}, for all active maps
     */
    public static Map<Member, MuteEntry> getCurrentMutes(JDA jda) {
        Guild guild = jda.getGuildById(Constants.GUILD_ID);
        Map<Member, MuteEntry> mutes = new HashMap<>();

        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM mute")) {
            ResultSet queryResult = ps.executeQuery();
            while (queryResult.next()) {
                Member target = guild.getMemberById(queryResult.getString("id"));
                User requester = jda.getUserById(queryResult.getString("requester"));
                Timestamp expiry = queryResult.getTimestamp("expiry");
                String reason = queryResult.getString("reason");
                mutes.put(target, new MuteEntry(target, requester, expiry, reason));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mutes;
    }

    /**
     * Stops the timer and all schedules mutes in order to be able to shutdown gracefully
     */
    public static void cancelTimers() {
	    TIMER.cancel();
	}

    /**
     *
     * @param target The target that was muted
     * @param requester The requester of the mute, that may come from a warn
     *
     */
    public static record MuteEntry(Member target, User requester, Timestamp expiry, String reason) {}

}
