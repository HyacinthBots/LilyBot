package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseHelper {

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
