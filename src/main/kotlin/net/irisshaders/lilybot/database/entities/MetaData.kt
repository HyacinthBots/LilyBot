package net.irisshaders.lilybot.database.entities

import kotlinx.serialization.Serializable

// TODO Kdoc

@Serializable
data class MainMetaData(
	val version: Int,
	val id: String = "mainMeta"
)

@Serializable
data class ConfigMetaData(
	val version: Int,
	val id: String = "configMeta"
)