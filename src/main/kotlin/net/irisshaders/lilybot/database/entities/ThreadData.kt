package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for threads.
 *
 * @param threadId The ID of the thread
 * @param ownerId The ID of the thread's owner
 * @param preventArchiving Whether to stop the thread from being archived or not
 * @since 3.2.0
 */
@Suppress("DataClassShouldBeImmutable")
@Serializable
data class ThreadData(
	val threadId: Snowflake,
	val ownerId: Snowflake,
	var preventArchiving: Boolean = false
)
