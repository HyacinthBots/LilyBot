package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseHelper {

	/**
	 * Using the provided [guildId] and [column] a [value] or [error] will be returned from the
	 * config database
	 *
	 * @param guildId The ID of the guild the command was run in
	 * @param column The column you want the information form
	 * @return a [NoSuchElementException] or the result from the Database
	 * @author NoComment1105
	 */
	@Suppress("DuplicatedCode")
	@JvmName("selectInConfig!!")
	suspend fun selectInConfig(guildId: Snowflake, column: Column<String>): String? {
		var value: String? = null
		var error: String? = null
		newSuspendedTransaction {
			try {
				value = DatabaseManager.Config.select {
					DatabaseManager.Config.guildId eq guildId.toString()
				}.single()[column]
			} catch (e: NoSuchElementException) {
				error = "NoSuchElementException"
			}
		}
		return if (error == null) value else error
	}

	/**
	 * Using the provided [guildId] and nullable [column] a [value] or [error] will be returned from the
	 * config database
	 *
	 * @param guildId The ID of the guild the command was run in
	 * @param column The column you want the information from. Nullable
	 * @return a [NoSuchElementException] or the result from the Database
	 * @author NoComment1105
	 */
	@Suppress("DuplicatedCode")
	@JvmName("selectInConfig?")
	suspend fun selectInConfig(guildId: Snowflake, column: Column<String?>): String? {
		var value: String? = null
		var error: String? = null
		newSuspendedTransaction {
			try {
				value = DatabaseManager.Config.select {
					DatabaseManager.Config.guildId eq guildId.toString()
				}.single()[column]
			} catch (e: NoSuchElementException) {
				error = "NoSuchElementException"
			}
		}
		return if (error == null) value else error
	}

	/**
	 * Using the provided [componentId] and nullable [column] a [value] or [error] will be returned from the
	 * config database
	 *
	 * @param componentId The ID of the component you're looking for.
	 * @param column The column you want the information from.
	 * @return a [NoSuchElementException] or the result from the Database.
	 * @author NoComment1105
	 */
	suspend fun selectInComponents(componentId: String, column: Column<String>): String? {
		var value: String? = null
		var error: String? = null
		newSuspendedTransaction {
			try {
				value = DatabaseManager.Components.select {
					DatabaseManager.Components.componentId eq componentId
				}.single()[column]
			} catch (e: NoSuchElementException) {
				error = "NoSuchElementException"
			}
		}
		return if (error == null) value else error
	}
}
