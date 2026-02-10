package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for GitHub commands.
 *
 * @property guildId The ID of the guild the data is for
 * @property defaultRepo The Default Repo to search for issues in
 *
 * @since 4.3.0
 */
@Serializable
data class GithubData(
    val guildId: Snowflake,
    val defaultRepo: String
)
