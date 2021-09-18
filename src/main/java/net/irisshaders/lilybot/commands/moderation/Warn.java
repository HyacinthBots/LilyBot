package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.ResponseHelper;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
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

        Member target = event.getOption("member").getAsMember();
        String targetId = target.getId();
        String points = event.getOption("points").getAsString();
        String reason = event.getOption("reason") == null ? "No reason provided." : event.getOption("reason").getAsString();
        User user = event.getUser();
        InteractionHook hook = event.getHook();
        Guild guild = event.getGuild();
        Role mutedRole = guild.getRoleById(Constants.MUTED_ROLE);
        TextChannel actionLog = guild.getTextChannelById(Constants.ACTION_LOG);

        event.deferReply(true).queue(); // deferred because it may take more than 3 seconds for the SQL below

        try {
            updatePoints(points, targetId);
        } catch (SQLException e) {
            e.printStackTrace();
            event.replyEmbeds(ResponseHelper.genFailureEmbed(user, "Failed to warn " + target.getUser().getAsTag() + " with " + points + " points",
                    "Stacktrace: " + Arrays.toString(e.getStackTrace()))).queue();
        }

        try {
            readPoints(targetId, target, points, reason, hook, actionLog, guild);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            consequences(target, targetId, reason, user, guild, mutedRole, actionLog);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void consequences(Member target, String targetId, String reason, User user, Guild guild, Role mutedRole, TextChannel actionLog) throws SQLException {
        @Language("SQL")
        String queryString = "SELECT points FROM warn WHERE id = (?)";
        PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(queryString);
        statement.setString(1, targetId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            int currentPoints = resultSet.getInt("points");
            if (currentPoints >= 25 && currentPoints < 50) { // 1 hr mute
                mute(guild, target, mutedRole, "1h", user, actionLog);
            } else if (currentPoints >= 50 && currentPoints < 100) { // 3 hr mute
                mute(guild, target, mutedRole, "3h", user, actionLog);
            } else if (currentPoints >= 100 && currentPoints < 150) { // 12 hr mute
                mute(guild, target, mutedRole, "12h", user, actionLog);
            } else if (currentPoints >= 150) { // ban
                ban(target, user, actionLog, reason);
            }
        }
        statement.closeOnCompletion();
    }

    private void updatePoints(String points, String targetId) throws SQLException {
        @Language("SQL")
        String updateString = "UPDATE warn SET points = points + (?) WHERE id IS (?)";
        PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(updateString);
        statement.setString(1, points);
        statement.setString(2, targetId);
        statement.execute();
        statement.closeOnCompletion();
    }

    private void readPoints(String targetId, Member target, String points, String reason, InteractionHook hook, TextChannel actionLog, Guild guild) throws SQLException {
        @Language("SQL")
        String queryString = "SELECT points FROM warn WHERE id = (?)";
        PreparedStatement statement = SQLiteDataSource.getConnection().prepareStatement(queryString);
        statement.setString(1, targetId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            String currentPoints = resultSet.getString("points");
            MessageEmbed warnEmbed = new EmbedBuilder()
                    .setTitle("Warned " + target.getUser().getAsTag() + " with " + points + " points!")
                    .setColor(Color.CYAN)
                    .addField("Total Points:", currentPoints, false)
                    .addField("Points added:", points, false)
                    .addField("Reason:", reason, false)
                    .setTimestamp(Instant.now())
                    .build();
            hook.sendMessageEmbeds(warnEmbed).queue();
            actionLog.sendMessageEmbeds(warnEmbed).queue();
            if (guild.getMembers().contains(target))
                target.getUser().openPrivateChannel()
                    .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(warnEmbed))
                    .queue(null, throwable -> {
                        System.out.println(); // does nothing
                    });
        }
        resultSet.close();
        statement.closeOnCompletion();
    }

    private void mute(Guild guild, Member target, Role mutedRole, String duration, User user, TextChannel actionLog) {
        target.getUser().openPrivateChannel()
            .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("You were muted for " + duration + " as a result of being warned.")
                .setColor(Color.CYAN)
                .setFooter("Warn was issued by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build()))
                .queue(null, throwable -> {
                    actionLog.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Failed to DM " + target.getUser().getAsTag() + " for mute.")
                            .setColor(Color.CYAN)
                            .setFooter("Mute was originally requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build())
                            .queue();
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
            actionLog.sendMessageEmbeds(new EmbedBuilder().setTitle("Failed to DM " + target.getUser().getAsTag() + " for ban.")
                    .setColor(Color.CYAN)
                    .setFooter("Ban was originally requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build())
                    .queue();
        });
    }

}
