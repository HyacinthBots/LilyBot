package net.irisshaders.lilybot

import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake


val GUILD_ID: Snowflake
    get() = Snowflake(  // Store this as a Discord snowflake, aka an ID
            env("GUILD_ID").toLong()  // An exception will be thrown if it can't be found
    )
val TOKEN = env("TOKEN")
val MODERATOR_ROLE = env("MODERATOR_ROLE")
