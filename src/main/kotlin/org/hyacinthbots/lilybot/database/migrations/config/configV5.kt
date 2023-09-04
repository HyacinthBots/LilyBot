@file:Suppress("DEPRECATION_ERROR", "UnusedPrivateMember", "UNUSED_PARAMETER", "RedundantSuspendModifier")

package org.hyacinthbots.lilybot.database.migrations.config

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun configV5(db: CoroutineDatabase) {
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
