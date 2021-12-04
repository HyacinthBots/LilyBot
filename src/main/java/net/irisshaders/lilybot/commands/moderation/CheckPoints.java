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

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class CheckPoints extends SlashCommand {
    public CheckPoints() {
        this.name = "check-points";
        this.help = "Checks how many points a member has";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{Constants.MODERATOR_ROLE, Constants.TRIAL_MODERATOR_ROLE};
        this.guildOnly = true;
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.USER, "member", "The member to warn.").setRequired(true));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        Member target = event.getOption("member").getAsMember();
        String targetId = target.getId();
        User user = event.getUser();
        InteractionHook hook = event.getHook();
        TextChannel channel = event.getTextChannel();

        event.deferReply(true).queue();

        // Insert target with default values if they are not in the DB, if they are already in the DB, do nothing
        insertUsers(targetId);
        // SELECT points from target
        readPoints(target, hook, user, channel);

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
     * A method for reading the current number of points in the database.
     * @param target The target. (Member)
     * @param hook How the message is followed up. (InteractionHook)
     * @param user The user of the command. (User)
     * @param actionLog Where the moderation messages are sent. (TextChannel)
     */
    private void readPoints(Member target, InteractionHook hook, User user, TextChannel actionLog) {
        int totalPoints;
        try (Connection connection = SQLiteDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT points FROM warn WHERE id = (?)")) {
            ps.setString(1, target.getId());
            ResultSet resultSet = ps.executeQuery();
            totalPoints = resultSet.getInt("points");
            MessageEmbed checkPointsEmbed = new EmbedBuilder()
                    .setTitle(target.getUser().getAsTag() + " has " + totalPoints + " points!")
                    .setColor(Color.CYAN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();
            hook.sendMessageEmbeds(checkPointsEmbed).mentionRepliedUser(false).queue();
            actionLog.sendMessageEmbeds(checkPointsEmbed).queue();
            target.getUser().openPrivateChannel()
                    .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(checkPointsEmbed))
                    .queue(null, throwable -> System.out.println());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
