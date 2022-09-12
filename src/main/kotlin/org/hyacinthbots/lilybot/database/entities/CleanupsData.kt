package org.hyacinthbots.lilybot.database.entities

import kotlinx.datetime.Instant

/**
 * The data for when to run cleanups on old data.
 *
 * @property runGuildCleanup The instant to run the guild cleanups. Should be run on 30-day intervals.
 * @property runThreadCleanup The instant to run the thread cleanups. Should be run on 7-day intervals.
 * @since 4.1.0
 */
data class CleanupsData(
	val runGuildCleanup: Instant,
	val runThreadCleanup: Instant,
)
