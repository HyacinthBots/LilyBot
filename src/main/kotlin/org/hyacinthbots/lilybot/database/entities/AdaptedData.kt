/*
* This code was utilized from [cozy](https://github.com/QuiltMC/cozy-discord) by QuiltMC
* and hence is subject to the terms of the Mozilla Public Licence V. 2.0
* A copy of this licence can be found at https://mozilla.org/MPL/2.0/.
*/
package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import dev.kordex.core.storage.StorageType
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
