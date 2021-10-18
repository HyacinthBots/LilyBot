package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake

// Allows for easier imports of the secret magic needed
val BOT_TOKEN = Snowflake(env("TOKEN"))
val GUILD_ID = Snowflake(env("GUILD_ID"))
val MODERATORS = Snowflake(env("MODERATOR_ROLE"))
val MUTED_ROLE = Snowflake(env("MUTED_ROLE"))
val ACTION_LOG = Snowflake(env("ACTION_LOG"))
val OWNER_ID = Snowflake(env("OWNER"))
val GITHUB_OAUTH = Snowflake(env("GITHUB_OAUTH"))
val CONFIG_PATH = Snowflake(env("CONFIG_PATH"))

