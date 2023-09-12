@file:Suppress("DEPRECATION_ERROR", "UnusedPrivateMember", "UNUSED_PARAMETER", "RedundantSuspendModifier")

package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.kotlin.client.coroutine.MongoDatabase

suspend fun configV5(db: MongoDatabase) {
// 	val collection = db.getCollection<UtilityConfigDataOld>("utilityConfigData")
// 	val oldConfigs = collection.find().toList()
// 	val newConfigs = mutableListOf<UtilityConfigData>()
//
// 	oldConfigs.forEach {
// 		newConfigs.add(UtilityConfigData(it.guildId, it.utilityLogChannel))
// 	}
//
// 	db.dropCollection("utilityConfigData")
// 	db.createCollection("utilityConfigData")
//
// 	with(db.getCollection<UtilityConfigData>("utilityConfigData")) {
// 		insertMany(newConfigs)
// 	}
}
