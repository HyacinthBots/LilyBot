package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake

/** The Bot Token. */
val BOT_TOKEN = env("TOKEN")

/** The ID of the guild used for testing features. */
val TEST_GUILD_ID = Snowflake(env("TEST_GUILD_ID"))

/** The channel in the [TEST_GUILD_ID] where online notifications will be posted. */
val ONLINE_STATUS_CHANNEL = Snowflake(env("ONLINE_STATUS_CHANNEL"))

/** The string for connection to the database, defaults to local host. */
val MONGO_URI = envOrNull("MONGO_URI") ?: "mongodb://localhost:27017"

/** Sentry connection DSN. If null, Sentry won't be used. */
val SENTRY_DSN = envOrNull("SENTRY_DSN")

/** The environment the bot is being run in. production or development. */
val ENVIRONMENT = env("ENVIRONMENT")
