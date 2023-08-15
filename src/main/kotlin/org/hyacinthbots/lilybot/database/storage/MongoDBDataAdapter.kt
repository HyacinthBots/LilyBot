/*
* This code was utilized from [cozy](https://github.com/QuiltMC/cozy-discord) by QuiltMC
* and hence is subject to the terms of the Mozilla Public License V. 2.0
* A copy of this license can be found at https://mozilla.org/MPL/2.0/.
*/

package org.hyacinthbots.lilybot.database.storage

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.storage.Data
import com.kotlindiscord.kord.extensions.storage.DataAdapter
import com.kotlindiscord.kord.extensions.storage.StorageUnit
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.bson.conversions.Bson
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.AdaptedData
import org.koin.core.component.inject

@OptIn(InternalSerializationApi::class)
class MongoDBDataAdapter : DataAdapter<String>(), KordExKoinComponent {
	private val database: Database by inject()
	private val collectionCache: MutableMap<String, MongoCollection<AdaptedData>> = mutableMapOf()

	private fun StorageUnit<*>.getIdentifier(): String =
		buildString {
			append("${storageType.type}/")

			if (guild != null) append("guild-$guild/")
			if (channel != null) append("channel-$channel/")
			if (user != null) append("user-$user/")
			if (message != null) append("message-$message/")

			append(identifier)
		}

	private fun getCollection(namespace: String): MongoCollection<AdaptedData> {
		var collection = collectionCache[namespace]

		if (collection == null) {
			collection = database.configDatabase.getCollection(namespace)

			collectionCache[namespace] = collection
		}

		return collection
	}

	private fun constructQuery(unit: StorageUnit<*>): Bson {
		var query = eq(AdaptedData::identifier.name, unit.identifier)

		query = and(query, eq(AdaptedData::type.name, unit.storageType))

		query = and(query, eq(AdaptedData::channel.name, unit.channel))
		query = and(query, eq(AdaptedData::guild.name, unit.guild))
		query = and(query, eq(AdaptedData::message.name, unit.message))
		query = and(query, eq(AdaptedData::user.name, unit.user))

		return query
	}

	override suspend fun <R : Data> delete(unit: StorageUnit<R>): Boolean {
		removeFromCache(unit)

		val result = getCollection(unit.namespace)
			.deleteOne(constructQuery(unit))

		return result.deletedCount > 0
	}

	override suspend fun <R : Data> get(unit: StorageUnit<R>): R? {
		val dataId = unitCache[unit]

		if (dataId != null) {
			val data = dataCache[dataId]

			if (data != null) {
				return data as R
			}
		}

		return reload(unit)
	}

	override suspend fun <R : Data> reload(unit: StorageUnit<R>): R? {
		val dataId = unit.getIdentifier()
		val result = getCollection(unit.namespace).find(constructQuery(unit)).firstOrNull()?.data

		if (result != null) {
			dataCache[dataId] = Json.decodeFromString(unit.dataType.serializer(), result)
			unitCache[unit] = dataId
		}

		return dataCache[dataId] as R?
	}

	override suspend fun <R : Data> save(unit: StorageUnit<R>): R? {
		val data = get(unit) ?: return null

		getCollection(unit.namespace).replaceOne(
			Filters.empty(),
			AdaptedData(
				identifier = unit.identifier,

				type = unit.storageType,

				channel = unit.channel,
				guild = unit.guild,
				message = unit.message,
				user = unit.user,

				data = Json.encodeToString(unit.dataType.serializer(), data)
			)
		)

		return data
	}

	override suspend fun <R : Data> save(unit: StorageUnit<R>, data: R): R {
		val dataId = unit.getIdentifier()

		dataCache[dataId] = data
		unitCache[unit] = dataId

		getCollection(unit.namespace).replaceOne(
			Filters.empty(),
			AdaptedData(
				identifier = unit.identifier,

				type = unit.storageType,

				channel = unit.channel,
				guild = unit.guild,
				message = unit.message,
				user = unit.user,

				data = Json.encodeToString(unit.dataType.serializer(), data)
			)
		)

		return data
	}
}
