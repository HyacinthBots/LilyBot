package net.irisshaders.lilybot.database.entities

import kotlinx.serialization.Serializable

/**
 * The data for the metadata of the main database.
 *
 * @property version The version of the database
 * @property id The id of the database. Remains constant
 *
 * @since 4.0.0
 */
@Serializable
data class MainMetaData(
	val version: Int,
	val id: String = "mainMeta"
)

/**
 * The data for the metadata of the config database.
 *
 * @property version The version of the database
 * @property id The id of the database. Remains constant
 *
 * @since 4.0.0
 */
@Serializable
data class ConfigMetaData(
	val version: Int,
	val id: String = "configMeta"
)
