package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.commands.application.message.EphemeralMessageCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.types.respond

/**
 * This is a simple function to get a value from the configuration database in an ephemeral slash command context,
 * null check it, and respond accordingly. It takes a string input of the column that is requested and gets it, if it's
 * null a private error message (i.e. for commands staff run), and null is returned. If null this should be handled with
 * an elvis operator to return the action.
 *
 * @param inputColumn The column you wish to get from the database
 * @return The column value from the database or null
 * @author NoComment1105
 */
suspend fun EphemeralSlashCommandContext<*>.getFromConfigPrivateResponse(inputColumn: String) =
	DatabaseHelper.selectInConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Is your configuration set?"
		}
		null
}


/**
 * This is a simple function to get a value from the configuration database in an ephemeral slash command context,
 * null check it, and respond accordingly. It takes a string input of the column that is requested and gets it, if it's
 * null a public error message (i.e. for commands the people run), and null is returned. If null this should be handled
 * with an elvis operator to return the action.
 *
 * @param inputColumn The column you wish to get from the database
 * @return The column value from the database or a public error and null
 * @author NoComment1105
 */
suspend fun EphemeralSlashCommandContext<*>.getFromConfigPublicResponse(inputColumn: String) =
	DatabaseHelper.selectInConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Please inform a member of staff!"
		}
		null
}

/**
 * This is a simple function to get a value from the configuration database in an ephemeral message command context,
 * null check it, and respond accordingly. It takes a string input of the column that is requested and gets it, if it's
 * null a private error message (i.e. for commands staff run), and null is returned. If null this should be handled with
 * an elvis operator to return the action.
 *
 * @param inputColumn The column you wish to get from the database
 * @return The column value from the database or a private error and null
 * @author NoComment1105
 */
suspend fun EphemeralMessageCommandContext.getFromConfigPublicResponse(inputColumn: String) =
	DatabaseHelper.selectInConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Please inform a member of staff!"
		}
		null
}
