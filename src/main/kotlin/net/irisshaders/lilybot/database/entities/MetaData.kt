package net.irisshaders.lilybot.database.entities

data class MetaData(
	val version: Int,
	val configVersion: Int,
	val id: String = "meta"
)
