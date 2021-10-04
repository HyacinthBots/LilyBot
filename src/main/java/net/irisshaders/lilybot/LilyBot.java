package net.irisshaders.lilybot;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.irisshaders.lilybot.commands.Ping;
import net.irisshaders.lilybot.commands.custom.Custom;
import net.irisshaders.lilybot.commands.moderation.Shutdown;
import net.irisshaders.lilybot.commands.moderation.*;
import net.irisshaders.lilybot.database.SQLiteDataSource;
import net.irisshaders.lilybot.events.ReadyHandler;
import net.irisshaders.lilybot.utils.Constants;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

@SuppressWarnings("ConstantConditions")
public class LilyBot {

    public static LilyBot INSTANCE;

    public static final Logger LOG_LILY = LoggerFactory.getLogger("Lily");
    public static final Logger LOG_GITHUB = LoggerFactory.getLogger("GitHub");

    public final JDA jda;
    public final GitHub gitHub;
    public final EventWaiter waiter;
    public final Properties config;
    public final Path configPath;

    public LilyBot(Path configPath) {
        JDA jda = null;
        this.configPath = configPath;
        var properties = new Properties();
        try {
            properties.load(Files.newInputStream(configPath));
        } catch (IOException e) {
            LOG_LILY.error("Error loading lily config file at "+configPath, e);
        }
        this.config = properties;
        
        EventWaiter waiter = new EventWaiter();
        CommandClient builder = new CommandClientBuilder()
                .setPrefix(String.format("<@!%s> ", jda.getSelfUser().getId()))
                .setHelpConsumer(null)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Iris 1.17.1"))
                .setOwnerId(Constants.OWNER)
                .forceGuildOnly(Constants.GUILD_ID)
                .build();

        LilyBot.addBuiltinCommands(builder);
        this.addCustomCommands(builder);

        try {
            SQLiteDataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
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
                    .addEventListeners(builder, waiter, new ReadyHandler())
                    .build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        // jda.addEventListener(new Report()); // TODO uncomment when threads are finished but in the builder if possible


        GitHub github = null;
        try {
            github = new GitHubBuilder().withOAuthToken(Constants.GITHUB_OAUTH).build();
            LOG_GITHUB.info("Logged into GitHub!");
        } catch (Exception exception) {
            exception.printStackTrace();
            LOG_GITHUB.error("Failed to log into GitHub!");
            jda.shutdownNow();
        }
        
        this.jda = jda;
        this.gitHub = github;
        this.waiter = waiter;
    }

    public static void main(String[] args) {
        INSTANCE = new LilyBot(Paths.get(Constants.CONFIG_PATH));
    }

    public static void addBuiltinCommands(CommandClient commands) {
        // add commands now
        commands.addSlashCommand(new Ping());

        // Mod Commands
        commands.addSlashCommand(new Ban());
        commands.addSlashCommand(new Kick());
        commands.addSlashCommand(new Unban());
        commands.addSlashCommand(new Clear());
        commands.addSlashCommand(new Mute());
        commands.addSlashCommand(new MuteList());
        commands.addSlashCommand(new Warn());
        commands.addSlashCommand(new Say());

        // Services
        commands.addSlashCommand(new net.irisshaders.lilybot.commands.services.GitHub());

        // normal commands
        // builder.addCommand(new Report()); // TODO uncomment when threads are finished

        // Shutdown Command
        commands.addSlashCommand(new Shutdown());
    }

    public void addCustomCommands(CommandClient commands) {
        var cmdNames = this.config.getProperty("commands").split(" ");
        for (var cmd : cmdNames) {
            LOG_LILY.info("Adding custom command '{}'", cmd);
            commands.addSlashCommand(Custom.parse(cmd, this.config));
        }
    }

}
