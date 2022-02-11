package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake

// Allows for easier imports of the secret magic needed
val BOT_TOKEN = env("TOKEN")
val GITHUB_OAUTH = env("GITHUB_OAUTH")
val SENTRY_DSN = envOrNull("SENTRY_DSN")

val GUILD_ID = Snowflake(env("GUILD_ID"))
val MODERATORS = Snowflake(env("MODERATOR_PING_ROLE"))
val SUPPORT_TEAM = Snowflake(env("SUPPORT_ROLE"))
val MOD_ACTION_LOG = Snowflake(env("MOD_ACTION_LOG"))
val MESSAGE_LOGS = Snowflake(env("MESSAGE_LOGS"))
val SUPPORT_CHANNEL = Snowflake(env("SUPPORT_CHANNEL"))
val JOIN_CHANNEL = Snowflake(env("JOIN_CHANNEL"))

const val CUSTOM_COMMANDS_PATH = "commands.config.toml"
const val JDBC_URL = "jdbc:sqlite:database.db"
