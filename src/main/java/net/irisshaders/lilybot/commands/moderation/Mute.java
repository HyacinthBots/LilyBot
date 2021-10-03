package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.utils.Constants;

import java.awt.*;
import java.sql.Connection;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ConstantConditions")
public class Mute extends SlashCommand {

    public Mute() {
        this.name = "mute";
        this.help = "Mutes a specified member for the given reason and duration. Defaults to 6h and no reason.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
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

        TextChannel actionLog = event.getGuild().getTextChannelById(Constants.ACTION_LOG);
        Member target = event.getOption("member").getAsMember();
        User user = event.getUser();
        JDA jda = event.getJDA();
        Guild guild = jda.getGuildById(Constants.GUILD_ID);
        String reason = event.getOption("reason") == null ? "No reason provided" : event.getOption("reason").getAsString();
        String duration = event.getOption("duration") == null ? "6h" : event.getOption("duration").getAsString();
        Role mutedRole = guild.getRoleById(Constants.MUTED_ROLE);

        if (!target.getRoles().contains(mutedRole)) {

            MessageEmbed userEmbed = new EmbedBuilder()
                    .setTitle("You were muted")
                    .addField(String.format("You are muted from %s for:", guild.getName()), duration, false)
                    .addField("Reason:", reason, false)
                    .setColor(Color.CYAN)
                    .setFooter("Muted by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            MessageEmbed muteEmbed = new EmbedBuilder()
                    .setTitle("Mute")
                    .addField("Muted:", target.getUser().getAsMention(), false)
                    .addField("Muted for:", duration, false)
                    .addField("Reason:", reason, false)
                    .setColor(Color.CYAN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            int durationInt = parseDuration(duration);

            // Send the embed as Interaction reply, in action log and to the user
            event.replyEmbeds(muteEmbed).mentionRepliedUser(false).setEphemeral(true).submit();
            actionLog.sendMessageEmbeds(muteEmbed).queue();
            target.getUser().openPrivateChannel()
                    .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(userEmbed))
                    .queue(null, throwable -> {
                        MessageEmbed failedToDMEmbed = new EmbedBuilder()
                                .setTitle("Failed to DM " + target.getUser().getAsTag() + " for mute.")
                                .setColor(Color.CYAN)
                                .setFooter("Mute was originally requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .build();
                        actionLog.sendMessageEmbeds(failedToDMEmbed).queue();
                    });

            guild.addRoleToMember(target, mutedRole).queue();
            Timestamp expiry = Timestamp.from(Instant.now().plusSeconds(durationInt));
            MuteEntry mute = new MuteEntry(target, user, expiry);
            insertMuteToDb(mute);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    unmute(mute);
                }
            }, expiry);

        } else if (target.getRoles().contains(mutedRole)) {

            MessageEmbed alreadyMutedEmbed = new EmbedBuilder()
                    .setTitle("Already muted")
                    .setDescription("Do you want to unmute? Respond with the buttons below.")
                    .addField("The following member is already muted:", target.getUser().getAsTag(), false)
                    .setColor(Color.CYAN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(alreadyMutedEmbed).addActionRow(
                    Button.of(ButtonStyle.PRIMARY, "mute:yes", "Yes", Emoji.fromUnicode("\u2705")),
                    Button.of(ButtonStyle.PRIMARY, "mute:no", "No", Emoji.fromUnicode("\u274C"))
            ).mentionRepliedUser(false).setEphemeral(true).queue(interactionHook -> LilyBot.INSTANCE.waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
                if (!buttonClickEvent.getUser().equals(user)) return false;
                if (!equalsAny(buttonClickEvent.getButton().getId())) return false;
                return !buttonClickEvent.isAcknowledged();
            }, buttonClickEvent -> {

                User buttonClickEventUser = buttonClickEvent.getUser();
                String id = buttonClickEvent.getButton().getId().split(":")[1];

                switch (id) {

                    case "yes" -> {

                        MessageEmbed unmuteEmbed = new EmbedBuilder()
                                .setTitle("Unmute")
                                .addField("Unmuted:", target.getUser().getAsMention(), false)
                                .addField("Reason:", "Manual Unmuted by " + buttonClickEventUser.getAsTag(), false)
                                .setColor(Color.CYAN)
                                .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .build();

                        buttonClickEvent.replyEmbeds(unmuteEmbed).mentionRepliedUser(false).setEphemeral(true).submit();
                        actionLog.sendMessageEmbeds(unmuteEmbed).queue();

                        guild.removeRoleFromMember(target, mutedRole).queue();

                    }
                    case "no" -> {

                        MessageEmbed stillMutedEmbed = new EmbedBuilder()
                                .setTitle(String.format("%s is still muted", target.getUser().getAsTag()))
                                .setColor(Color.CYAN)
                                .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .build();

                        buttonClickEvent.replyEmbeds(stillMutedEmbed).mentionRepliedUser(false).setEphemeral(true).submit();

                    }

                }

            }));

        }

    }

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

    private static void unmute(MuteEntry mute) {
        Guild guild = LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID);
        TextChannel actionLog = LilyBot.INSTANCE.jda.getTextChannelById(Constants.ACTION_LOG);
        
        MessageEmbed unmuteEmbed = new EmbedBuilder()
                .setTitle("Unmute")
                .addField("Unmuted:", mute.target().getUser().getAsMention(), false)
                .addField("Reason:", "The duration of the mute is over.", false)
                .setColor(Color.CYAN)
                .setFooter("Mute was originally requested by " + mute.requester().getAsTag(), mute.requester().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
        
        guild.removeRoleFromMember(mute.target(), LilyBot.INSTANCE.jda.getRoleById(Constants.MUTED_ROLE)).queue(
                success -> actionLog.sendMessageEmbeds(unmuteEmbed).queue(),
                error -> {
                    actionLog.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle(String.format("Unable to unmute %s:", mute.target().getUser().getName()))
                            .appendDescription(error.toString())
                            .setColor(Color.RED)
                            .build()).queue();
                    error.printStackTrace();
                }
        );
        removeUserFromDb(mute.target().getId());
    }

    private void insertMuteToDb(MuteEntry mute) {
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO mute(id, expiry, requester) VALUES (?, ?, ?)")) {
            ps.setString(1, mute.target().getId());
            ps.setTimestamp(2, mute.expiry());
            ps.setString(3, mute.requester().getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void removeUserFromDb(String targetId) {
        try (Connection connection = SQLiteDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("DELETE FROM mute WHERE id=?")) {
               ps.setString(1, targetId);
               ps.executeUpdate();
           } catch (SQLException e) {
               e.printStackTrace();
           }
    }

    private boolean equalsAny(String id) {
        return id.equals("mute:yes") ||
                id.equals("mute:no");
    }

    /**
     * Re-schedules all saved mutes, and unmutes all expired mutes
     */
    public static void rescheculeMutes() {
        for (MuteEntry mute : getMutes()) {
            if (mute.expiry().before(Date.from(Instant.now()))) {
                unmute(mute); // Already expired
            } else {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        unmute(mute);
                    }
                }, mute.expiry());
            }
        }
    }

    /**
     * Gets a list of registered mutes, as a {@link List} of {@link MuteEntry}
     */
    public static List<MuteEntry> getMutes() {
        Guild guild = LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID);
        List<MuteEntry> mutes = new ArrayList<>();
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM mute")) {
            ResultSet queryResult = ps.executeQuery();
            while (queryResult.next()) {
                Member target = guild.getMemberById(queryResult.getString("id"));
                User requester = LilyBot.INSTANCE.jda.getUserById(queryResult.getString("requester"));
                Timestamp expiry = queryResult.getTimestamp("expiry");
                mutes.add(new MuteEntry(target, requester, expiry));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mutes;
    }
    
    private static record MuteEntry(Member target, User requester, Timestamp expiry) {}

}
