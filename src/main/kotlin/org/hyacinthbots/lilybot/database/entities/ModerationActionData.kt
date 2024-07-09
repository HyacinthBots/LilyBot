package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.hyacinthbots.lilybot.extensions.moderation.ModerationAction

/**
 * The data for Moderation action.
 *
 * @property actionType The type of action you're adding
 * @property guildId The ID of the guild the action occurred in
 * @property targetUserId The ID of the user this action happened to
 * @property data The [ActionData] for the action
 * @property ignore Whether to ignore the action or not. Defaults to false
 * @since 5.0.0
 */
@Serializable
data class ModerationActionData(
	val actionType: ModerationAction,
	val guildId: Snowflake,
	val targetUserId: Snowflake,
	val data: ActionData,
	val ignore: Boolean = false
)

/**
 * Further, more in-depth data about a [moderation action][ModerationActionData].
 *
 * @property actioner The ID of the user that requested the action
 * @property deletedMessages The amount of messages deleted in the action
 * @property timeData The [TimeData] for the action
 * @property reason The reason for the action
 * @property dmOutcome The outcome of trying to send a DM to the user
 * @property dmOverride Whether the DM sending function was override
 * @property imageUrl The URL for the image attached to the action
 * @since 5.0.0
 */
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

/**
 * Further, more in-depth data about the [time data for actions][ActionData.timeData].
 *
 * @property durationDtp The Duration as a [DateTimePeriod]
 * @property durationInst The Duration as an [Instant]
 * @property start The start [Instant] of the action
 * @property end The end [Instant] of the action
 * @since 5.0.0
 */
@Serializable
data class TimeData(
	val durationDtp: DateTimePeriod?,
	val durationInst: Instant?,
	val start: Instant? = null,
	val end: Instant? = null,
)
