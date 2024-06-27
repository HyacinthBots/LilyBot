package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.hyacinthbots.lilybot.extensions.moderation.ModerationAction

// TODO DOCS
@Serializable
data class ModerationActionData(
	val actionType: ModerationAction,
	val guildId: Snowflake,
	val targetUserId: Snowflake,
	val data: ActionData,
	val ignore: Boolean = false
)

// TODO DOCS
@Serializable
data class ActionData(
	val actioner: Snowflake?,
	val deletedMessages: Int?,
	val timeData: TimeData?,
	val reason: String?,
	val dmOutcome: Boolean?,
	val dmOverride: Boolean?,
	val imageUrl: String?
)

// TODO DOCS
@Serializable
data class TimeData(
	val durationDtp: DateTimePeriod?,
	val durationInst: Instant?,
	val start: Instant? = null,
	val end: Instant? = null,
)
