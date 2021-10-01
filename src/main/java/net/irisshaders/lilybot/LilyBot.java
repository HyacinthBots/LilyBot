package net.irisshaders.lilybot;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.CommandType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.irisshaders.lilybot.commands.Ping;
import net.irisshaders.lilybot.commands.moderation.Shutdown;
import net.irisshaders.lilybot.commands.moderation.*;
import net.irisshaders.lilybot.commands.support.*;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.events.ReadyHandler;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.Memory;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;

@SuppressWarnings("ConstantConditions")
public class LilyBot {

    public static void main(String[] args) {

        JDA jda = null;
        try {
            jda = JDABuilder.createDefault(Constants.TOKEN)
                    .setEnabledIntents(
                            GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS,
                            GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_WEBHOOKS,
                            GatewayIntent.GUILD_INVITES, GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING,
                            GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_TYPING,
                            GatewayIntent.DIRECT_MESSAGE_REACTIONS
                    )
                    .setRawEventsEnabled(true)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setActivity(Activity.watching("Loading..."))
                    .setAutoReconnect(true)
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }

        EventWaiter waiter = new EventWaiter();
        CommandClient builder = new CommandClientBuilder()
                .setPrefix("!")
                .setHelpConsumer(null)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Iris 1.17.1"))
                .setOwnerId(Constants.OWNER)
                .forceGuildOnly(Constants.GUILD_ID)
                .build();

        jda.addEventListener(builder, waiter);

        jda.addEventListener(new ReadyHandler());
        jda.addEventListener(new Report()); // TODO uncomment when threads are finished

        try {
            SQLiteDataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        GitHub github = null;
        try {
            github = new GitHubBuilder().withOAuthToken(Constants.GITHUB_OAUTH).build();
            LoggerFactory.getLogger("GitHub").info("Logged into GitHub!");
        } catch (Exception exception) {
            exception.printStackTrace();
            LoggerFactory.getLogger("GitHub").error("Failed to log into GitHub!");
            jda.shutdownNow();
        }

        Memory.remember(github, waiter);

        // add commands now
        builder.addSlashCommand(new Ping());

        // Mod Commands
        builder.addSlashCommand(new Ban());
        builder.addSlashCommand(new Kick());
        builder.addSlashCommand(new Unban());
        builder.addSlashCommand(new Clear());
        builder.addSlashCommand(new Mute());
        builder.addSlashCommand(new MuteList());
        builder.addSlashCommand(new Warn());

        // Shutdown Command
        builder.addSlashCommand(new Shutdown());

        // Support Commands
        builder.addSlashCommand(new Sodium());
        builder.addSlashCommand(new Config());
        builder.addSlashCommand(new Logs());
        builder.addSlashCommand(new CrashReport());
        builder.addSlashCommand(new Starline());
        builder.addSlashCommand(new Indium());
        builder.addSlashCommand(new ETA());
        builder.addSlashCommand(new Rule());

        // Services
        builder.addSlashCommand(new net.irisshaders.lilybot.commands.services.GitHub());

        // normal commands
        // builder.addCommand(new Report()); // TODO uncomment when threads are finished

        jda.getGuildById(Constants.GUILD_ID).upsertCommand(new CommandData(CommandType.MESSAGE_CONTEXT, "Report message")).queue();

    }

}
