package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.ResponseHelper;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        User target = event.getOption("member").getAsUser();
        String targetId = target.getId();
        String points = event.getOption("points").getAsString();
        String reason = event.getOption("reason") == null ? "No reason provided." : event.getOption("reason").getAsString();
        User user = event.getUser();
        Guild guild = event.getGuild();
        InteractionHook hook = event.getHook();

        event.deferReply().queue(); // deferred because it may take more than 3 seconds for the SQL below

        try {
            insertUsers(guild);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            updatePoints(points, targetId);
        } catch (SQLException e) {
            e.printStackTrace();
            event.replyEmbeds(ResponseHelper.genFailureEmbed(user, "Failed to warn " + target.getAsTag() + " with " + points + " points",
                    "Stacktrace: " + Arrays.toString(e.getStackTrace()))).queue();
        }

        try {
            readPoints(targetId, target, points, reason, hook);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void insertUsers(Guild guild) throws SQLException {
        String insertString = "INSERT OR IGNORE INTO [warn](id, points) VALUES (?, ?)";
        List<Member> members = guild.getMembers();
        PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(insertString);
        {
            LoggerFactory.getLogger(Warn.class).info("Writing all guild members to database!");
            for (Member member : members) {
                String memberId = member.getId();
                statement.setString(1, memberId);
                statement.setString(2, "0");
                statement.execute();
                statement.closeOnCompletion();
            }
            statement.closeOnCompletion();
        }

    }

    public void updatePoints(String points, String targetId) throws SQLException {
        String updateString = "UPDATE warn SET points = points + (?) WHERE id IS (?)";
        PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(updateString);
        {
            statement.setString(1, points);
            statement.setString(2, targetId);
            statement.execute();
            statement.closeOnCompletion();
            statement.closeOnCompletion();
        }
    }

    public void readPoints(String targetId, User target, String points, String reason, InteractionHook hook) throws SQLException {
        String selectString = "SELECT points FROM warn WHERE id = (?)";
        PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(selectString);
        {
            statement.setString(1, targetId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String currentPoints = resultSet.getString("points");
                MessageEmbed warnEmbed = new EmbedBuilder()
                        .setTitle("Warned " + target.getAsTag() + " with " + points + " points!")
                        .setColor(Color.CYAN)
                        .addField("Total Points:", currentPoints, false)
                        .addField("Points added:", points, false)
                        .addField("Reason:", reason, false)
                        .setTimestamp(Instant.now())
                        .build();
                hook.sendMessageEmbeds(warnEmbed).queue();
            }
            resultSet.close();
            statement.closeOnCompletion();
        }
    }
}
