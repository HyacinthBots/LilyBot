/*
* This code was utilized from [cozy](https://github.com/QuiltMC/cozy-discord) by QuiltMC
* and hence is subject to the terms of the Mozilla Public License V. 2.0
* A copy of this license can be found at https://mozilla.org/MPL/2.0/.
*/
package org.hyacinthbots.lilybot.database.entities

import com.kotlindiscord.kord.extensions.storage.StorageType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
@Suppress("DataClassShouldBeImmutable")
data class AdaptedData(
	val identifier: String,

	val type: StorageType? = null,

	val channel: Snowflake? = null,
	val guild: Snowflake? = null,
	val message: Snowflake? = null,
	val user: Snowflake? = null,

	var data: String
)
