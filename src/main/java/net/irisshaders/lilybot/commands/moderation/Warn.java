package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.commands.moderation.Timeout.TimeOutEntry;
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

@SuppressWarnings("ConstantConditions")
public class Warn extends SlashCommand {

    public Warn() {
        this.name = "warn";
        this.help = "Warns a member for any infractions.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE, Constants.TRIAL_MODERATOR_ROLE};
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
        TextChannel actionLog = guild.getTextChannelById(Constants.ACTION_LOG);
        JDA jda = event.getJDA();

        event.deferReply(true).queue();

        // Insert target with default values if they are not in the DB, if they are already in the DB, do nothing
        insertUsers(targetId);
        // UPDATE Target's points with the given points
        updateUsers(points, targetId);
        // SELECT points from target, executes upon reaching a threshold
        readPoints(target, points, hook, reason, user, actionLog, jda);

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
     * @param jda The JDA instance. (JDA)
     */
    private void readPoints(Member target, String points, InteractionHook hook, String reason, User user, TextChannel actionLog, JDA jda) {
        int totalPoints = 0;
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT points FROM warn WHERE id = (?)")) {
            ps.setString(1, target.getId());
            ResultSet resultSet = ps.executeQuery();
            totalPoints = resultSet.getInt("points");
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        consequences(target, reason, user, actionLog, totalPoints, jda);
    }

    /**
     * A method for handling the consequences of earning points.
     * @param target The target. (Member)
     * @param reason The reason for the mute / ban. (String)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     * @param totalPoints The total number of points in the database. (int)
     * @param jda The JDA instance. (JDA)
     */
    private void consequences(Member target, String reason, User user, TextChannel actionLog, int totalPoints, JDA jda) {
        if (totalPoints >= 50 && totalPoints < 100) { // 3 hr mute
            mute(target, "3h", user, jda);
        } else if (totalPoints >= 100 && totalPoints < 150) { // 12 hr mute
            mute(target, "12h", user, jda);
        } else if (totalPoints >= 150) { // ban
            ban(target, user, actionLog, reason);
        }
    }

    /**
     * A utility method for giving mutes from here. Basically calls {@link Timeout#timeout(TimeOutEntry, Interaction)}.<p>
     * It also checks if the {@link Member} is muted before muting
     * @param target The target. (Member)
     * @param duration The length of the mute. (String)
     * @param requester The user of the command. (User)
     */
    private void mute(Member target, String duration, User requester, JDA jda) {
        if (target.isTimedOut()) { // Currently muted
            return; // Do nothing. The warn was already notified, and there just was no mute since the user is already muted
        }
        Instant expiry = Instant.now().plusSeconds(Timeout.parseDuration(duration));
        TimeOutEntry mute = new TimeOutEntry(target, requester, expiry, "Being warned");
        Timeout.timeout(mute, null);
    }

    /**
     * A method for banning.
     * @param target The target. (Member)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     * @param reason The reason for the ban. (String)
     */
    private void ban(Member target, User user, TextChannel actionLog, String reason) {
        MessageEmbed banEmbed = ResponseHelper.responseEmbed("Banned a member", user, Color.CYAN)
                .addField("Banned:", target.getUser().getAsTag(), false)
                .addField("Reason:", reason, false)
                .build();
        actionLog.sendMessageEmbeds(banEmbed).queue();
        target.ban(7, reason).queue(null, throwable -> actionLog.sendMessageEmbeds(
                ResponseHelper.genFailureEmbed(user, "Failed to DM " + target.getUser().getAsTag() + " for ban.",
                        null)).queue())
        ;
    }

}
