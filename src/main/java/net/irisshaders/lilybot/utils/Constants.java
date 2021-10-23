package net.irisshaders.lilybot.utils;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * A convenience Interface to get Dotenv entries.
 */
public interface Constants {

    Dotenv dotenv = Dotenv.load();

    String MODERATOR_ROLE = dotenv.get("MODERATOR_ROLE");
    String TRIAL_MODERATOR_ROLE = dotenv.get("TRIAL_MODERATOR_ROLE");
    String MUTED_ROLE = dotenv.get("MUTED_ROLE");
    String GUILD_ID = dotenv.get("GUILD_ID");
    String ACTION_LOG = dotenv.get("ACTION_LOG");
    String OWNER = dotenv.get("OWNER");
    String TOKEN = dotenv.get("TOKEN");
    String GITHUB_OAUTH = dotenv.get("GITHUB_OAUTH");
    String CONFIG_PATH = dotenv.get("CONFIG_PATH");

}
