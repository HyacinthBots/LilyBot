package net.irisshaders.lilybot.database.entities

data class MetaData(
	val mainVersion: Int,
	val configVersion: Int,
	val id: String = "meta"
)
