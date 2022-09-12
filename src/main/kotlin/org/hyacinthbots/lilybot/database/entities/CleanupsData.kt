package org.hyacinthbots.lilybot.database.entities

import kotlinx.datetime.Instant

data class CleanupsData(
	val runGuildCleanup: Instant,
	val runThreadCleanup: Instant,
	val id: String = "cleanups"
)
