package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.commands.application.message.EphemeralMessageCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.events.EventContext
import com.kotlindiscord.kord.extensions.events.EventHandler
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.gateway.Event
import kotlinx.coroutines.flow.toList

suspend fun EphemeralSlashCommandContext<*>.getFromConfig(inputColumn: String) =
	DatabaseHelper.selectInConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Is your configuration set?"
		}
		null
}

suspend fun EphemeralSlashCommandContext<*>.getFromConfigPublicResponse(inputColumn: String) =
	DatabaseHelper.selectInConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Please inform a member of staff!"
		}
		null
}

suspend fun EphemeralMessageCommandContext.getFromConfig(inputColumn: String) =
	DatabaseHelper.selectInConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Please inform a member of staff!"
		}
		null
}



