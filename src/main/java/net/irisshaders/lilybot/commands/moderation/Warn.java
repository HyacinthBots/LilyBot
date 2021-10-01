package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ConstantConditions")
public class Warn extends SlashCommand {

    public Warn() {
        this.name = "warn";
        this.help = "Warns a member for any infractions.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE};
        this.guildOnly = true;
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to warn.").setRequired(true));
        optionData.add(new OptionData(OptionType.INTEGER, "points", "The number of points the user should be given.").setRequired(true));
        optionData.add(new OptionData(OptionType.STRING, "reason", "The reason for the warn").setRequired(false));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        Member target = event.getOption("member").getAsMember();
        String targetId = target.getId();
        String points = event.getOption("points").getAsString();
        String reason = event.getOption("reason") == null ? "No reason provided." : event.getOption("reason").getAsString();
        User user = event.getUser();
        InteractionHook hook = event.getHook();
        Guild guild = event.getGuild();
        Role mutedRole = guild.getRoleById(Constants.MUTED_ROLE);
        TextChannel actionLog = guild.getTextChannelById(Constants.ACTION_LOG);

        event.deferReply(true).queue();

        // Insert target with default values if they are not in the DB, if they are already in the DB, do nothing
        insertUsers(targetId);
        // UPDATE Target's points with the given points
        updateUsers(points, targetId);
        // SELECT points from target, executes upon reaching a threshold
        readPoints(target, points, hook, reason, user, actionLog, guild, mutedRole);

    }

    /**
     * A method for inserting the users into the database. If they are already in the database, they are ignored.
     * @param targetId The id of the User to give the points to. (String)
     */
    private void insertUsers(String targetId) {
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO warn(id, points) VALUES (?, ?)")) {
            ps.setString(1, targetId);
            ps.setInt(2, 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method for updating the users in the database.
     * @param points The number of points to give. (String)
     * @param targetId The id of the User to give the points to. (String)
     */
    private void updateUsers(String points, String targetId) {
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE warn SET points = points + (?) WHERE id = (?)")) {
            ps.setString(1, points);
            ps.setString(2, targetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method for reading the current number of points in the database.
     * @param target The target. (Member)
     * @param points The number of points to award. (String)
     * @param hook How the message is followed up. (InteractionHook)
     * @param reason The reason for the points to be given. (String)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     * @param guild The guild where this took place. (Guild)
     * @param mutedRole The role to give for a mute. (Role)
     */
    private void readPoints(Member target, String points, InteractionHook hook, String reason, User user, TextChannel actionLog, Guild guild, Role mutedRole) {
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT points FROM warn WHERE id = (?)")) {
            ps.setString(1, target.getId());
            ResultSet resultSet = ps.executeQuery();
            int totalPoints = resultSet.getInt("points");
            MessageEmbed warnEmbed = new EmbedBuilder()
                    .setTitle(target.getUser().getAsTag() + " was given " + points + " points!")
                    .setColor(Color.CYAN)
                    .addField("Total Points:", String.valueOf(totalPoints), false)
                    .addField("Points added:", points, false)
                    .addField("Reason:", reason, false)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();
            hook.sendMessageEmbeds(warnEmbed).mentionRepliedUser(false).queue();
            actionLog.sendMessageEmbeds(warnEmbed).queue();
            target.getUser().openPrivateChannel()
                    .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(warnEmbed))
                    .queue(null, throwable -> System.out.println());
            consequences(target, reason, user, actionLog, guild, mutedRole, totalPoints);
            ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method for handling the consequences of earning points.
     * @param target The target. (Member)
     * @param reason The reason for the mute / ban. (String)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     * @param guild The guild where this took place. (Guild)
     * @param mutedRole The role to give for a mute. (Guild)
     * @param totalPoints The total number of points in the database. (int)
     */
    private void consequences(Member target, String reason, User user, TextChannel actionLog, Guild guild, Role mutedRole, int totalPoints) {
        if (totalPoints >= 50 && totalPoints < 50) { // 1 hr mute
            mute(guild, target, mutedRole, "1h", user, actionLog);
        } else if (totalPoints >= 50 && totalPoints < 100) { // 3 hr mute
            mute(guild, target, mutedRole, "3h", user, actionLog);
        } else if (totalPoints >= 100 && totalPoints < 150) { // 12 hr mute
            mute(guild, target, mutedRole, "12h", user, actionLog);
        } else if (totalPoints >= 150) { // ban
            ban(target, user, actionLog, reason);
        }
    }

    /**
     * A method for giving out mutes.
     * @param guild The guild where this took place. (Guild)
     * @param target The target. (Member)
     * @param mutedRole The role to give for a mute. (Role)
     * @param duration The length of the mute. (String)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     */
    private void mute(Guild guild, Member target, Role mutedRole, String duration, User user, TextChannel actionLog) {
        target.getUser().openPrivateChannel()
            .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("You were muted for " + duration + " as a result of being warned.")
                .setColor(Color.CYAN)
                .setFooter("Warn was issued by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build()))
                .queue(null, throwable -> {
                    actionLog.sendMessageEmbeds(ResponseHelper.genFailureEmbed(user, "Failed to DM " + target.getUser().getAsTag() + " for mute.", null)).queue();
                });
        actionLog.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(target.getUser().getAsTag() + " was muted for " + duration + " as a result of being warned.")
                .setColor(Color.CYAN)
                .setFooter("Warn was issued by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build())
                .queue();
        guild.addRoleToMember(target, mutedRole).queue();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                guild.removeRoleFromMember(target, mutedRole).queue();
            }
        }, Mute.parseDuration(duration));
    }

    /**
     * A method for banning.
     * @param target The target. (Member)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     * @param reason The reason for the ban. (String)
     */
    private void ban(Member target, User user, TextChannel actionLog, String reason) {
        MessageEmbed banEmbed = new EmbedBuilder()
                .setTitle("Banned a member.")
                .setColor(Color.CYAN)
                .addField("Banned:", target.getUser().getAsTag(), false)
                .addField("Reason:", reason, false)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
        actionLog.sendMessageEmbeds(banEmbed).queue();
        target.ban(7, reason).queue(null, throwable -> {
            actionLog.sendMessageEmbeds(ResponseHelper.genFailureEmbed(user, "Failed to DM " + target.getUser().getAsTag() + " for ban.", null)).queue();
        });
    }

}
