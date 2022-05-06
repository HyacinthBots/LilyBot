package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake

// Allows for easier imports of the secret magic needed
val BOT_TOKEN = env("TOKEN")
val TEST_GUILD_ID = Snowflake(env("TEST_GUILD_ID"))
val ONLINE_STATUS_CHANNEL = Snowflake(env("ONLINE_STATUS_CHANNEL"))
val MONGO_URI = envOrNull("MONGO_URI") ?: "mongodb://localhost:27017"
val SENTRY_DSN = envOrNull("SENTRY_DSN")
